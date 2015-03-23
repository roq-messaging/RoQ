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
package org.roqmessaging.core.factory;

import java.net.ConnectException;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.RoQPublisherConnection;
import org.roqmessaging.core.RoQSubscriberConnection;
import org.roqmessaging.zookeeper.RoQZKSimpleConfig;
import org.roqmessaging.zookeeper.RoQZooKeeper;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;


/**
 * Class RoQConnectionFactory
 * <p> Description: Create the connection object that will manage the connection between client and exchanges.
 * 
 * @author sskhiri
 */
public class RoQConnectionFactory implements IRoQConnectionFactory {
	//The global config server address 
	private String configServer = null;
	// ZMQ config
	private Context context= null;
	//The socket to the global config
	private Socket globalConfigReq;
	
	// TODO: Following params in a config file
	// Number of times that the client retry the request
	private int maxRetry = 6;
	// the rcv timeout of ZMQ
	private int timeout = 5000;
	private RoQZooKeeper zk;
	private Logger logger = Logger.getLogger(RoQConnectionFactory.class);
	
	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param configManager the zookeeper IP addresses, the default port 5000 will be applied
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQConnectionFactory(String zkAddresses) {
		zk = new RoQZooKeeper(zkAddresses);
	}

	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param cfg Zookeeper config
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQConnectionFactory(RoQZKSimpleConfig cfg) {
		zk = new RoQZooKeeper(cfg);
	}

	/**
	 * @throws ConnectException 
	 * @see org.roqmessaging.clientlib.factory.IRoQConnectionFactory#createRoQConnection()
	 */
	public IRoQConnection createRoQConnection(String qName) 
			throws IllegalStateException, ConnectException {
		String monitorHost = translateToMonitorHost(qName);
		if(monitorHost.isEmpty()){
			//TODO do we create a new queue ?
			throw  new IllegalStateException("The queue creation has failed @ the global configuration");
		}
		logger.info("Creating a connection factory for "+qName+ " @ "+ monitorHost);
		return new RoQPublisherConnection(monitorHost.split(",")[0]);
	}
	
	/**
	 * @throws ConnectException 
	 * @see org.roqmessaging.clientlib.factory.IRoQConnectionFactory#createRoQSubscriberConnection(java.lang.String)
	 */
	public IRoQSubscriberConnection createRoQSubscriberConnection(String qName, String key) 
			throws IllegalStateException, ConnectException {
		String monitorConfig = translateToMonitorHost(qName);
		if(monitorConfig.isEmpty()){
			//TODO do we create a new queue ?
			throw  new IllegalStateException("The queue Name is not registred @ the global configuration");
		}
		logger.info("Creating a subscriber connection factory for "+qName+ " @ "+ monitorConfig);
		String[] config = monitorConfig.split(",");
		return new RoQSubscriberConnection(config[0],config[1], key);
	}
	
	/**
	 * @param qName the logical queue name
	 * @return the monitor host address to contact +"," + the stat monitor address
	 * @throws ConnectException 
	 */
	public String translateToMonitorHost (String qName) 
			throws ConnectException {
		logger.debug("Asking the the global configuration Manager to translate the qName in a monitor host ...");
		//1.  Get the location of the monitor according to the logical name
		//We get the location of the corresponding host manager
		byte[] request = (Integer.toString(RoQConstant.CONFIG_GET_HOST_BY_QNAME)+","+qName).getBytes();
		byte[] response = sendRequest(request);
		String monitorHost = new String(response);
		logger.info("Creating a connection factory for "+qName+ " @ "+ monitorHost);
		return monitorHost;
	}
	
	/**
	 * Removes the socket connection to the global config manager
	 */
	private void closeSocketConnection() {
		this.logger.debug("Closing factory socket");
		this.globalConfigReq.setLinger(0);
		this.globalConfigReq.close();
	}

	/**
	 * Initialise the socket connection.
	 */
	private void initSocketConnection() 
			throws IllegalStateException {
		// Get the active master address
		this.configServer = zk.getGCMLeaderAddress();
		if (configServer == null) 
			throw new IllegalStateException("GCM node not found");
		logger.info("Active GCM address: " + this.configServer);
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://"+this.configServer+":5000");
		globalConfigReq.setReceiveTimeOut(timeout);
	}

	/**
	 * This method sends a request to the GCM.
	 * First it get the active GCM, then it sent the request
	 * If the request fails because leader has changed or the connection
	 * cannot be established (timeout), the request is resent up to maxRetry times.
	 * @param request
	 * @return response
	 * @throws ConnectException 
	 */
	private byte[]  sendRequest (byte[] request) throws ConnectException {
		// The configuration should load the information about the monitor corresponding to this queue
		byte[] response = null;
		String responseString = "";
		int retry = 0;
		// If the request has failed, we retry until to reach the maxRetry
		do {
			try {
				if (retry > 0) {
					logger.info("GCM not found");
					Thread.sleep(1500); // Wait between two requests
				}
				initSocketConnection();
				globalConfigReq.send(request, 0);
				response = globalConfigReq.recv(0);
				if (response != null) {
					logger.info("GCM found");
					responseString = new String(response);
				}
				closeSocketConnection();
			} catch (Exception e1) {
				//e1.printStackTrace();
			} finally {
				retry++;
			}
		} while ((response == null || responseString.equals(Integer.toString(RoQConstant.EVENT_LEAD_LOST))) 
				&& retry < maxRetry);
		if (response == null || responseString.equals(Integer.toString(RoQConstant.EVENT_LEAD_LOST)))
			throw new ConnectException("Failed to process the request @ GCM");
		return response;
	}

}
