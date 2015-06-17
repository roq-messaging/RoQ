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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.zeromq.ZMQ;

/**
 * Class GlobalConfigurationStateClient
 * <p> Description: Define the state of the global configuration. Then it provides an encapsulation mechanism for
 *  all objects that want to get the global configuration. This cntralized the information and the access to the information.
 * 
 * @author sskhiri
 */
public class GlobalConfigurationStateClient extends GlobalConfigurationState {
	// ZMQ elements
	private ZMQ.Socket globalConfigReq = null;
	private ZMQ.Context context;
	//RoQ element ID
	private String clientID = null;
	//The global configuration server IP address
	protected String configServer = null;
	protected int hcmTIMEOUT = 3000;
	private Logger logger = Logger.getLogger(GlobalConfigurationStateClient.class);
	
	/**
	 * Initialize the ZMQ configuration. 
	 * @param configurationServer
	 */
	public GlobalConfigurationStateClient(String configurationServer) {
		super();
		this.configServer = configurationServer;
		context = ZMQ.context(1);
		this.hcmTIMEOUT = 2000;
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://" + configurationServer + ":5000");
		this.clientID =String.valueOf(System.currentTimeMillis()) + "globalconfigclientState";
	}
	
	/**
	 * Initialize the ZMQ configuration. 
	 * @param configurationServer
	 */
	public GlobalConfigurationStateClient(String configurationServer, int hcmTIMEOUT) {
		super();
		this.configServer = configurationServer;
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		this.hcmTIMEOUT = hcmTIMEOUT;
		globalConfigReq.connect("tcp://" + configurationServer + ":5000");
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
		updateLocalConfiguration(hostManagers);
	}
	
	/**
	 * Update the local configuration according to the refreshed list of host
	 * managers. According to the delta it removes or adds new sockets.
	 * <p>
	 * This method connects directly all the host managers. If really to many
	 * host we can imagine to connect the host only when we need, a kind a lazy
	 * loading, but it is not yet implemented.
	 * 
	 * @param hostManagers
	 *            the new list of host manager
	 */
	private void updateLocalConfiguration(List<String> hostManagers) {
		// Create the socket for all host manager that are new or remove the old
		// ones
		logger.debug("Updating new local configuration");
		List<String> toRemove = new ArrayList<String>();
		List<String> toAdd = new ArrayList<String>();
		// 1. Remove the old one
		for (String existingHost : hostManagerMap.keySet()) {
			if (!hostManagers.contains(existingHost)) {
				// must be removed
				toRemove.add(existingHost);
			}
		}
		for (String hostToRemove : toRemove) {
			logger.debug("Removing to " + hostToRemove);
			ZMQ.Socket socket = this.hostManagerMap.remove(hostToRemove);
			socket.setLinger(0);
			socket.close();
		}
		// 2. To add
		for (String hostAddress : hostManagers) {
			if (!this.hostManagerMap.containsKey(hostAddress)) {
				// Must create one
				toAdd.add(hostAddress);
			}
		}
		for (String hostToadd : toAdd) {
			try {
				ZMQ.Socket socket = context.socket(ZMQ.REQ);
				String address = "tcp://" + hostToadd + ":5100";
				logger.debug("Connect to " + address);
				socket.setReceiveTimeOut(hcmTIMEOUT);
				socket.setSendTimeOut(hcmTIMEOUT);
				socket.connect(address);
				this.hostManagerMap.put(hostToadd, socket);
				this.getHostManagerAddresses().add(hostToadd);
				logger.debug("Added host " + hostToadd + " in the local configuration");
			} catch (Exception e) {
				logger.error("Error when connecting to host " + hostToadd, e);
			}
		}
	}
}
