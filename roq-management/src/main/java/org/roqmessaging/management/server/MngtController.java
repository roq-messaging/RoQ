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

import java.sql.DriverManager;
import java.sql.SQLException;
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
	private ZMQ.Socket mngtSubSocket = null;
	private ZMQ.Socket mngtRepSocket = null;
	// Logical queue factory
	private IRoQLogicalQueueFactory factory = null;
	// running
	private volatile boolean active = true;
	private ShutDownMonitor shutDownMonitor = null;
	// utils for serialization
	private RoQSerializationUtils serializationUtils = null;
	// Management infra
	private MngtServerStorage storage = null;
	// The BSON serialiazer
	private IRoQSerializer serializer = null;
	// The DB file
	private String dbName = "Management.db";
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
	public MngtController(String globalConfigAddress, String dbName, int period, GCMPropertyDAO props, RoQZooKeeperClient zk) {
		try {
			this.period = period;
			this.dbName = dbName;
			this.properties= props;
			this.scalingConfigListener = new HashMap<String, ZMQ.Socket>();
			this.zk = zk;
			
			// Get the various ZMQ ports used by the MngtController
			int interfacePort = properties.ports.get("MngtController.interface");
			int shutDownPort  = properties.ports.get("MngtController.shutDown");
			int globalConfigTimerPort  = properties.ports.get("GlobalConfigTimer.pub");
			
			// Initialize the connections
			init(globalConfigAddress, interfacePort, shutDownPort, globalConfigTimerPort);
		} catch (SQLException e) {
			logger.error("Error while initiating the SQL connection", e);
		} catch (ClassNotFoundException e) {
			logger.error("Error while initiating the SQL connection", e);
		}
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
	 * @param globalConfigTimerPort
	 *            the port on which the GlobalConfigTimer periodically publishes its data
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 *             whenthe JDBC driver has not been found
	 */
	private void init(String globalConfigAddress, int interfacePort, int shutDownPort, int globalConfigTimerPort) 
			throws SQLException, ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		// Init ZMQ subscriber API for configuration
		context = ZMQ.context(1);
		mngtSubSocket = context.socket(ZMQ.SUB);
		mngtSubSocket.connect("tcp://" + globalConfigAddress + ":" + globalConfigTimerPort);
		mngtSubSocket.subscribe("".getBytes());
		// Init ZMQ subscriber API for configuration
		mngtRepSocket = context.socket(ZMQ.REP);
		mngtRepSocket.bind("tcp://*:"+interfacePort);
		// init variable
		this.serializationUtils = new RoQSerializationUtils();
		this.storage = new MngtServerStorage(DriverManager.getConnection("jdbc:sqlite:" + this.dbName));
		this.factory = new LogicalQFactory(globalConfigAddress);
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
		poller.register(mngtSubSocket);// 0
		poller.register(mngtRepSocket);
		// Init variables
		// int infoCode = 0;

		// Start the running loop
		while (this.active) {
			// Set the poll time out, it returns either when something arrive or
			// when it time out
			poller.poll(100);
			/*
			if (poller.pollin(0)) {
				logger.debug("Recieving Message in the update broadcast update channel");
				// An event arrives start analysing code
				String info = new String(mngtSubSocket.recv(0));
				// Check if exchanges are present: this happens when the queue
				// is shutting down a client is asking for a
				// connection
				infoCode = Integer.parseInt(info);
				switch (infoCode) {
				case RoQConstant.MNGT_UPDATE_CONFIG:
					// Infocode, map(Q Name, host)
					logger.debug("Recieving update configuration message");
					if (mngtSubSocket.hasReceiveMore()) {
						// The new hashmap QName, target host location
						HashMap<String, String> newConfig = this.serializationUtils.deserializeObject(mngtSubSocket
								.recv(0));
						// The lis of hosts
						List<String> hosts = this.serializationUtils.deserializeObject(mngtSubSocket.recv(0));
						try {
							storage.updateConfiguration(newConfig, hosts);
						} catch (SQLException e) {
							logger.error("Error while updating the configuration", e);
						}
					} else {
						// Problem expected map here
						logger.error("Error, expected map of (Qname, host", new IllegalStateException(
								"Expected a second message" + " in the envelope"));
					}
					break;
				default:
					break;
				}
			}
			*/
			// Pollin 1 = Management facade interface in BSON
			if (poller.pollin(1)) {
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
								sendReply_fail("ERROR when stopping Running queue, check logs of the logical queue factory");
							}
							break;
						/*
						case RoQConstant.BSON_CONFIG_REMOVE_QUEUE:
							logger.debug("Processing a REMOVE QUEUE REQUEST");
							if (!checkField(request, "QName")) {
								break;
							}
							qName = (String) request.get("QName");
							logger.debug("Remove " + qName);
							// Removing Q
							// 1. Check whether the queue is running
							try {
								QueueManagementState state = this.storage.getQueue(qName);
								if (state != null) {
									if (state.isRunning()) {
										// 2. if running ask the global
										// configuration
										// manager to remove it
										if (!this.factory.removeQueue(qName)) {
											mngtRepSocket
													.send(serializer
															.serialiazeConfigAnswer(RoQConstant.FAIL,
																	"ERROR when stopping Running queue, check logs of the logical queue factory"),
															0);
										} else {
											mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.OK,
													"The queue has been removed."), 0);
										}
										break;
									}
								} else {
									mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.FAIL,
											"ERROR when stopping Running queue, the name does not exist"), 0);
									break;
								}
								// 3. ask the storage manager to remove it
								this.storage.removeQueue(qName);
								// 4. send back OK
								mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.OK, "SUCCESS"), 0);
							} catch (Exception e) {
								logger.error("Error while processing the REMOVE Q", e);
							}
							break;
						 	*/
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
							// 2. Get the queue in DB
							if (!factory.startQueue(qName)) {
								sendReply_fail("ERROR when starting  queue, check logs of the logical queue factory");
							} else {
								sendReply_ok("SUCCESS");
							}
							
							logger.debug("Create queue name = " + qName + " on " + host + " from a start command");
							break;
						/*
						case RoQConstant.BSON_CONFIG_START_QUEUE:
							logger.info("Start  Q Request ... checking whether the Queue is known ...");
							logger.debug("Incoming request:" + request.toString());
							// Starting a queue
							// Just create a queue on a host
							if (!checkField(request, "QName")) {
								break;
							}
							// 1. Get the name
							qName = (String) request.get("QName");
							// 2. Get the queue in DB
							QueueManagementState queue = this.storage.getQueue(qName);
							// 3. Get the host
							if (queue == null) {
								mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.FAIL,
										"ERROR when starting  queue, the queue " + qName + " is not present in DB"), 0);
							} else {
								host = queue.getHost();
								// Just create queue the timer will update the
								// management server configuration
								if (!factory.createQueue(qName, host)) {
									mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.FAIL,
											"ERROR when starting  queue, check logs of the logical queue factory"), 0);
								} else {
									mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.OK, "SUCCESS"), 0);
								}
							}

							logger.debug("Create queue name = " + qName + " on " + host + " from a start command");
							break;
							*/
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
							// Just create queue the timer will update the
							// management server configuration
							if (!factory.createQueue(qName, host)) {
								sendReply_fail("ERROR when starting  queue, check logs of the logical queue factory");
							} else {
								sendReply_ok("SUCCESS");
							}
							logger.debug("Create queue name = " + qName + " on " + host);
							break;

						case RoQConstant.BSON_CONFIG_STOP_QUEUE:
							logger.debug("Stop queue Request");
							// Stopping a queue is just removing from the global
							// configuration
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
								|| !checkField(request, "Host")) {
								sendReply_fail("ERROR: Missing field in the request: <QName>, <Host>");
								break;
							}
							// 1. Get the host
							host = (String) request.get("Host");
							qName = (String) request.get("QName");
							logger.debug("Create Xchange on queue " + qName + " on " + host);
							if (!this.factory.createExchange(qName, host)) {
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
							
							AutoScalingConfig config = zk.getScalingConfig(new Metadata.Queue(qName));
							if (config != null) {
								mngtRepSocket.send(
										this.serializer.serialiazeAutoScalingConfigAnswer(qName, config), 0);
							} else {
								sendReply_fail("ERROR NO auto scaling rule defined for this queue");
							}
							break;
						/*	
						case RoQConstant.BSON_CONFIG_GET_AUTOSCALING_RULE:
							logger.debug("GET autoscaling rule request received ...");
							if (!checkField(request, "QName"))
								break;
							// 1. Get the qname
							qName = (String) request.get("QName");
							logger.debug("Getting autoscaling configuration for " + qName);
							// 2. Get the queue management state
							QueueManagementState queueS = this.storage.getQueue(qName);
							if (queueS == null) {
								mngtRepSocket
										.send(serializer
												.serialiazeConfigAnswer(RoQConstant.FAIL,
														"ERROR when creating autoscaling rule, the queue name does not exist in DB"),
												0);
							} else {
								// 3. If the reference exist get the auto
								// scaling config
								if (queueS.getAutoScalingCfgRef() != null) {
									AutoScalingConfig config = this.storage.getAutoScalingCfg(queueS
											.getAutoScalingCfgRef());
									if (config != null) {
										mngtRepSocket.send(
												this.serializer.serialiazeAutoScalingConfigAnswer(qName, config), 0);
									} else {
										mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.FAIL,
												"ERROR NO auto scaling rule defined for this queue"), 0);
									}
								}
								queueS = null;
							}
							break;
							*/
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
								// 2. Register the address
								// 2.1 Create a push socket To check if the bind can be done at the pull side
								ZMQ.Socket push = context.socket(ZMQ.PUSH);
								// WARNING no check of address format && check that the bind can be done at the client level
								push.connect(addressCallBack);
								// 2.2 Add the socket in the hash map
								this.scalingConfigListener.put(qName, push);
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
							AutoScalingConfig config = new AutoScalingConfig();
							qName = (String) request.get("QName");
							logger.debug("Adding autoscaling configuration for " + qName);
							
							// Decode host rule if it is present in the request
							BSONObject hostRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_HOST);
							if (hostRule != null) {
								config.setHostRule(
										new HostScalingRule(
												((Integer) hostRule.get(RoQConstant.BSON_AUTOSCALING_HOST_RAM)).intValue(),
												((Integer) hostRule.get(RoQConstant.BSON_AUTOSCALING_HOST_CPU)).intValue()));
								logger.debug("Host scaling rule decoded : " + config.getHostRule().toString());
							}
							
							// Decode exchange rule if it is present in the request
							BSONObject exchangeRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_XCHANGE);
							if (exchangeRule != null) {
								config.setXgRule(
										new XchangeScalingRule(
												((Integer) hostRule.get(RoQConstant.BSON_AUTOSCALING_XCHANGE_THR)).intValue(),
												0));
								logger.debug("Xchange scaling rule : " + config.getXgRule().toString());
							}
							
							// Decode queue rule if it is present in the request
							BSONObject queueRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_QUEUE);
							if (queueRule != null) {
								config.setqRule(
										new LogicalQScalingRule(
												((Integer) hostRule.get(RoQConstant.BSON_AUTOSCALING_Q_PROD_EXCH)).intValue(),
												((Integer) hostRule.get(RoQConstant.BSON_AUTOSCALING_Q_THR_EXCH)).intValue()));
								logger.debug("Queue scaling rule : " + config.getqRule().toString());
							}
							
							if (!factory.setScalingRule(config)) {
								sendReply_fail("ERROR when setting scaling rule");
							}
							
							//The scaling rule has been updated need to check whether there was a listener for this Q
							if(this.scalingConfigListener.containsKey(qName)){
								this.logger.info("New scaling rule have been defined for "+ qName +": notifying the listener");
								this.scalingConfigListener.get(qName).send(
										this.serializer.serialiazeAutoScalingConfigAnswer(qName, config),
										0);
							}
							sendReply_ok("The autoscaling config has been created for the Q " + qName);
							break;
							
						/*
						case RoQConstant.BSON_CONFIG_ADD_AUTOSCALING_RULE:
							logger.debug("ADD autoscaling rule request received ...");
							if (!checkField(request, "QName")
									|| ((!request.containsField(RoQConstant.BSON_AUTOSCALING_HOST))
											&& (!request.containsField(RoQConstant.BSON_QUEUES)) && (!request
												.containsField(RoQConstant.BSON_QUEUES)))) {
								break;
							}
							// 1. Get the qname
							qName = (String) request.get("QName");
							logger.debug("Adding autoscaling configuration for " + qName);
							// 2. Check if the q exist
							QueueManagementState queueState = this.storage.getQueue(qName);
							if (queueState == null) {
								mngtRepSocket
										.send(serializer
												.serialiazeConfigAnswer(RoQConstant.FAIL,
														"ERROR when creating autoscaling rule, the queue name does not exist in DB"),
												0);
								break;
							} else {
								// For the Host scaling rule
								AutoScalingConfig config = new AutoScalingConfig();
								config.setName((String) request.get(RoQConstant.BSON_AUTOSCALING_CFG_NAME));
								if (config.getName() == null) {
									mngtRepSocket
											.send(serializer
													.serialiazeConfigAnswer(RoQConstant.FAIL,
															"ERROR when creating autoscaling rule, the configuration name in BSON msg is NULL"),
													0);
									break;
								}
								// Check if it exist- security check for
								// avoiding rollbacking
								if (storage.getAutoScalingCfg(config.getName()) != null) {
									mngtRepSocket
											.send(serializer
													.serialiazeConfigAnswer(RoQConstant.FAIL,
															"ERROR when creating autoscaling rule, the auto scaling configuration name already exist and must be unique."),
													0);
									break;
								}
								// Init the FK id
								int qRuleID = -1, xRuleID = -1, hRuleID = -1;

								// 3.1. Decoding all rules and storing them in
								// DB
								// Decode host rule
								BSONObject hRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_HOST);
								if (hRule != null) {
									config.setHostRule(new HostScalingRule(((Integer) hRule
											.get(RoQConstant.BSON_AUTOSCALING_HOST_RAM)).intValue(), ((Integer) hRule
											.get(RoQConstant.BSON_AUTOSCALING_HOST_CPU)).intValue()));
									logger.debug("Host scaling rule decoded : " + config.getHostRule().toString());
									hRuleID = this.storage.addAutoScalingRule(config.getHostRule());
									if (hRuleID != -1) {
										config.getHostRule().setID(hRuleID);
									} else {
										mngtRepSocket
												.send(serializer
														.serialiazeConfigAnswer(RoQConstant.FAIL,
																"ERROR when creating autoscaling rule in DB - the Row ID has been returned as -1"),
														0);
										break;
									}
								}
								// Decode Xchange rule
								BSONObject xRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_XCHANGE);
								if (xRule != null) {
									config.setXgRule(new XchangeScalingRule(((Integer) xRule
											.get(RoQConstant.BSON_AUTOSCALING_XCHANGE_THR)).intValue(), 0f));
									logger.debug("Xchange scaling rule : " + config.getHostRule().toString());
									xRuleID = this.storage.addAutoScalingRule(config.getXgRule());
									if (xRuleID != -1) {
										config.getXgRule().setID(xRuleID);
									} else {
										rollbacklHrule(hRule, hRuleID);
										mngtRepSocket
												.send(serializer
														.serialiazeConfigAnswer(RoQConstant.FAIL,
																"ERROR when creating autoscaling rule in DB - the Row ID has been returned as -1"),
														0);
										break;
									}
								}
								// decode Q rule
								BSONObject qRule = (BSONObject) request.get(RoQConstant.BSON_AUTOSCALING_QUEUE);
								if (qRule != null) {
									config.setqRule(new LogicalQScalingRule(((Integer) qRule
											.get(RoQConstant.BSON_AUTOSCALING_Q_PROD_EXCH)).intValue(),
											((Integer) qRule.get(RoQConstant.BSON_AUTOSCALING_Q_THR_EXCH)).intValue()));
									logger.debug("Host scaling rule : " + config.getHostRule().toString());
									// 3.1. Create the auto scaling
									// configuration
									qRuleID = this.storage.addAutoScalingRule(config.getqRule());
									if (qRuleID != -1) {
										config.getqRule().setID(qRuleID);
									} else {
										rollbacklHrule(hRule, hRuleID);
										rollbacklXrule(xRule, xRuleID);
										mngtRepSocket
												.send(serializer
														.serialiazeConfigAnswer(RoQConstant.FAIL,
																"ERROR when creating autoscaling rule in DB - the Row ID has been returned as -1"),
														0);
										break;
									}
								}
								// 3.2. Create an auto scaling rule config
								if ((qRuleID != -1) || (xRuleID != -1) || (hRuleID != -1)) {
									if (this.storage.addAutoScalingConfig(config.getName(),
											hRuleID == -1 ? 0 : hRuleID, qRuleID == -1 ? 0 : qRuleID, xRuleID == -1 ? 0
													: xRuleID) == false) {
										// this means we did not manage to
										// create the configuration=> rollback
										rollbacklHrule(hRule, hRuleID);
										rollbacklXrule(xRule, xRuleID);
										rollbacklQrule(qRule, qRuleID);
										mngtRepSocket
												.send(serializer
														.serialiazeConfigAnswer(RoQConstant.FAIL,
																"ERROR when creating autoscaling rule in DB - the rule name and configuration cannot be created. See log files."),
														0);
										break;
									}
									// 3.3. Update the queue to point to this
									// configuration
									if (this.storage.updateAutoscalingQueueConfig(qName, config.getName()) == false) {
										rollbacklHrule(hRule, hRuleID);
										rollbacklXrule(xRule, xRuleID);
										rollbacklQrule(qRule, qRuleID);
										mngtRepSocket
												.send(serializer
														.serialiazeConfigAnswer(
																RoQConstant.FAIL,
																"ERROR when creating autoscaling rule in DB - the auto scaling rule cannot be associated with the queue. See log files."),
														0);
										break;
									}else{
										//The scaling rule has been updated need to check whether there was a listener for this Q
										if(this.scalingConfigListener.containsKey(qName)){
											this.logger.info("New scaling rule have been defined for "+ qName +": notifying the listener");
											this.scalingConfigListener.get(qName).send(this.serializer.serialiazeAutoScalingConfigAnswer(qName, config), 0);
										}
									}
								} else {
									logger.warn("Huum strange, no rules has been defined for this scaling rule.");
								}
								mngtRepSocket.send(
										serializer.serialiazeConfigAnswer(RoQConstant.OK,
												"The autoscaling " + config.getName() + " has been created for the Q "
														+ qName), 0);
							}
							break;
							*/
							
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
		this.mngtSubSocket.setLinger(0);
		this.mngtRepSocket.setLinger(0);
		this.mngtSubSocket.close();
		this.mngtRepSocket.close();
		
	}

	/**
	 * @param qRule
	 *            the bson object representing the rule
	 * @param qRuleID
	 *            the rule ID
	 */
	private void rollbacklQrule(BSONObject qRule, int qRuleID) {
		if (qRule != null) {
			logger.debug("Rolling back q rule");
			try {
				this.storage.removeQScalingRuleByID(qRuleID);
			} catch (SQLException e) {
				logger.error("Error while rollbacking Qrule", e);
			}
		}
	}

	/**
	 * @param xRule
	 *            the bson object representing the rule
	 * @param xRuleID
	 *            the rule ID
	 */
	private void rollbacklXrule(BSONObject xRule, int xRuleID) {
		if (xRule != null) {
			logger.debug("Rolling back Xchange  rule");
			try {
				this.storage.removeXchangeScalingRuleByID(xRuleID);
			} catch (SQLException e) {
				logger.error("Error while rollbacking Xchange rule", e);
			}
		}
	}

	/**
	 * @param hRule
	 *            the bson host rule
	 * @param hRuleID
	 *            the rule ID
	 */
	private void rollbacklHrule(BSONObject hRule, int hRuleID) {
		if (hRule != null) {
			logger.debug("Rolling back Xchange  rule");
			try {
				this.storage.removeHostcalingRuleByID(hRuleID);
			} catch (SQLException e) {
				logger.error("Error while rollbacking Xchange rule", e);
			}
		}
	}

	/**
	 * Clean the management configuration by removing non persistant information
	 * such as the host name that must be dynamic when hosts starts.
	 */
	/*
	private void cleanMngtConfig() {
		
		try {
			// this.storage.removeHosts();
		} catch (SQLException e) {
			logger.error("Error when deleting the hosts from configuration", e);
		}
		
	}
	*/
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

	/**
	 * @return the storage
	 */
	public MngtServerStorage getStorage() {
		return storage;
	}

	/**
	 * @param storage
	 *            the storage to set
	 */
	public void setStorage(MngtServerStorage storage) {
		this.storage = storage;
	}

}
