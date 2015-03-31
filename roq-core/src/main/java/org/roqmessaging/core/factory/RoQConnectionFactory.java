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
import org.roqmessaging.core.RoQGCMConnection;
import org.roqmessaging.core.RoQPublisherConnection;
import org.roqmessaging.core.RoQSubscriberConnection;
import org.roqmessaging.zookeeper.RoQZKSimpleConfig;
import org.roqmessaging.zookeeper.RoQZooKeeper;

/**
 * Class RoQConnectionFactory
 * <p> Description: Create the connection object that will manage the connection between client and exchanges.
 * 
 * @author sskhiri
 */
public class RoQConnectionFactory implements IRoQConnectionFactory {	
	private RoQZooKeeper zk;
	private Logger logger = Logger.getLogger(RoQConnectionFactory.class);
	private RoQGCMConnection gcmConnection;
	
	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param configManager the zookeeper IP addresses, the default port 5000 will be applied
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQConnectionFactory(String zkAddresses, int maxRetry, int timeout) {
		zk = new RoQZooKeeper(zkAddresses);
		zk.start();
		gcmConnection = new RoQGCMConnection(zk, maxRetry, timeout);
	}
	
	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param configManager the zookeeper IP addresses, the default port 5000 will be applied
	 */
	public RoQConnectionFactory(String zkAddresses) {
		zk = new RoQZooKeeper(zkAddresses);
		zk.start();
		gcmConnection = new RoQGCMConnection(zk, 8, 3000);
	}

	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param cfg Zookeeper config
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQConnectionFactory(RoQZKSimpleConfig cfg) {
		zk = new RoQZooKeeper(cfg);
		zk.start();
		gcmConnection = new RoQGCMConnection(zk, 8, 3000);
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
		byte[] response = gcmConnection.sendRequest(request, 5000);
		String monitorHost = new String(response);
		logger.info("Creating a connection factory for "+qName+ " @ "+ monitorHost);
		return monitorHost;
	}
	
	public void close() {
		zk.close();
	}

}
