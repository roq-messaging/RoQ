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
import java.util.List;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.zeromq.ZMQ;

/**
 * Class GlobalConfigurationState
 * <p> Description: Define the state of the global configuration. Then it provides an encapsulation mechanism for
 *  all objects that want to get the global configuration. This cntralized the information and the access to the information.
 * 
 * @author sskhiri
 */
public class GlobalConfigurationState {
	// [Host manager address, socket>
	private HashMap<String, ZMQ.Socket> hostManagerMap;
	// QName, monitor location
	private HashMap<String, String> queueMonitorMap = null;
	//QName, target host location
	private HashMap<String, String> queueHostLocation = null;
	//QName, stat monitor address server (ready to connect!)
	private HashMap<String, String> queueMonitorStatMap = null;
	
	// ZMQ elements
	private ZMQ.Socket globalConfigReq = null;
	private ZMQ.Context context;
	// Logger
	private Logger logger = Logger.getLogger(GlobalConfigurationState.class);
	
	//The global configuration server IP address
	private String configServer = null;
	//utils
	private RoQSerializationUtils serializationUtils=null;
	//RoQ element ID
	private String clientID = null;

	/**
	 * @param configurationServer the global configuration server address
	 */
	public GlobalConfigurationState(String configurationServer) {
		this.configServer = configurationServer;
		this.serializationUtils = new RoQSerializationUtils();
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://" + this.configServer + ":5000");
		this.clientID =String.valueOf(System.currentTimeMillis()) + "globalconfigclientState";
	}
	
	
	/**
	 * Reload cache from the global configuration server
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#refreshTopology()
	 */
	public void refreshConfiguration() {
		// Load the topology config
		globalConfigReq.send((Integer.toString(RoQConstant.INIT_REQ) + "," + this.clientID).getBytes(), 0);
		// The configuration should load all information about the Local host
		// managers = system topology
		logger.debug("Sending global state global config request...");
		byte[] configuration = globalConfigReq.recv(0);
		List<String> hostManagers = this.serializationUtils.deserializeObject(configuration);
		if (globalConfigReq.hasReceiveMore()) {
			// The logical queue config is sent in the part 2
			byte[] qConfiguration = globalConfigReq.recv(0);
			queueMonitorMap = this.serializationUtils.deserializeObject(qConfiguration);
		}
		if (globalConfigReq.hasReceiveMore()) {
			// The host location distribution is sent in the part 3
			byte[] qConfiguration = globalConfigReq.recv(0);
			queueHostLocation =this.serializationUtils.deserializeObject(qConfiguration);
		}
		if (globalConfigReq.hasReceiveMore()) {
			// The stat monitor for each logical queue is sent in the part 4
			byte[] qConfiguration = globalConfigReq.recv(0);
			queueMonitorStatMap = this.serializationUtils.deserializeObject(qConfiguration);
		}
		logger.info("Getting configuration with " + hostManagers.size() + " Host managers and "
				+ queueMonitorMap.size() + " queues");
	}
	
	
	

	/**
	 * @return the hostManagerMap
	 */
	public HashMap<String, ZMQ.Socket> getHostManagerMap() {
		return hostManagerMap;
	}

	/**
	 * @param hostManagerMap the hostManagerMap to set
	 */
	public void setHostManagerMap(HashMap<String, ZMQ.Socket> hostManagerMap) {
		this.hostManagerMap = hostManagerMap;
	}

	/**
	 * @return the queueMonitorMap
	 */
	public HashMap<String, String> getQueueMonitorMap() {
		return queueMonitorMap;
	}

	/**
	 * @param queueMonitorMap the queueMonitorMap to set
	 */
	public void setQueueMonitorMap(HashMap<String, String> queueMonitorMap) {
		this.queueMonitorMap = queueMonitorMap;
	}

	/**
	 * @return the queueHostLocation
	 */
	public HashMap<String, String> getQueueHostLocation() {
		return queueHostLocation;
	}

	/**
	 * @param queueHostLocation the queueHostLocation to set
	 */
	public void setQueueHostLocation(HashMap<String, String> queueHostLocation) {
		this.queueHostLocation = queueHostLocation;
	}

	/**
	 * @return the queueMonitorStatMap
	 */
	public HashMap<String, String> getQueueMonitorStatMap() {
		return queueMonitorStatMap;
	}

	/**
	 * @param queueMonitorStatMap the queueMonitorStatMap to set
	 */
	public void setQueueMonitorStatMap(HashMap<String, String> queueMonitorStatMap) {
		this.queueMonitorStatMap = queueMonitorStatMap;
	}
	
	
	
	
	
	
	

}
