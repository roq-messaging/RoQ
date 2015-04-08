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
package org.roqmessaging.management.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.LogicalQFactory;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.internal.GCMPropertyDAO;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.server.state.QueueManagementState;
import org.roqmessaging.management.zookeeper.Metadata;
import org.roqmessaging.management.zookeeper.RoQZooKeeperClient;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

/**
 * Class MngtController
 * <p>
 * Description: Controller that loads/receive data from the
 * {@linkplain GlobalConfigurationManager} and refreshes the stored data. 
 * Global config manager         base port <br>
 * Shut down config manager      base port +1<br>
 * Global config manager publish base port +2<br>
 * Mngt request server           base port +3<br>
 * Shutdown monitor              base port +4<br>
 * Management timer              base port +5<br>
 * 
 * Note: the base port is provided in the GCM configuration file.
 * 
 * @author sskhiri
 */
public class MngtController implements Runnable, IStoppable {
	private Logger logger = Logger.getLogger(MngtController.class);
	// ZMQ
	private ZMQ.Context context = null;
	private ZMQ.Socket mngtRepSocket = null;
	// Logical queue factory
	private IRoQLogicalQueueFactory factory = null;
	// running
	private volatile boolean active = true;
	private ShutDownMonitor shutDownMonitor = null;
	// utils for serialization
	private RoQSerializationUtils serializationUtils = null;
	// The BSON serialiazer
	private IRoQSerializer serializer = null;
	// The publication period of the configuration
	private int period = 60000;
	// The map that register for a queue the configuration listener [Qname, Push
	// pull address]
	private HashMap<String, ZMQ.Socket> scalingConfigListener = null;
	//The HCM properties
	private GCMPropertyDAO properties = null;
	//The timer that sends periodically configuration to 3rd parties
	private Timer publicationTimer=null;
	//Handle ont the timer
	private MngtControllerTimer controllerTimer=null;
	
	private RoQZooKeeperClient zk;

	/**
	 * Constructor.
	 * 
	 * @param globalConfigAddress
	 *            the address on which the global config server runs.
	 * @param zk the ZooKeeper client
	 */
	public MngtController(String globalConfigAddress, int period, GCMPropertyDAO props, RoQZooKeeperClient zk) {
			this.period = period;
			this.properties= props;
			this.scalingConfigListener = new HashMap<String, ZMQ.Socket>();
			this.zk = zk;
			
			// Get the various ZMQ ports used by the MngtController
			int interfacePort = properties.ports.get("MngtController.interface");
			int shutDownPort  = properties.ports.get("MngtController.shutDown");
			
			// Initialize the connections
			init(globalConfigAddress, interfacePort, shutDownPort);
	}

	/**
	 * Initialize connections for the admin interface, the shut down port and
	 * the subscription to the host information published by the
	 * GlobalConfigurationManager through its instance of GlobalConfigTimer.
	 * 
	 * @param globalConfigAddress
	 *            the global configuration server address
	 * @param interfacePort
	 *            the port used for the admin interface
	 * @param shutDownPort
	 *            the port on which the shut down thread listens
	 */
	private void init(String globalConfigAddress, int interfacePort, int shutDownPort) {
		context = ZMQ.context(1);
		mngtRepSocket = context.socket(ZMQ.REP);
		mngtRepSocket.bind("tcp://*:"+interfacePort);
		// init variable
		this.serializationUtils = new RoQSerializationUtils();
		this.factory = new LogicalQFactory(globalConfigAddress, properties.gethcmTIMEOUT());
		this.serializer = new RoQBSONSerializer();
		// Shutdown thread configuration
		this.shutDownMonitor = new ShutDownMonitor(shutDownPort, this);
		new Thread(this.shutDownMonitor).start();
	}
	
	/**
	 * Start a timer which periodically triggers the run() method of the
	 * MngtControllerTimer.
	 */
	private void startMngtControllerTimer() {
		int port = properties.ports.get("MngtControllerTimer.pub");
		controllerTimer  = new MngtControllerTimer(this.period, this, port);
		publicationTimer = new Timer();
		publicationTimer.schedule(controllerTimer, period, period);
	}
	/**
	 * Runs in a while loop and wait for updated information from the
	 * {@linkplain GlobalConfigurationManager}
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		logger.debug("Starting " + getName());
		this.active = true;

		// Start a timer which periodically triggers the run() method of the
		// MngtControllerTimer, publishing data that no one listens to.
		startMngtControllerTimer();

		// ZMQ init of the subscriber socket
		ZMQ.Poller poller = new Poller(2);
		poller.register(mngtRepSocket);
		// Init variables
		// int infoCode = 0;

		// Start the running loop
		while (this.active) {
			// Set the poll time out, it returns either when something arrive or
			// when it time out
			poller.poll(100);
			
			// Pollin 0 = Management facade interface in BSON
			if (poller.pollin(0)) {
				try {
					// 1. Checking the command ID
					BSONObject request = BSON.decode(mngtRepSocket.recv(0));
					if (isMultiPart(mngtRepSocket)) {
						// Send an error message
						sendReply_fail("ERROR the client used a multi part ZMQ message while only on is required.");
					}
					if (checkField(request, "CMD")) {
						// Variables
						String qName = "?";
						String host = "?";
						String transID = "?";
						boolean recoveryMod = false;
						String hostToRecover = "";
						AutoScalingConfig config = null;
						Metadata.Queue queue = null;

						// 2. Getting the command ID
						switch ((Integer) request.get("CMD")) {
						
						case RoQConstant.BSON_CONFIG_REMOVE_QUEUE:
							logger.debug("Processing a REMOVE QUEUE REQUEST");
							if (!checkField(request, "QName")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>");
								break;
							}
							qName = (String) request.get("QName");
							logger.debug("Remove " + qName);
							if (!this.factory.removeQueue(qName)) {
								sendReply_fail("ERROR when removing the queue, check logs of the logical queue factory");
							} else {
								sendReply_ok("SUCCESS");
							}
							break;
						case RoQConstant.BSON_CONFIG_START_QUEUE:
							logger.info("Start  Q Request ... checking whether the Queue is known ...");
							logger.debug("Incoming request:" + request.toString());
							// Starting a queue
							// Just create a queue on a host
							if (!checkField(request, "QName")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>");
								break;
							}
							// 1. Get the name
							qName = (String) request.get("QName");
							host = zk.getHCM(new Metadata.Queue(qName)).address;

							if (!factory.startQueue(qName, host)) {
								sendReply_fail("ERROR when starting  queue, check logs of the logical queue factory");
							} else {
								sendReply_ok("SUCCESS");
							}
							
							logger.debug("Create queue name = " + qName + " on " + host + " from a start command");
							break;
						case RoQConstant.BSON_CONFIG_QUEUE_EXISTS:
							logger.info("Start  Q Request ... checking whether the Queue is known ...");
							logger.debug("Incoming request:" + request.toString());
							// Check if queue QName exists
							if (!checkField(request, "QName")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>");
								break;
							}
							// 1. Get the name
							qName = (String) request.get("QName");

							if (!factory.queueAlreadyExists(qName)) {
								sendReply_fail("NO");
							} else {
								sendReply_ok("YES");
							}
							
							logger.debug("Create queue name = " + qName + " on " + host + " from a start command");
							break;
						case RoQConstant.BSON_CONFIG_CREATE_QUEUE:
							logger.debug("Create Q request ...");
							// Starting a queue
							// Just create a queue on a host
							if (!checkField(request, "QName")
								|| !checkField(request, "Host")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>, <Host>");
								break;
							}
							// 1. Get the name
							qName = (String) request.get("QName");
							host = (String) request.get("Host");
							hostToRecover = zk.qTransactionExists(qName);
							if (hostToRecover != null) {
								// The transaction already exists, recovery mod
								recoveryMod = true;
								if (getHosts().contains(hostToRecover)) {
									host = hostToRecover;
								} else {
									// The initial host not exists
									// change the transaction to target the new host
									zk.removeQTransaction(qName);
									zk.createQTransaction(qName, host);
									
								}
								logger.info("Recover process for queue " + qName + " started " +
										"on HCM: " + hostToRecover);
							}
							else {
								// We create a transaction 
								zk.createQTransaction(qName, host);
							}
							// Just create queue the timer will update the
							// management server configuration
							int returnCode = factory.createQueue(qName, host, recoveryMod);
							if (returnCode == -1) {
								// We remove the transaction if the queue already exists
								zk.removeQTransaction(qName);
								sendReply_fail("The queue already exists");
							} else if (returnCode == -2) {
								sendReply_fail("The HCM looks to be unavailble");
							} else if (returnCode == -3) {
								sendReply_fail("The Queue creation process has failed on GCM or HCM");
							} else {
								// Transaction completed
								zk.removeQTransaction(qName);
								sendReply_ok("SUCCESS");
							}
							logger.debug("Create queue name = " + qName + " on " + host);
							break;
						case RoQConstant.BSON_CONFIG_CREATE_QUEUE_AUTOMATICALLY:
							logger.debug("Create Q automatically request ...");
							// Starting a queue
							// Just create a queue on a host
							if (!checkField(request, "QName")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>");
								break;
							}
							// 1. Get the name
							qName = (String) request.get("QName");
							hostToRecover = zk.qTransactionExists(qName);
							logger.info("transaction Exists? " + hostToRecover);
							boolean hostFound = false;
							ArrayList<String> hostsList = getHosts();
							if (hostToRecover != null) {
								// The transaction already exists, recovery mod
								recoveryMod = true;
								host = hostToRecover;
								// TODO: ArrayList is not a good data structure for this kind of Operation
								// But this operation is not performed very often
								if (!hostsList.contains(hostToRecover)) {
									// the host for this transaction no longer exists.
									zk.removeQTransaction(qName);
									if (hostsList.isEmpty()) {
										sendReply_fail("ERROR when selecting a host, check logs of the logical queue factory");
									} else {
										// Select a random host
										logger.info("Original host lost: " + hostToRecover);
										// TODO Select the less loaded host
										host = hostsList.get((int) Math.random() % hostsList.size());
										// Create a transaction with the new host
										zk.createQTransaction(qName, host);
										hostFound = true;
									}
								} else {
									hostFound = true;
								}
								logger.info("Recover process for queue " + qName + " started " +
										"on HCM: " + host);
							}
							else {
								// We are not in recovery mod
								if (hostsList.isEmpty()) {
									sendReply_fail("ERROR when selecting a host, check logs of the logical queue factory");
								} else {
									// Select a random host
									// TODO Select the less loaded host
									// We create a transaction
									host = hostsList.get((int) Math.random() % hostsList.size());
									zk.createQTransaction(qName, host);
									hostFound = true;
								}
							}
							// if we found a host on which put the queue
							if (hostFound) {
								// Just create queue the timer will update the
								// management server configuration
								int code = factory.createQueue(qName, host, recoveryMod);
								if (code == -1) {
									// We remove the transaction if the queue already exists
									zk.removeQTransaction(qName);
									sendReply_fail("The queue already exists");
								} else if (code == -2) {
									sendReply_fail("The HCM looks to be unavailble");
								} else if (code == -3) {
									sendReply_fail("The Queue creation process has failed on GCM or HCM");
								} else {
									// Transaction completed
									zk.removeQTransaction(qName);
									sendReply_ok("SUCCESS");
								}
								logger.debug("Create queue name = " + qName + " on " + host);
							}
							break;

						case RoQConstant.BSON_CONFIG_STOP_QUEUE:
							logger.debug("Stop queue Request");
							// Stopping a queue is just clearing its "running" flag
							// from the global configuration
							// 1. Check the request
							if (!checkField(request, "QName")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>");
								break;
							}
							// 2. Get the Qname
							qName = (String) request.get("QName");
							logger.debug("Stop queue name = " + qName);
							// 3. Remove the queue
							if (!this.factory.stopQueue(qName)) {
								sendReply_fail("ERROR when stopping Running queue, check logs of the logical queue factory");
							} else {
								sendReply_ok("SUCCESS");
							}
							break;

						case RoQConstant.BSON_CONFIG_CREATE_XCHANGE:
							logger.debug("Create Xchange request");
							if (!checkField(request, "QName")
								|| !checkField(request, "Host") || !checkField(request, "TransactionID")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>, <Host>, <TransactionID>");
								break;
							}
							// 1. Get the host
							host = (String) request.get("Host");
							qName = (String) request.get("QName");
							// The transaction ID is created by the user and must be reused while the 
							// exchange has not been created. The creation can fail for many reason
							// This ID is the best way to ensure that Exchange creation is idempotent.
							// An ID could be IPadress:randomNumber
							transID = (String) request.get("TransactionID");
							hostToRecover = zk.exchangeTransactionExists(transID);
							if (hostToRecover == null) {
								zk.createExchangeTransaction(transID, host);
							} else {
								host = hostToRecover;
							}							
							logger.debug("Create Xchange on queue " + qName + " on " + host);
							if (!this.factory.createExchange(qName, host, transID)) {
								sendReply_fail("ERROR when creating Xchange check logs of the logical queue factory");
							} else {
								sendReply_ok("SUCCESS");
							}
							break;

						/*
						 * Auto scaling configuration requests
						 */
						case RoQConstant.BSON_CONFIG_GET_AUTOSCALING_RULE:
							logger.debug("GET autoscaling rule request received ...");
							if (!checkField(request, "QName")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>");
								break;
							}
							// 1. Get the qname
							qName = (String) request.get("QName");
							logger.debug("Getting autoscaling configuration for " + qName);
							
							byte[] temp;
							config = new AutoScalingConfig();
							queue = new Metadata.Queue(qName);
							
							temp = zk.getNameScalingConfig(queue);
							if (temp != null) {
								config.setName(new String(temp));
							}
							temp = zk.getHostScalingConfig(queue);
							if (temp != null) {
								config.hostRule = serializationUtils.deserializeObject(temp);
							}
							temp = zk.getExchangeScalingConfig(queue);
							if (temp != null) {
								config.xgRule   = serializationUtils.deserializeObject(temp);
							}
							temp = zk.getQueueScalingConfig(queue);
							if (temp != null) {
								config.qRule    = serializationUtils.deserializeObject(temp);
							}
							
							if ((config.hostRule == null)
									&& (config.xgRule == null)
									&& (config.qRule  == null)) {
								sendReply_fail("ERROR NO auto scaling rule defined for this queue");
							} else {
								mngtRepSocket.send(
										this.serializer.serialiazeAutoScalingConfigAnswer(qName, config), 0);
							}
							break;
						case RoQConstant.BSON_CONFIG_REGISTER_FOR_AUTOSCALING_RULE_UPDATE:
							logger.debug("REGISTER FOR  autoscaling rule request received ...");
							if (!checkField(request, "QName") && !checkField(request, "Address")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>, <Address>");
								break;
							}
							try {
								// 1. Get the qname
								qName = (String) request.get("QName");
								String addressCallBack = (String) request.get("Address");
								logger.debug("REGISTER autoscaling configuration update  for " + qName
										+ " on call back " + addressCallBack);
								// add the socket for the listener if it not already exists
								if (!this.scalingConfigListener.containsKey(qName)) {
									// 2. Register the address
									// 2.1 Create a push socket To check if the bind can be done at the pull side
									ZMQ.Socket push = context.socket(ZMQ.PUSH);
									// WARNING no check of address format && check that the bind can be done at the client level
									push.connect(addressCallBack);
									// 2.2 Add the socket in the hash map
									this.scalingConfigListener.put(qName, push);
								}
								sendReply_ok("Registrated on "+addressCallBack);
							} catch (Exception e) {
								logger.error("Error when registrating the listener, the request was " + request, e);
								sendReply_fail(e.getMessage());
							}
							break;

						case RoQConstant.BSON_CONFIG_ADD_AUTOSCALING_RULE:
							logger.debug("ADD autoscaling rule request received ...");
							if (!checkField(request, "QName")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>");
								break;
							}
							// 1. Get the qname
							config = new AutoScalingConfig();
							qName = (String) request.get("QName");
							logger.debug("Adding autoscaling configuration for " + qName);
							
							// Set host config name
							config.setName((String) request.get(RoQConstant.BSON_AUTOSCALING_CFG_NAME));
							
							// Decode host rule if it is present in the request
							BSONObject hostRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_HOST);
							if (hostRule != null) {
								config.setHostRule(new HostScalingRule(
										((Integer) hostRule.get(RoQConstant.BSON_AUTOSCALING_HOST_RAM)).intValue(),
										((Integer) hostRule.get(RoQConstant.BSON_AUTOSCALING_HOST_CPU)).intValue()));
								logger.debug("Host scaling rule decoded : " + config.getHostRule().toString());
							}
							
							// Decode exchange rule if it is present in the request
							BSONObject exchangeRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_XCHANGE);
							if (exchangeRule != null) {
								config.setXgRule(
										new XchangeScalingRule(
												((Integer) exchangeRule.get(RoQConstant.BSON_AUTOSCALING_XCHANGE_THR)).intValue(),
												0));
								logger.debug("Xchange scaling rule : " + config.getXgRule().toString());
							}
							
							// Decode queue rule if it is present in the request
							BSONObject queueRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_QUEUE);
							if (queueRule != null) {
								config.setqRule(
										new LogicalQScalingRule(
												((Integer) queueRule.get(RoQConstant.BSON_AUTOSCALING_Q_PROD_EXCH)).intValue(),
												((Integer) queueRule.get(RoQConstant.BSON_AUTOSCALING_Q_THR_EXCH)).intValue()));
								logger.debug("Queue scaling rule : " + config.getqRule().toString());
							}
							
							// save scaling config to zookeeper
							queue = new Metadata.Queue(qName);
							zk.setNameScalingConfig(config.getName(), queue);
							zk.setHostScalingConfig(serializationUtils.serialiseObject(config.hostRule), queue);
							zk.setExchangeScalingConfig(serializationUtils.serialiseObject(config.xgRule), queue);
							zk.setQueueScalingConfig(serializationUtils.serialiseObject(config.qRule), queue);
							
							//The scaling rule has been updated need to check whether there was a listener for this Q
							if(this.scalingConfigListener.containsKey(qName)){
								this.logger.info("New scaling rule have been defined for "+ qName +": notifying the listener");
								this.scalingConfigListener.get(qName).send(
										this.serializer.serialiazeAutoScalingConfigAnswer(qName, config),
										0);
							}
							sendReply_ok("The autoscaling config has been created for the Q " + qName);
							break;
							
							//Request for getting the properties of the cloud user
						case RoQConstant.BSON_CONFIG_GET_CLOUD_PROPERTIES:
							logger.debug("GET Cloud properties request...");
							
							// First, Get the cloud config from ZooKeeper
							byte[] serializedConfig = zk.getCloudConfig();
							if (serializedConfig == null) {
								mngtRepSocket.send(
										serializer.serialiazeConfigAnswer(RoQConstant.FAIL, "The property object is null"), 0);
								break;
							}
							
							// Then, build a BSON object
							CloudConfig cloudConfig = serializationUtils.deserializeObject(serializedConfig);
							BSONObject prop = new BasicBSONObject();
							prop.put("cloud.use", cloudConfig.inUse);
							if(cloudConfig.inUse){
								prop.put("cloud.user",     cloudConfig.user);
								prop.put("cloud.password", cloudConfig.password);
								prop.put("cloud.endpoint", cloudConfig.endpoint);
								prop.put("cloud.gateway",  cloudConfig.gateway);
								prop.put("RESULT",RoQConstant.OK );
							}
							
							// Send it
							logger.debug("Sending cloud properties to client :"+ prop.toString());
							mngtRepSocket.send(
									BSON.encode(prop), 0);
							break;
						default:
							mngtRepSocket.send(
									serializer.serialiazeConfigAnswer(RoQConstant.FAIL, "INVALID CMD Value"), 0);
							break;
						}
					}
				} catch (Exception e) {
					logger.error("Error when receiving message", e);
				}
			}
			// If connection with zookeeper has been lost
			if (!GlobalConfigurationManager.hasLead) {
				// We wait that process become master
				logger.info("connection with ZK has been lost, " +
						"waiting for leadership");
				// Stop to send update
				this.controllerTimer.cancel();
				this.publicationTimer.cancel();
				this.publicationTimer.purge();
				
				// While we have not the lead we respond
				// that the leader has changed
				while (!GlobalConfigurationManager.hasLead) {
					poller.poll(100);
					if (poller.pollin(0)) {
						mngtRepSocket.recv(0);
						mngtRepSocket.send(
								serializer.serialiazeConfigAnswer(
									RoQConstant.EVENT_LEAD_LOST, ""),
								0);
					}
				}
				// Restart timer
				startMngtControllerTimer();
				logger.info("connection with ZK has been re-etablished" +
						" MngmtController restarted");
			}
		}// END OF THE LOOP
		cleanStop();
		logger.info("Management controller stopped.");
	}
	
	private void sendReply_ok(String string) {
		mngtRepSocket.send(
			serializer.serialiazeConfigAnswer(
				RoQConstant.OK, string),
			0);
	}

	private void sendReply_fail(String string) {
		mngtRepSocket.send(
			serializer.serialiazeConfigAnswer(
				RoQConstant.FAIL, string),
			0);
	}

	/**
	 * Clean all the timers and socket of the thread.
	 */
	private void cleanStop() {
		logger.info("Clean STOP of the Management Controller");
		logger.debug("Canceling timers ...");
		this.controllerTimer.cancel();
		this.publicationTimer.cancel();
		this.publicationTimer.purge();
		// This call was commented out because stopping a GCM
		// does not mean that hosts are also stopped
		// logger.debug("Removing host configurations ...");
		// this.cleanMngtConfig();
		logger.debug("Closing sockets ...");
		this.mngtRepSocket.setLinger(0);
		this.mngtRepSocket.close();
		
	}
	
	/**
	 * @param socket
	 *            the socket to check
	 * @return true if a mutli part is recieved.
	 */
	private boolean isMultiPart(Socket socket) {
		boolean isMulti = false;
		while (socket.hasReceiveMore() && this.active) {
			logger.info("WAITING FOR Multi part message ...");
			isMulti = true;
		}
		return isMulti;
	}

	/**
	 * Checks whether the field is present in the BSON request
	 * 
	 * @param request
	 *            the request
	 * @param field
	 *            the field to check
	 * @return true if the field is present, false otherwise, in addition it
	 *         sends a INVALI answer.
	 */
	private boolean checkField(BSONObject request, String field) {
		if (!request.containsField(field)) {
			mngtRepSocket.send(
					serializer.serialiazeConfigAnswer(RoQConstant.FAIL, "The " + field
							+ "  field is not present, INVALID REQUEST"), 0);
			logger.error("Invalid request, does not contain " + field + " field.");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		logger.info("Stopping " + this.getClass().getName() + " cleaning sockets");
		this.active = false;
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return this.getClass().getName() + " Server";
	}

	/**
	 * @return the shutDownMonitor
	 */
	public ShutDownMonitor getShutDownMonitor() {
		return shutDownMonitor;
	}
	
	public ArrayList<String> getHosts() throws Exception {
		// Get the hosts
		List<Metadata.HCM> hosts = zk.getHCMList();
		ArrayList<String> hostAddresses = new ArrayList<String>();
		for (Metadata.HCM hcm : hosts) {
			hostAddresses.add(hcm.address);
		}
		return hostAddresses;
	}

	public ArrayList<QueueManagementState> getQueues() {
		ArrayList<QueueManagementState> queues = new ArrayList<QueueManagementState>();
		
		for (Metadata.Queue queue : zk.getQueueList()) {
			queues.add(new QueueManagementState(
					queue.name,
					zk.getHCM(queue).address,
					zk.isRunning(queue)));
		}
		return queues;
	}

	public QueueManagementState getQueue(String queueName) {
		Metadata.Queue queue = new Metadata.Queue(queueName);
		
		QueueManagementState qms = new QueueManagementState(
				queue.name,
				zk.getHCM(queue).address,
				zk.isRunning(queue));
		return qms;
	}
}
