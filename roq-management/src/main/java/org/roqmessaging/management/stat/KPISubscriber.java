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
package org.roqmessaging.management.stat;

import java.net.ConnectException;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.RoQGCMConnection;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.utils.Time;
import org.roqmessaging.zookeeper.RoQZKSimpleConfig;
import org.roqmessaging.zookeeper.RoQZooKeeper;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * Class KPISubscriber
 * <p> Description: This class is a generic KPI subscriber class that connect to the GCM, get the location of
 *  the stat monitor and subscribe to the general stat channel. The method {@link #processStat(Integer, BSONObject)} 
 *  must be implemented by extending classes (client classes).
 * 
 * @author sskhiri
 */
public abstract class KPISubscriber implements Runnable, IStoppable{
	//ZMQ configuration
	protected ZMQ.Context context = null;
	//KPI socket
	protected ZMQ.Socket kpiSocket = null;
	//The configuration server
	protected int gcm_interfacePort = 0;
	//the Qname to subscriber
	protected String qName = null;
	//Define whether the thread must continue to run
	protected volatile boolean active = true;
	
	//the logger
	protected Logger logger = Logger.getLogger(KPISubscriber.class);
	
	protected RoQZooKeeper zkClient;
	
	private RoQGCMConnection gcmConnection;
	
	/**
	 * @param gcm_address the IP address of the global configuration
	 * @param gcm_interfacePort the port used by the GCM for the interface to the topology
	 * @param qName the queue from which we want receive statistic. 
	 */
	public KPISubscriber(String zk_address, int gcm_interfacePort, String qName) {
		try {
			// ZMQ Init
			logger.debug("Init ZMQ context");
			this.context = ZMQ.context(1);
			// Copy parameters
			this.gcm_interfacePort = gcm_interfacePort;
			this.qName = qName;
			RoQZKSimpleConfig zkConf = new RoQZKSimpleConfig();
			zkConf.servers = zk_address;
			// ZK INIT
			zkClient = new RoQZooKeeper(zkConf);
			zkClient.start();
			gcmConnection = new RoQGCMConnection(zkClient, 50, 4000);
		} catch (Exception e) {
			logger.error("Error while initiating the KPI statistic channel", e);
		}
	}
	
	/**
	 * Subscribe to the statistic stream got from the global configuration
	 * @return true if the subscription succeed, false in the other cases.
	 * @throws IllegalStateException if the monitor stat is not present in the cache
	 * @throws ConnectException 
	 */
	public boolean subscribe() throws IllegalStateException, ConnectException {
		logger.debug("Get the stat monitor address from the GCM");

		String monitorStatServer = addSubscriberToGCM();
		if (monitorStatServer == null)
			throw new IllegalStateException("monitor not found for KPI subscriber");
		logger.debug("Got the Stat monitor address @"+ monitorStatServer);
		
		// 2. Register a socket to the stat monitor
		kpiSocket = context.socket(ZMQ.SUB);
		kpiSocket.setReceiveTimeOut(100);
		kpiSocket.connect(monitorStatServer);
		kpiSocket.subscribe("".getBytes());
		logger.debug("Connected to Stat monitor " + monitorStatServer);
		return true;
	}
	
	public String addSubscriberToGCM() throws ConnectException {
		// 1.2 Send the request
		// Prepare the request BSON object
		BSONObject request = new BasicBSONObject();
		request.put("CMD", RoQConstant.BSON_CONFIG_GET_HOST_BY_QNAME);
		request.put("QName", qName);
		//Send 
		byte[] configuration = gcmConnection.sendRequest(BSON.encode(request), 5000);
		//Decode answer
		BSONObject dConfiguration = BSON.decode(configuration);
		//Check the error code
		if(dConfiguration.containsField("RESULT")){
			if((int)dConfiguration.get("RESULT") == RoQConstant.FAIL){
				logger.warn("The subscribe request failed because of the queue configuration: "+ dConfiguration.get("COMMENT"));
				return null;
			}
		}
		return (String) dConfiguration.get(RoQConstant.BSON_STAT_MONITOR_HOST);
	}

	/**
	 * Delegates the process stat to the client class.
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		ZMQ.Poller poller = new ZMQ.Poller(1);
		Long lastSubscribe = Time.currentTimeMillis();
		poller.register(kpiSocket);
			while (active) {
				
				// Allows to continually receive the rules
				// from the statMonitor, even after a failover
				if (Time.currentTimeMillis() - lastSubscribe > 10000) {
					try {
						addSubscriberToGCM();
					} catch (ConnectException e) {
						e.printStackTrace();
					}
				}
				poller.poll(100);
				if (poller.pollin(0)) {
					do {
						if(active){
							// Stat coming from the KPI stream
							BSONObject statObj = BSON.decode(kpiSocket.recv(0));
							logger.debug("Start analysing info code " + statObj.get("CMD"));
							processStat((Integer) statObj.get("CMD"), statObj, kpiSocket);
						}else break;
						
					} while (kpiSocket.hasReceiveMore());
				}
			}
			this.kpiSocket.setLinger(0);
			this.kpiSocket.close();
	}
	
	/**
	 * Checks whether the field is present in the BSON request
	 * @param request the request
	 * @param field the field to check
	 * @return true if the field is present, false otherwise, in addition it sends a INVALID answer.
	 */
	protected boolean checkField(BSONObject request, String field) throws AssertionError {
		if (!request.containsField(field)) {
			logger.info("ERROR:");
			logger.error("The " + field + "  field is not present, INVALID REQUEST");
			logger.error("Invalid request, does not contain Host field.");
			try {
				Assert.fail("Invalid request, does not contain " + field + " field");
			} catch (AssertionFailedError e) {
				logger.error("The field is not present", e);
			}
			
			return false;
		} else {
			return true;
		}
	}

	/**
	 * In this method the client code will process the statistic.
	 * @param CMD the command code of the statistic.
	 * @param statObj the bson stat object
	 * @param statSocket the socket by which we receive the message
	 */
	abstract public void processStat(Integer CMD, BSONObject statObj, Socket statSocket);

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		logger.info("Closing socket at the KPI subscriber side");
		gcmConnection.active = false;
		this.active = false;
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		zkClient.close();
	}
	
	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "KPI subscriber";
	}

}
