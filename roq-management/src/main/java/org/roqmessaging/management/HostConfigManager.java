/**
 * Copyright 2012 EURANOVA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.roqmessaging.management;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.factory.HostProcessFactory;
import org.roqmessaging.management.config.internal.FileConfigurationReader;
import org.roqmessaging.management.config.internal.HostConfigDAO;
import org.roqmessaging.management.monitor.HcmState;
import org.roqmessaging.scaling.ScalingProcess;
import org.roqmessaging.scaling.launcher.ScalingProcessLauncher;
import org.zeromq.ZMQ;

/**
 * Class HostConfigManager
 * <p>
 * Description: Responsible for the local management of the queue elements. For
 * each host it will track the list of monitors and exchanges. Notice that the
 * host config manager implement a server pattern that exposes services to the
 * monitor management.
 * 
 * @author sskhiri
 */
public class HostConfigManager implements Runnable, IStoppable {
	// ZMQ config
	private ZMQ.Socket clientReqSocket = null;
	private ZMQ.Socket globalConfigSocket =null;
	private ZMQ.Context context;
	// Logger
	private Logger logger = Logger.getLogger(HostConfigManager.class);
	// Host manager config
	private volatile boolean running = false;
	// The host configuration manager properties
	private HostConfigDAO properties = null;
	// Local configuration maintained by the host manager
	
	// Contains server state information
	private HcmState serverState;
	
	// processFactory
	private HostProcessFactory processFactory;
	
	//The shutdown monitor
	private ShutDownMonitor shutDownMonitor = null;
	//Network & IP address Configuration
	private boolean useNif = false;

	/**
	 * Constructor
	 * @param propertyFile the location of the property file
	 */
	public HostConfigManager(String propertyFile) {
		try {
			// Global init
			FileConfigurationReader reader = new FileConfigurationReader();
			this.properties = reader.loadHCMConfiguration(propertyFile);
			if(this.properties.getNetworkInterface()==null)
				useNif=false;
			else {
				useNif=true;
			}
			logger.info(this.properties.toString());
			// ZMQ Init
			this.context = ZMQ.context(1);
			this.clientReqSocket = context.socket(ZMQ.REP);
			this.clientReqSocket.setLinger(0);
			this.clientReqSocket.bind("tcp://*:5100");
			this.globalConfigSocket = context.socket(ZMQ.REQ);
			this.globalConfigSocket.connect("tcp://" + this.properties.getGcmAddress() + ":5000");
			// Init ServerState
			this.serverState = new HcmState();
			// Init process Factory
			this.processFactory = new HostProcessFactory(serverState, properties);
			// Init the shutdown monitor
			this.shutDownMonitor = new ShutDownMonitor(5101, this);
			new Thread(this.shutDownMonitor).start();
		} catch (ConfigurationException e) {
			logger.error("Error while reading configuration in " + propertyFile, e);
		}
	}
	
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		this.running = true;
		//1. Register to the global configuration
		registerHost();
		// ZMQ init
		ZMQ.Poller items = new ZMQ.Poller(1);
		items.register(this.clientReqSocket);

		// 2. Start the main run of the monitor
		while (this.running) {
			items.poll(100);
			if (items.pollin(0)) { // Comes from a client
				logger.debug("Receiving Incoming request @host...");
				String[] info = new String(clientReqSocket.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);
				logger.debug("Start analysing info code = " + infoCode);
				switch (infoCode) {

				// Receive a create queue request on the local host manager
				// (likely from the LogicalQFactory
				case RoQConstant.CONFIG_CREATE_QUEUE:
					logger.debug("Recieveing create Q request from a client ");
					if (info.length == 2) {
						String qName = info[1];
						logger.debug("The request format is valid with 2 parts, Q to create:  " + qName);
						String monitorAddress = serverState.getMonitor(qName);
						if (monitorAddress == null) {
							// 1. Start the monitor
							monitorAddress = processFactory.startNewMonitorProcess(qName);
						}
						// 2. Start the exchange
						// 2.1. Getting the monitor stat address
						// 2.2. Start the exchange
						boolean xChangeOK =  serverState.getExchanges(qName) != null;
						
						// The 00000000000000000 value is the id of the first Exchange process
						if (!xChangeOK)
							xChangeOK = processFactory.startNewExchangeProcess(qName, serverState.getMonitor(qName),
									serverState.getStat(qName), "00000000000000000"); 
						//2.3. Start the scaling process
						boolean scalingOK = serverState.getScalingProcess(qName) != null;
						if (!scalingOK)
							scalingOK = startNewScalingProcess(qName);
						// if OK send OK
						if (monitorAddress != null & xChangeOK && scalingOK) {
							logger.info("Successfully created new Q for " + qName + "@" + monitorAddress);
							this.clientReqSocket.send(
									(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_OK) + "," + monitorAddress + "," + serverState.getMonitor(qName))
											.getBytes(), 0);
						} else {
							logger.error("The create queue request has failed at the monitor host,check log (when starting launching scripts");
							this.clientReqSocket.send(
									(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) + "," + monitorAddress)
											.getBytes(), 0);
						}
					} else {
						logger.error("The create queue request sent does not contain 3 part: ID, quName, Monitor host");
						this.clientReqSocket.send(
								(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) + ", ").getBytes(), 0);
					}
					break;

				case RoQConstant.CONFIG_REMOVE_QUEUE:
					logger.debug("Recieveing remove Q request from a client ");
					if (info.length == 2) {
						String qName = info[1];
						processFactory.removingQueue(qName);
						// Removing Q information
						serverState.removeExchange(qName);
						serverState.removeMonitor(qName);
						serverState.removeStat(qName);
						serverState.removeScalingProcess(qName);
						this.clientReqSocket.send((Integer.toString(RoQConstant.OK) + ", ").getBytes(), 0);
					} else {
						logger.error("The remove queue request sent does not contain 2 part: ID, quName");
						this.clientReqSocket.send(
								(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) + ", ").getBytes(), 0);
					}
					break;

				case RoQConstant.CONFIG_CREATE_EXCHANGE:
					logger.debug("Recieveing create XChange request from a client ");
					if (info.length == 5) {
						String qName = info[1];
						String id = info[2];
						// Qname, monitorhost, monitorstat host
						if (processFactory.startNewExchangeProcess(qName, info[3], info[4], id)) {
							this.clientReqSocket.send((Integer.toString(RoQConstant.OK) ).getBytes(), 0);
						} else {
							this.clientReqSocket.send((Integer.toString(RoQConstant.FAIL) ).getBytes(), 0);
						}
					} else {
						logger.error("The create new exchange does not contain 4 parts: ID, Qname, monitor, monitor host");
						this.clientReqSocket.send(
								(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) ).getBytes(), 0);
					}
					break;
				case RoQConstant.CONFIG_INFO_EXCHANGE:
					logger.debug("Recieveing  get XChange INFO from a client ");
					try {
						//Answer in3 parts
						//[OK or FAIL], [Number of exchange on host], [max limit of exchange defined in property]
						this.clientReqSocket.send((Integer.toString(RoQConstant.OK) ).getBytes(), ZMQ.SNDMORE);
						this.clientReqSocket.send((Integer.toString(this.getExchangeNumber()) ).getBytes(), ZMQ.SNDMORE);
						this.clientReqSocket.send((Integer.toString(this.properties.getMaxNumberEchanges()) ).getBytes(), 0);
					} catch (Exception e) {
						this.clientReqSocket.send((Integer.toString(RoQConstant.FAIL) ).getBytes(), 0);
					}
				
					break;
				}
			}
		}
		stopAllRunningQueueOnHost();
		unregisterHostFromConfig();
		logger.info("Closing the client & global config sockets.");
		this.clientReqSocket.setLinger(0);
		this.globalConfigSocket.setLinger(0);
		this.clientReqSocket.close();
		this.globalConfigSocket.close();
	}

	/**
	 * @param qName the name of queue for which we need to create the scaling process
	 * @param port the listener port on wich the sclaing process will scubscribe to configuration update
	 * @return true if the creation was OK
	 */
	private boolean startNewScalingProcess(String qName) {
		if(serverState.statExists(qName)){
			//1. Compute the stat monitor port+2
			int basePort = RoQSerializationUtils.extractBasePort(serverState.getMonitor(qName));
			basePort+=2;
			
			// Get the address and the ports used by the GCM
			String gcm_address = this.properties.getGcmAddress();
			int gcm_interfacePort = this.properties.ports.get("GlobalConfigurationManager.interface");
			int gcm_adminPort    = this.properties.ports.get("MngtController.interface");
			
			//2. Check wether we need to launch it locally or in its own process
			if(this.properties.isQueueInHcmVm()){
				// Local startup in the same VM as the host config Monitor
				logger.info("Starting the scaling process  for queue " + qName + ", using a listener port= " + basePort +", GCM ="+this.properties.getGcmAddress() );
				
				ScalingProcess scalingProcess = new ScalingProcess(gcm_address, gcm_interfacePort, gcm_adminPort, qName, basePort);
				//Here is the problem we still do not have registred the queue at the GCM
				scalingProcess.subscribe();
				// Launch the thread
				new Thread(scalingProcess).start();
			}else{
				//Start in its own VM
				// 2. Launch script
				try {
					ProcessBuilder pb = new ProcessBuilder("java", "-Djava.library.path="
							+ System.getProperty("java.library.path"), "-cp", System.getProperty("java.class.path"),
							ScalingProcessLauncher.class.getCanonicalName(),
							gcm_address, Integer.toString(gcm_interfacePort), Integer.toString(gcm_adminPort),
							qName, Integer.toString(basePort));
					logger.debug("Starting: " + pb.command());
					final Process process = pb.start();
					pipe(process.getErrorStream(), System.err);
					pipe(process.getInputStream(), System.out);
				} catch (IOException e) {
					logger.error("Error while executing script", e);
					return false;
				}	
			}
			//4. Add the configuration information
			logger.debug("Storing scaling process information");
			serverState.putScalingProcess(qName, basePort+1);
		}else{
			return false;
		}
		return true;
	}

	/**
	 * Remove all queues delcared on this host. This operation is part of the cleaning 
	 * house before closing the host.
	 */
	private void stopAllRunningQueueOnHost() {
		Set<String> monitors = serverState.getAllMonitors();
		for (String qName : monitors) {
			logger.info("Cleaning host - removing  "+qName);
			processFactory.removingQueue(qName);
		}
		for (String qName : monitors) {
			// Removing Q information
			serverState.removeExchange(qName);
			serverState.removeMonitor(qName);
			serverState.removeStat(qName);
			serverState.removeScalingProcess(qName);
		}
		
	}

	/**
	 * @return the total number of exchanges on the host
	 */
	private int getExchangeNumber() {
		int total =0;
		for (String  queue : serverState.getAllExchanges()) {
			total+=serverState.getExchanges(queue).size();
		}
		return total;
	}

	/**
	 * Unregister the host from the configuration management when shutdown.
	 */
	private void unregisterHostFromConfig() {
		logger.info("UN-Registration process started");
		if(useNif)Assert.assertNotNull(this.properties.getNetworkInterface());
		this.globalConfigSocket.send((new Integer(RoQConstant.CONFIG_REMOVE_HOST).toString()+"," +
				(!(useNif)?RoQUtils.getInstance().getLocalIP():RoQUtils.getInstance().getLocalIP(this.properties.getNetworkInterface()))).getBytes(),0);
		String   info[] = new String (this.globalConfigSocket.recv(0)).split(",");
		int infoCode = Integer.parseInt(info[0]);
		logger.debug("Start analysing info code = "+ infoCode);
		if(infoCode != RoQConstant.OK){
			throw new IllegalStateException("The global config manager cannot register us ..");
		}
		logger.info("UN-Registration process sucessfull");
	}

	/**
	 * Register the host config manager to the global configration
	 */
	private void registerHost() throws IllegalStateException {
		logger.info("Registration process started");
		if(useNif)Assert.assertNotNull(this.properties.getNetworkInterface());
		this.globalConfigSocket.send((new Integer(RoQConstant.CONFIG_ADD_HOST).toString()+"," +
				(!(useNif)?RoQUtils.getInstance().getLocalIP():RoQUtils.getInstance().getLocalIP(this.properties.getNetworkInterface()))).getBytes(),0);
		String   info[] = new String (this.globalConfigSocket.recv(0)).split(",");
		int infoCode = Integer.parseInt(info[0]);
		logger.debug("Start analysing info code = "+ infoCode);
		if(infoCode != RoQConstant.OK){
			throw new IllegalStateException("The global config manager cannot register us ..");
		}
		logger.info("Registration process sucessfull");
		
	}

	private static void pipe(final InputStream src, final PrintStream dest) {
		new Thread(new Runnable() {
			public void run() {
				try {
					byte[] buffer = new byte[1024];
					for (int n = 0; n != -1; n = src.read(buffer)) {
						dest.write(buffer, 0, n);
					}
				} catch (IOException e) { // just exit
				}
			}
		}).start();
	}
	
	/**
	 * return the HCM State  
	 * @return HcmState
	 * 
	 */
	public HcmState getServerState() {
		return this.serverState;
	}
	
	/**
	 * 
	 */
	public void shutDown() {
		this.running = false;
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "Host config manager " + RoQUtils.getInstance().getLocalIP();
	}
	
	/**
	 * Use the encapsulation to let the shutdown monitor manage all shutdown 
	 * related actions.
	 * @return the shutDownMonitor
	 */
	public ShutDownMonitor getShutDownMonitor() {
		return shutDownMonitor;
	}

}
