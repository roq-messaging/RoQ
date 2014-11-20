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

import java.util.HashMap;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.bson.BSONDecoder;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.management.config.internal.FileConfigurationReader;
import org.roqmessaging.management.config.internal.GCMPropertyDAO;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.server.MngtController;
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
	//ZMQ config
	private ZMQ.Socket clientReqSocket = null;
	private ZMQ.Context context;
	private GCMPropertyDAO properties=null;
	//The GCM state
	private GlobalConfigurationState stateDAO = null;
	//The shutdown monitor
	private ShutDownMonitor shutDownMonitor = null;
	//utils
	private RoQSerializationUtils serializationUtils=null;
	
	//The management controller that maintains the off line configuration
	private MngtController mngtController = null;
	//The SQL DB name
	private String dbName = "Management.db";
	//Handles on timers:
	private GlobalConfigTimer configTimerTask = null;
	//Handles on the management timer that send update to the magements.
	private Timer mngtTimer=null;
	
	//Serializer (BSON)
	private IRoQSerializer serialiazer = new RoQBSONSerializer();
	
	private Logger logger = Logger.getLogger(GlobalConfigurationManager.class);

	
	/**
	 * Constructor.
	 * @param configFile the file location of the properties. By default, is the GCM.properties in the resources.
	 */
	public GlobalConfigurationManager(String configFile) {
		try {
			this.logger.info("Started global config Runnable");

			// Init variables and pointers
			this.running = true;
			this.serializationUtils = new RoQSerializationUtils();
			this.stateDAO = new GlobalConfigurationState();

			// Read configuration from file
			FileConfigurationReader reader = new FileConfigurationReader();
			this.properties = reader.loadGCMConfiguration(configFile);
			logger.info("Configuration from props: " + properties.toString());
						
			// Initialization of the ZMQ port for the GCM interface
			int port = properties.ports.get("GlobalConfigurationManager.interface");
			this.context = ZMQ.context(1);
			this.clientReqSocket = context.socket(ZMQ.REP);
			this.clientReqSocket.bind("tcp://*:"+port);

			// Start the shutdown thread
			int shutDownPort = properties.ports.get("GlobalConfigurationManager.shutDown");
			this.shutDownMonitor = new ShutDownMonitor(shutDownPort, this);
			new Thread(this.shutDownMonitor).start();

			// The Management controller - the start is in the run to take the
			// period attribute
			this.mngtController = new MngtController("localhost", dbName, (properties.getPeriod() + 500), this.properties);
			if (properties.isFormatDB())
				this.mngtController.getStorage().formatDB();
			new Thread(mngtController).start();
		} catch (Exception e) {
			logger.error("Error in constructor", e);
		}
	}

	/**
	 * Start the timer that periodically triggers the
	 * run() method of GlobalConfigTimer.
	 */
	private void startGlobalConfigTimer() {
		int port = properties.ports.get("GlobalConfigTimer.pub");
		mngtTimer = new Timer("Management config publisher");
		configTimerTask = new GlobalConfigTimer(port, this);
		mngtTimer.schedule(configTimerTask, 500, this.properties.getPeriod());
	}
	/**
	 * Main run
	 * @see java.lang.Runnable#run()
	 */
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
				if(content.contains(",")){
					processStandardRequest(content);
				}else{
					processBSONRequest(encoded);
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
				String qName = (String) request.get("QName");
				logger.debug("GET HOST BY QNAME = "+ qName);
				
				//Get the host for this QName
				if(this.stateDAO.getQueueMonitorMap().containsKey(qName) && this.stateDAO.getQueueMonitorStatMap().containsKey(qName)){
					//Replace the stat monitor port to the subscription port
					String subscribingKPIMonitor = this.stateDAO.getQueueMonitorStatMap().get(qName);
					int basePort = RoQSerializationUtils.extractBasePort(subscribingKPIMonitor);
					String portOff = subscribingKPIMonitor.substring(0, subscribingKPIMonitor.length() - "xxxx".length());
					subscribingKPIMonitor= portOff+(basePort+1);
					logger.debug("Answering back:"+ this.stateDAO.getQueueMonitorMap().get(qName)+","+subscribingKPIMonitor);
					this.clientReqSocket.send(this.serialiazer.serialiazeMonitorInfo(this.stateDAO.getQueueMonitorMap().get(qName),subscribingKPIMonitor), 0);
				}else{
					logger.warn(" No logical queue as:"+qName);
					this.clientReqSocket.send(serialiazer.serialiazeConfigAnswer(RoQConstant.FAIL,
							"The Queue "+qName+"  is not registred."), 0);
				}
			}
		}else{
			this.logger.error("The BSON object does not contain the CMD field ", new IllegalStateException("Expecting the CDM field in the request BSON object"));
		}
	}

	/**
	 * This method processes the string we got.
	 * @param request the string request we received.
	 */
	private void processStandardRequest(String request) {
		String  info[] = request.split(",");
		int infoCode = Integer.parseInt(info[0]);
		logger.debug("Start analysing info code = "+ infoCode);
		switch (infoCode) {
		
		//init request from client that want to receive a local cache of configuration
		case RoQConstant.INIT_REQ:
			// A client is asking for the topology of all local host
			// manager
			logger.debug("Recieveing init request from a client ");
			this.clientReqSocket.send( this.serializationUtils.serialiseObject(this.stateDAO.getHostManagerAddresses()), ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(this.stateDAO.getQueueMonitorMap()), ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(this.stateDAO.getQueueHostLocation()), ZMQ.SNDMORE);
			this.clientReqSocket.send(this.serializationUtils.serialiseObject(this.stateDAO.getQueueMonitorStatMap()), 0);
			logger.debug("Sending back the topology - list of local host");
			break;
			
		//A create queue request
		case RoQConstant.CONFIG_REMOVE_QUEUE:
			logger.debug("Recieveing remove Q request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid we 2 part:  "+ info[1]);
				// register the queue
				removeQueue(info[1]);
				this.clientReqSocket.send(Integer.toString(RoQConstant.OK).getBytes(), 0);
			}else{
					logger.error("The remove queue request sent does not contain 2 part: ID, quName");
					this.clientReqSocket.send(Integer.toString(RoQConstant.FAIL).getBytes(), 0);
				}
			break;
			
			//A remove  queue request
		case RoQConstant.CONFIG_CREATE_QUEUE:
			logger.debug("Recieveing create Q request from a client ");
			if (info.length >3) {
				logger.debug("The request format is valid we 4 part:  "+ info[1] +" "+ info[2]+ " "+ info[3]+ " "+ info[4]);
				// The logical queue config is sent int the part 2
				String qName = info[1];
				String monitorHost = info[2];
				String statMonitorHost = info[3];
				String targetAddress = info[4];
				// register the queue
				addQueueName(qName, monitorHost);
				addQueueStatMonitor(qName, statMonitorHost);
				addQueueLocation(qName, targetAddress);
				this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_OK).getBytes(), 0);
			}else{
					logger.error("The create queue request sent does not contain 3 part: ID, quName, Monitor host");
					this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL).getBytes(), 0);
				}
			break;
			
			//A add host request
		case RoQConstant.CONFIG_ADD_HOST:
			logger.debug("Recieveing ADD HOST request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid adding as host : "+ info[1]);
				// The logical queue config is sent int the part 2
				addHostManager(info[1]);
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
				logger.debug("The request format is valid - Asking for translating  "+ info[1]);
				if(this.stateDAO.getQueueMonitorMap().containsKey(info[1])){
					logger.debug("Answering back:"+ this.stateDAO.getQueueMonitorMap().get(info[1])+","+this.stateDAO.getQueueMonitorStatMap().get(info[1]));
					this.clientReqSocket.send((this.stateDAO.getQueueMonitorMap().get(info[1])+","+this.stateDAO.getQueueMonitorStatMap().get(info[1])).getBytes(), 0);
				}else{
					logger.warn(" No logical queue as:"+info[1]);
					this.clientReqSocket.send(("").getBytes(), 0);
				}
			}
			break;
			
			//Get the monitor and the statistic monitor forwarder  by the QName BUT In BSON for non java processes
		case RoQConstant.BSON_CONFIG_GET_HOST_BY_QNAME:
			logger.debug("Recieveing GET HOST by QNAME in BSON request from a client ");
			if (info.length == 2) {
				logger.debug("The request format is valid - Asking for translating  "+ info[1]);
				if(this.stateDAO.getQueueMonitorMap().containsKey(info[1]) && this.stateDAO.getQueueMonitorStatMap().containsKey(info[1])){
					//Replace the stat monitor port to the subscription port
					String subscribingKPIMonitor = this.stateDAO.getQueueMonitorStatMap().get(info[1]);
					int basePort = RoQSerializationUtils.extractBasePort(subscribingKPIMonitor);
					String portOff = subscribingKPIMonitor.substring(0, subscribingKPIMonitor.length() - "xxxx".length());
					subscribingKPIMonitor= portOff+(basePort+1);
					logger.debug("Answering back:"+ this.stateDAO.getQueueMonitorMap().get(info[1])+","+subscribingKPIMonitor);
					this.clientReqSocket.send(this.serialiazer.serialiazeMonitorInfo(this.stateDAO.getQueueMonitorMap().get(info[1]),subscribingKPIMonitor), 0);
				}else{
					logger.warn(" No logical queue as:"+info[1]);
					this.clientReqSocket.send(serialiazer.serialiazeConfigAnswer(RoQConstant.FAIL,
							"The Queue "+info[1]+"  is not registred."), 0);
				}
			}
			break;
		}
		
	}

	/**
	 * @param qName the name of the logical queue
	 * @param targetAddress the target host address to register
	 */
	public void addQueueLocation(String qName, String targetAddress) {
		logger.debug("Adding the logical queue to the target address" + targetAddress);
		this.stateDAO.getQueueHostLocation().put(qName, targetAddress);
		
	}

	/**
	 * Removes all reference of the this queue
	 * @param qName the logical queue name
	 */
	public void removeQueue(String qName) {
		if ((this.stateDAO.getQueueMonitorMap().remove(qName)==null) ||  (this.stateDAO.getQueueMonitorStatMap().remove(qName)==null) || (this.stateDAO.getQueueHostLocation().remove(qName)==null)){
			logger.error("Error while removing queue", new IllegalStateException("The queue name " + qName +" is not registred in the global configuration"));
		}else{
			logger.info("Removing queue "+ qName + " from global configuration");
		}
		
	}

	/**
	 * @param qName the name of the logical queue
	 * @param statMonitorHost the stat port address of the corresponding monitor
	 */
	public void addQueueStatMonitor(String qName, String statMonitorHost) {
		this.stateDAO.getQueueMonitorStatMap().put(qName, statMonitorHost);
		logger.debug("Adding stat monitor address for "+ qName +" @"+ statMonitorHost +" in global configuration");
		
	}


	/**
	 * @param host the ip address of the host to remove.
	 */
	public void removeHostManager(String host) {
		if(this.stateDAO.getHostManagerAddresses().remove(host)) logger.info("Removed host successfully "+ host);
		else  logger.error("Removed host failed on the hashmap "+ host);
	}


	/**
	 * Stop the active thread
	 */
	public void  shutDown(){
		this.running = false;
		logger.info("Shutting down the global configuration manager  - closing GCM elements...");
		this.clientReqSocket.close();
		//deactivate the timer
		configTimerTask.shutDown();
		this.mngtController.getShutDownMonitor().shutDown();
		this.logger.info("Shutting down config server");
	}
	
	/**
	 * Add a host manager address to the array.
	 * @param host the host to add (ip address), the port is defined by default as there is only 1 Host 
	 * manager per host machine.
	 */
	public void addHostManager(String host){
		this.logger.info("Adding new host manager reference : "+ host);
		if (!this.stateDAO.getHostManagerAddresses().contains(host)){
			stateDAO.getHostManagerAddresses().add(host);
		}
	}
	
	/**
	 * @param qName the logical queue name
	 * @param host as tcp://ip:port, tcp://ip:statport
	 */
	public void addQueueName(String qName, String host){
		this.stateDAO.getQueueMonitorMap().put(qName, host);
		logger.debug("Created queue "+ qName +" @"+ host +" in global configuration");
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
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
		return this.stateDAO.getQueueHostLocation();
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
	 */
	public Object getHostManagerAddresses() {
		return this.stateDAO.getHostManagerAddresses();
	}
	

}
