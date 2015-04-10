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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.bson.BSONDecoder;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.internal.FileConfigurationReader;
import org.roqmessaging.management.config.internal.GCMPropertyDAO;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.server.MngtController;
import org.roqmessaging.management.zookeeper.RoQZooKeeperClient;
import org.roqmessaging.zookeeper.Metadata;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;

/**
 * Class GlobalConfigurationManager
 * <p> Description: responsible for handling the global configuration. This class must run 
 * within a thread. In the future this class will share its data through a data grid.
 * Notice that the configuration is maintain through a state DAO, this object is ready to be shared on data grid. 
 * This state is persisted at the configuration manager level.
 * 
 * @author sskhiri
 */
public class GlobalConfigurationManager implements Runnable, IStoppable {
	private volatile boolean running;
	public static volatile boolean hasLead = false;
	//ZMQ config
	private ZMQ.Socket clientReqSocket = null;
	private ZMQ.Context context;
	private GCMPropertyDAO properties=null;
	//The shutdown monitor
	private ShutDownMonitor shutDownMonitor = null;
	//utils
	private RoQSerializationUtils serializationUtils=null;
	
	//The management controller that maintains the off line configuration
	private MngtController mngtController = null;
	//Handles on timers:
	private GlobalConfigTimer configTimerTask = null;
	//Handles on the management timer that send update to the magements.
	private Timer mngtTimer=null;
	
	//Serializer (BSON)
	private IRoQSerializer serialiazer = new RoQBSONSerializer();
	
	private Logger logger = Logger.getLogger(GlobalConfigurationManager.class);

	private RoQZooKeeperClient zk;
	
	/**
	 * Constructor.
	 * 
	 * @param properties  the gcm configuration
	 * @param cloudConfig the cloud configuration parameters
	 */
	public GlobalConfigurationManager(GCMPropertyDAO properties, CloudConfig cloudConfig) {
		init(properties, cloudConfig);
	}
	/**
	 * Constructor.
	 * @param configFile the file containing the GCM properties
	 */
	public GlobalConfigurationManager(String configFile) {
		FileConfigurationReader reader = new FileConfigurationReader();
		
		try {
			init(reader.loadGCMConfiguration(configFile),
					reader.loadCloudConfiguration(configFile));
		} catch (ConfigurationException e) {
			logger.error("Error in constructor", e);
		}
	}

	/**
	 * Performs all variable initializations and starts the
	 * ZooKeeper client and the MngtController instance.
	 * 
	 * @param properties  the gcm configuration
	 * @param cloudConfig the cloud configuration parameters
	 */
	private void init(GCMPropertyDAO properties, CloudConfig cloudConfig) {
		this.logger.info("Started global config Runnable");

		// Init variables and pointers
		this.running = true;
		this.serializationUtils = new RoQSerializationUtils();

		this.properties = properties;
		logger.info("Configuration from props: " + properties.toString());
		
		// Initialization of the ZMQ port for the GCM interface
		int port = properties.ports.get("GlobalConfigurationManager.interface");
		this.context = ZMQ.context(1);
		this.clientReqSocket = context.socket(ZMQ.REP);
		this.clientReqSocket.bind("tcp://*:"+port);

		// Setup and start the zookeeper client
		this.zk = new RoQZooKeeperClient(properties.zkConfig);
		this.zk.start();
		this.zk.startLeaderElection();
		
		// clear zookeeper storage if requested
		if (properties.isFormatDB()) {
			zk.clear();
		}

		logger.info("Writing cloud configuration to zookeeper");
		this.zk.setCloudConfig(serializationUtils.serialiseObject(cloudConfig));
		
		// Start the shutdown thread
		int shutDownPort = properties.ports.get("GlobalConfigurationManager.shutDown");
		this.shutDownMonitor = new ShutDownMonitor(shutDownPort, this);
		new Thread(this.shutDownMonitor).start();
		
		// Ensure that basic node are created
		zk.initZkClusterNodes();
		
		try {
			// This master wait until Curator indicates
			// that it is the leader
			logger.info("Waiting for leadership");
			zk.waitUntilLeader();
			logger.info("Leadership acquired");
			// Set leader address in ZK
			zk.setGCMLeader();		
			// Start service discovery
			zk.startServiceDiscovery();
			// Set that the process got the lead
			hasLead = true;
			// The Management controller - the start is in the run to take the
			// period attribute
			this.mngtController = new MngtController("localhost", (properties.getPeriod() + 500), this.properties, this.zk);
			new Thread(mngtController).start();
		} catch (EOFException | InterruptedException e) {
			logger.info("The leader election has failed, stopping the " +
					"GCM");
			this.mngtController.getShutDownMonitor().shutDown();
			e.printStackTrace();
		} catch (Exception e) {
			logger.info("An error occured with the curator service discovery");
			e.printStackTrace();
		}
	}
	
	/**
	 * Start the timer that periodically triggers the
	 * run() method of GlobalConfigTimer.
	 */
	private void startGlobalConfigTimer() {
		int port = properties.ports.get("GlobalConfigTimer.pub");
		configTimerTask = new GlobalConfigTimer(port, this);
		mngtTimer = new Timer("Management config publisher");
		mngtTimer.schedule(configTimerTask, 500, this.properties.getPeriod());
	}

	/**
	 * Main run
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		this.running = true;
		
		// Start the timer that periodically triggers the
		// run() method of GlobalConfigTimer.
		
		startGlobalConfigTimer();
		
		//ZMQ init
		ZMQ.Poller items = new Poller(3);
		items.register(this.clientReqSocket);
		//2. Start the main run of the monitor
		while (this.running) {
			items.poll(100);
			if (items.pollin(0)){ //Comes from a client
				byte[] encoded = clientReqSocket.recv(0);
				String content = new String(encoded);
				logger.debug("Receiving request..." + content);
				
				//check if the string contains a "," if not that means that it is a BSON encoded request
				if(content.contains(",")) {
					try {
						processStandardRequest(content);
					} catch (Exception e) {
						logger.warn("A failure has occured, cause ZK discovery service" + e);
					}
				}else{
					processBSONRequest(encoded);
				}
			}
			// If connection with zookeeper has been lost
			if (!hasLead) {
				// We wait that process become the active one
				try {
					logger.info("connection with ZK has been lost," +
							" waiting for leadership");
					// Stop the timer task
					configTimerTask.cancel();
					mngtTimer.cancel();
					mngtTimer.purge();
					// Notifies that the leader has changed
					while (!GlobalConfigurationManager.hasLead) {
						items.poll(100);
						if (items.pollin(0)){ //Comes from a client
							byte[] encoded = clientReqSocket.recv(0);
							String content = new String(encoded);
							if(content.contains(",")){
								clientReqSocket.send(
										this.serialiazer.serialiazeConfigAnswer(
											RoQConstant.EVENT_LEAD_LOST, ""),
										0);
							} else {
								this.clientReqSocket.send(
										Integer.toString(RoQConstant.EVENT_LEAD_LOST).getBytes(), 0);
							}
						}
					}
					// Wait for leadership
					zk.waitUntilLeader();
					logger.info("connection with ZK has been reetablished " +
							" GCM is replaning the tasks");
					// Set leader address
					zk.setGCMLeader();
					// Restart the timer task
					startGlobalConfigTimer();
				} catch (EOFException | InterruptedException e) {
					logger.info("The leader election has failed, stopping the GCM");
					// Send GCM shutdown signal
					this.mngtController.getShutDownMonitor().shutDown();
					this.running = false;
				}
			}
		}
		logger.info("GCM Stopped.");
	}
	
	/**
	 * Processes the BSON requests
	 * @param encoded the encoded BSON object request
	 */
	private void processBSONRequest(byte[] encoded)  {
		logger.debug("Getting a BSON request");
		//1. BSON decoding
		BSONDecoder decoder = new BasicBSONDecoder();
		BSONObject request = decoder.readObject(encoded);
		
		//2. Get the "CMD"
		if(request.containsField("CMD")){
			int cmd = (Integer) request.get("CMD");
			if(cmd == RoQConstant.BSON_CONFIG_GET_HOST_BY_QNAME){
				String queueName = (String) request.get("QName");
				logger.debug("GET HOST BY QNAME = "+ queueName);
				
				String[] fields = getHostByQueueName_bson(queueName);
				if (fields == null) {
					logger.warn(" No logical queue as:"+queueName);
					this.clientReqSocket.send(serialiazer.serialiazeConfigAnswer(RoQConstant.FAIL,
							"The Queue "+queueName+"  is not registred."), 0);
				} else {
					String monitor = fields[0];
					String statMonitorKpiPub = fields[1];
					
					logger.debug("Answering back:"+ monitor + "," + statMonitorKpiPub);
					this.clientReqSocket.send(
							this.serialiazer.serialiazeMonitorInfo(
									monitor, statMonitorKpiPub),
							0);
				}
			} else {
				this.logger.error("Unrecognized BSON command: " + cmd);
			}
		}else{
			this.logger.error("The BSON object does not contain the CMD field ", new IllegalStateException("Expecting the CDM field in the request BSON object"));
		}
	}

	/**
	 * This method processes the string we got.
	 * @param request the string request we received.
	 * @throws Exception 
	 */
	private void processStandardRequest(String request) throws Exception {
		String  info[] = request.split(",");
		int infoCode = Integer.parseInt(info[0]);
		logger.debug("Start analysing info code = "+ infoCode);
		switch (infoCode) {
		
		//init request from client that want to receive a local cache of configuration
		case RoQConstant.INIT_REQ:
			// A client is asking for the topology of all local host
			// manager
			logger.debug("Recieveing init request from a client ");
			
			List<String> hcmStringList = getHostManagerAddresses();
			Map<String, String> queueMonitorMap = new HashMap<String, String>();
			Map<String, String> queueStatMonitorMap = new HashMap<String, String>();
			Map<String, String> queueHCMMap = new HashMap<String, String>();
			Map<String, List<String>> queueBUMonitorMap = new HashMap<String, List<String>>();
			Map<String, List<String>> queueBUMonitorHCMMap = new HashMap<String, List<String>>();

			// This is very ugly because for the moment the client does not cache anything,
			// meaning that each call represents a ZooKeeper transaction.
			// That should change.
			// 
			// Furthermore, to limit the number of calls, Metadata.Queue could contain instances
			// of Metadata.HCM, Metadata.Monitor and Metadata.StatMonitor.
			for (Metadata.Queue queue : zk.getQueueList()) {
				// Only return running queues
				if (!zk.isRunning(queue)) {
					continue;
				}
				
				Metadata.Monitor m = zk.getMonitor(queue);
				Metadata.StatMonitor sm = zk.getStatMonitor(queue);
				Metadata.HCM hcm = zk.getHCM(queue);
				
				ArrayList<Metadata.BackupMonitor> buList = zk.getBackUpMonitors(queue);
				List<String> monitorsSTBY = new ArrayList<String>();
				List<String> monitorsHostSTBY = new ArrayList<String>();
				for (Metadata.BackupMonitor backup : buList) {
					monitorsSTBY.add(backup.monitorAddress);
					monitorsHostSTBY.add(backup.hcmAddress);
				}
				queueBUMonitorHCMMap.put(queue.name, monitorsHostSTBY);
				queueBUMonitorMap.put(queue.name, monitorsSTBY);
				queueMonitorMap.put(queue.name, m.address);
				queueStatMonitorMap.put(queue.name, sm.address);
				queueHCMMap.put(queue.name, hcm.address);
			}

			this.clientReqSocket.send(this.serializationUtils.serialiseObject(hcmStringList),       ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(queueMonitorMap),     ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(queueHCMMap),         ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(queueStatMonitorMap), ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(queueBUMonitorMap), 	ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(queueBUMonitorHCMMap), 	0);
			
			
			logger.debug("Sending back the topology - list of local host");
			break;
			
		case RoQConstant.CONFIG_START_QUEUE:
			logger.debug("Recieveing start Q request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid we 2 part:  "+ info[1]);
				
				String queueName = info[1];
				// register the queue
				setQueueStarted(queueName);
				this.clientReqSocket.send(Integer.toString(RoQConstant.OK).getBytes(), 0);
			} else {
				logger.error("The start queue request sent does not contain 2 part: ID, quName");
				this.clientReqSocket.send(Integer.toString(RoQConstant.FAIL).getBytes(), 0);
			}
			break;
			
		case RoQConstant.CONFIG_STOP_QUEUE:
			logger.debug("Recieveing stop Q request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid we 2 part:  "+ info[1]);

				String queueName = info[1];
				setQueueStopped(queueName);
				this.clientReqSocket.send(Integer.toString(RoQConstant.OK).getBytes(), 0);
			} else {
				logger.error("The stop queue request sent does not contain 2 part: ID, quName");
				this.clientReqSocket.send(Integer.toString(RoQConstant.FAIL).getBytes(), 0);
			}
			break;
		
		case RoQConstant.CONFIG_REMOVE_QUEUE:
			logger.debug("Recieveing remove Q request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid we 2 part:  "+ info[1]);

				String queueName = info[1];
				removeQueue(queueName);
				this.clientReqSocket.send(Integer.toString(RoQConstant.OK).getBytes(), 0);
			}else{
					logger.error("The remove queue request sent does not contain 2 part: ID, quName");
					this.clientReqSocket.send(Integer.toString(RoQConstant.FAIL).getBytes(), 0);
				}
			break;
			
		case RoQConstant.CONFIG_CREATE_QUEUE:
			logger.debug("Recieveing create Q request from a client ");
			if (info.length >3) {
				logger.debug("The request format is valid we 4 part:  "+ info[1] +" "+ info[2]+ " "+ info[3]+ " "+ info[4]);
				
				String queueName = info[1];
				String monitorAddress = info[2];
				String statMonitorAddress = info[3];
				String hcmAddress = info[4];
				ArrayList<String> monitorsBU = new ArrayList<String>();
				ArrayList<String> monitorsBUHost = new ArrayList<String>();
				
				for (int i = 5; i < info.length; i+=2) {
					monitorsBUHost.add(info[i]);
					monitorsBU.add(info[i+1]);
				}
				// register the queue
				addQueue(queueName, monitorAddress, statMonitorAddress, hcmAddress, monitorsBU, monitorsBUHost);
				this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_REQUEST_OK).getBytes(), 0);
			}else{
					logger.error("The create queue request sent does not contain 3 part: ID, quName, Monitor host");
					this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_REQUEST_FAIL).getBytes(), 0);
				}
			break;
			
			//A add host request
		case RoQConstant.CONFIG_ADD_HOST:
			logger.debug("Recieveing ADD HOST request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid adding as host : "+ info[1]);
				// The logical queue config is sent int the part 2
				String hcmAddress = info[1];
				addHostManager(hcmAddress);
				
				this.clientReqSocket.send(Integer.toString(RoQConstant.OK).getBytes(), 0);
			}else{
				this.clientReqSocket.send(Integer.toString(RoQConstant.FAIL).getBytes(), 0);
			}
			break;
			
			//A remove host request
		case RoQConstant.CONFIG_REMOVE_HOST:
			logger.debug("Recieveing REMOVE HOST request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid ");
				// The logical queue config is sent int the part 2
				removeHostManager(info[1]);
				this.clientReqSocket.send(Integer.toString(RoQConstant.OK).getBytes(), 0);
			}
			break;
			
			//Get the monitor and the statistic monitor forwarder  by the QName
		case RoQConstant.CONFIG_GET_HOST_BY_QNAME:
			logger.debug("Recieveing GET HOST request from a client ");
			if (info.length == 2) {
				String queueName = info[1];
				
				logger.debug("The request format is valid - Asking for translating  "+ queueName);
				
				String[] fields = getHostByQueueName(queueName);
				if (fields == null) {
					logger.warn(" No logical queue as:"+queueName);
					clientReqSocket.send("".getBytes(), 0);
				} else {
					String monitor = fields[0];
					String statMonitor = fields[1];
					
					String reply = monitor + "," + statMonitor;
					logger.debug("Answering back:" + reply);
					this.clientReqSocket.send(reply.getBytes(), 0);
				}
			}
			break;
			
			//Get the monitor and the statistic monitor forwarder  by the QName BUT In BSON for non java processes
		case RoQConstant.BSON_CONFIG_GET_HOST_BY_QNAME:
			logger.debug("Recieveing GET HOST by QNAME in BSON request from a client ");
			if (info.length == 2) {
				String queueName = info[1];
				logger.debug("The request format is valid - Asking for translating  "+ queueName);
				
				String[] fields = getHostByQueueName_bson(queueName);
				if (fields == null) {
					logger.warn(" No logical queue as:"+queueName);
					this.clientReqSocket.send(serialiazer.serialiazeConfigAnswer(RoQConstant.FAIL,
							"The Queue "+queueName+"  is not registred."), 0);
				} else {
					String monitor = fields[0];
					String statMonitorKpiPub = fields[1];
					
					logger.debug("Answering back:"+ monitor + "," + statMonitorKpiPub);
					this.clientReqSocket.send(
							this.serialiazer.serialiazeMonitorInfo(
									monitor, statMonitorKpiPub),
							0);
				}
			}
			break;
		}
		
	}

	/**
	 * @param queueName the name of the logical queue
	 * @param monitorAddress address of the queue's Monitor in the format "tcp://x.y.z:port"
	 * @param statMonitorAddress address of the queue's StatisticMonitor in the format "tcp://x.y.z:port"
	 * @param hcmAddress ip address of the host that handles the queue 
	 */
	public void addQueue(String queueName, String monitorAddress, String statMonitorAddress, String hcmAddress, List<String> monitorsBU,  List<String> monitorsBUHost) {
		Metadata.Queue queue = new Metadata.Queue(queueName);
		Metadata.Monitor monitor = new Metadata.Monitor(monitorAddress);
		Metadata.StatMonitor statMonitor = new Metadata.StatMonitor(statMonitorAddress);
		Metadata.HCM hcm = new Metadata.HCM(hcmAddress);
		ArrayList<Metadata.BackupMonitor> monitorsBUMeta = new ArrayList<Metadata.BackupMonitor>();
		
		for (int i =0; i < monitorsBU.size(); i++) {
			monitorsBUMeta.add(new Metadata.BackupMonitor(monitorsBUHost.get(i) + "," + monitorsBU.get(i)));
		}
		
		zk.createQueue(queue, hcm, monitor, statMonitor, monitorsBUMeta);
		setQueueStarted(queueName);
	}

	/**
	 * Removes all reference of the this queue
	 * @param queueName the logical queue name
	 */
	public void removeQueue(String queueName) {
		zk.removeQueue(new Metadata.Queue(queueName));
	}
	
	/**
	 * Set the "running" flag for the given queue
	 * @param queueName the name of the logical queue
	 */
	public void setQueueStarted(String queueName) {
		zk.setRunning(new Metadata.Queue(queueName), true);
	}

	/**
	 * Clear the "running" flag for the given queue
	 * @param queueName the logical queue name
	 */
	public void setQueueStopped(String queueName) {
		zk.setRunning(new Metadata.Queue(queueName), false);
	}



	/**
	 * @param host the ip address of the host to remove.
	 * @throws Exception 
	 */
	public void removeHostManager(String host) throws Exception {
		Metadata.HCM hcm = new Metadata.HCM(host);
		zk.createHcmRemoveTransaction(hcm);
		zk.removeHCM(hcm);
	}


	/**
	 * Stop the active thread
	 */
	@Override
	public void  shutDown(){
		this.running = false;
		logger.info("Shutting down the global configuration manager  - closing GCM elements...");
		this.clientReqSocket.close();
		//deactivate the timer
		configTimerTask.shutDown();
		this.mngtController.getShutDownMonitor().shutDown();
		try {
			zk.closeGCM();
		} catch (IOException e) {
			logger.warn("FAILED to close ZK at GCM");
		}
		this.logger.info("Shutting down config server");
	}
	
	/**
	 * Add a host manager address to the array.
	 * @param host the host to add (ip address), the port is defined by default as there is only 1 Host 
	 * manager per host machine.
	 * @throws Exception 
	 */
	public void addHostManager(String host) throws Exception{
		// zk.registerHCM(new Metadata.HCM(host));
	}
	

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	@Override
	public String getName() {
		return "Global config manager";
	}

	/**
	 * @return the shutDownMonitor
	 */
	public ShutDownMonitor getShutDownMonitor() {
		return shutDownMonitor;
	}

	/**
	 * @return the queueHostLocation
	 */
	public HashMap<String, String> getQueueHostLocation() {
		HashMap<String, String> queueHCMMap = new HashMap<String, String>();

		// This is very ugly because for the moment the client does not cache anything,
		// meaning that each call represents a ZooKeeper transaction.
		// That should change.
		for (Metadata.Queue queue : zk.getQueueList()) {
			Metadata.HCM hcm = zk.getHCM(queue);
			queueHCMMap.put(queue.name, hcm.address);
		}
		return queueHCMMap;
	}


	/**
	 * @param configPeriod the configPeriod to set
	 */
	public void setConfigPeriod(int configPeriod) {
		this.properties.setPeriod(configPeriod);
	}

	/**
	 * @return the mngtController
	 */
	public MngtController getMngtController() {
		return mngtController;
	}


	/**
	 * @return the list of host manager addresses
	 * @throws Exception 
	 */
	public List<String> getHostManagerAddresses() throws Exception {
		List<Metadata.HCM> hcmList = zk.getHCMList();
		List<String> returnValue = new ArrayList<String>();
		
		for (Metadata.HCM hcm : hcmList) {
			returnValue.add(hcm.address);
		}
		
		return returnValue;
	}
	
	/**
	 * Method used to get the reply to a BSON_CONFIG_GET_HOST_BY_QNAME message.
	 * It should be noted that the name is badly chosen since that message
	 * does not return a host, but rather monitor and stat monitor addresses.
	 * 
	 * @param queueName name of the logical queue
	 * @return monitor address (base port) and stat monitor address,
	 * 		or null of they don't exist.
	 * 
	 *  Note: the stat monitor address returned here is in fact the ip and port
	 *  of the socket used by the stat monitor to publish kpi numbers. 
	 */
	private String[] getHostByQueueName_bson(String queueName) {
		Metadata.Queue queue = new Metadata.Queue(queueName);
		Metadata.Monitor monitor = zk.getMonitor(queue);
		Metadata.StatMonitor statMonitor = zk.getStatMonitor(queue);
		
		if ((monitor == null) || (statMonitor == null)) {
			return null;
		}
		String statMonitorAddress = statMonitor.address;
		int basePort = RoQSerializationUtils.extractBasePort(statMonitorAddress);
		String statMonitorKpiPub = statMonitorAddress.substring(0, statMonitorAddress.length() - "xxxx".length());
		statMonitorKpiPub += basePort + 1;
		
		String[] returnValue = {monitor.address, statMonitorKpiPub};
		return returnValue;
	}
	
	/**
	 * Method used to get the reply to a CONFIG_GET_HOST_BY_QNAME message.
	 * It should be noted that the name is badly chosen since that message
	 * does not return a host, but rather monitor and stat monitor addresses.
	 * 
	 * @param queueName name of the logical queue
	 * @return monitor address (base port) and stat monitor address (base port),
	 * 		or null of they don't exist.
	 */
	private String[] getHostByQueueName(String queueName) {
		Metadata.Queue queue = new Metadata.Queue(queueName);
		Metadata.Monitor monitor = zk.getMonitor(queue);
		Metadata.StatMonitor statMonitor = zk.getStatMonitor(queue);
		
		if ((monitor == null) || (statMonitor == null)) {
			return null;
		}
		String[] returnValue = {monitor.address, statMonitor.address};
		return returnValue;
	}

	public int getInterfacePort() {
		return properties.ports.get("GlobalConfigurationManager.interface");
	}

}
