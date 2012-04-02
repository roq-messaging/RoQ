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

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.RoQPublisherConnection;
import org.roqmessaging.core.RoQSubscriberConnection;
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
	
	private Logger logger = Logger.getLogger(RoQConnectionFactory.class);
	
	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param configManager the config manager IP address, the default port 5000 will be applied
	 */
	public RoQConnectionFactory(String configManager) {
		this.configServer= configManager;
		
	}

	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQConnectionFactory#createRoQConnection()
	 */
	public IRoQConnection createRoQConnection(String qName) throws IllegalStateException {
		String monitorHost = translateToMonitorHost(qName);
		if(monitorHost.isEmpty()){
			//TODO do we create a new queue ?
			throw  new IllegalStateException("The queue Name is not registred @ the global configuration");
		}
		logger.info("Creating a connection factory for "+qName+ " @ "+ monitorHost);
		return new RoQPublisherConnection(monitorHost.split(",")[0]);
	}
	
	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQConnectionFactory#createRoQSubscriberConnection(java.lang.String)
	 */
	public IRoQSubscriberConnection createRoQSubscriberConnection(String qName, String key) throws IllegalStateException {
		String monitorConfig = translateToMonitorHost(qName);
		if(monitorConfig.isEmpty()){
			//TODO do we create a new queue ?
			throw  new IllegalStateException("The queue Name is not registred @ the global configuration");
		}
		logger.info("Creating a subscriber connection factory for "+qName+ " @ "+ monitorConfig);
		String[] config = monitorConfig.split(",");
		return new RoQSubscriberConnection(config[0],config[1], 0, key);
	}
	
	/**
	 * @param qName the logical queue name
	 * @return the monitor host address to contact +"," + the stat monitor address
	 */
	public String translateToMonitorHost (String qName){
		initSocketConnection();
		logger.debug("Asking the the global configuration Manager to translate the qName in a monitor host ...");
		//1.  Get the location of the monitor according to the logical name
		//We get the location of the corresponding host manager
		globalConfigReq.send((Integer.toString(RoQConstant.CONFIG_GET_HOST_BY_QNAME)+","+qName).getBytes(), 0);
		// The configuration should load the information about the monitor corresponding to this queue
		byte[] monitor = globalConfigReq.recv(0);
		String monitorHost = new String(monitor);
		logger.info("Creating a connection factory for "+qName+ " @ "+ monitorHost);
		closeSocketConnection();
		return monitorHost;
	}

	/**
	 * Removes the socket connection to the global config manager
	 */
	private void closeSocketConnection() {
		this.globalConfigReq.close();
		
	}

	/**
	 * Initialise the socket connection.
	 */
	private void initSocketConnection() {
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://"+this.configServer+":5000");
		
	}

}
