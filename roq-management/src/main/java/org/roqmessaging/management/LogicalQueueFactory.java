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
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class LogicalQueueFactory
 * <p> Description: Responsible of the logical queue Life cycle management. Binds config server on port
 *  5000 and config server on 5100
 * 
 * @author sskhiri
 */
public class LogicalQueueFactory implements IRoQLogicalQueueFactory {
	//ZMQ elements
	private ZMQ.Socket globalConfigReq=null;
	private ZMQ.Context context;
	//Logger
	private Logger logger = Logger.getLogger(LogicalQueueFactory.class);
	//Config
	private String configServer = null;
	private String factoryID = null;
	private boolean initialized = false;
	
	//Config to hold
	//[Host manager address, socket>
	private HashMap<String, ZMQ.Socket>hostManagerMap	;
	//QName, monitor location
	private HashMap<String, String> queueMonitorMap=null;

	/**
	 * Initialise the socket to the config server.
	 */
	public LogicalQueueFactory(String configServer) {
		this.configServer= configServer;
		this.factoryID =String.valueOf(System.currentTimeMillis()) +"_queuefactory";
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://"+this.configServer+":5000");
		this.hostManagerMap = new HashMap<String, ZMQ.Socket>();
	}

	/**
	 * 1. Check if the name already exist in the topology <br>
	 * 2. Create the entry in the global config <br>
	 * 3.  Sends the create event to the hostConfig manager thread
	 * 4. If the answer is not confirmed, we remove back the entry in the global config and throw an exception
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#createQueue(java.lang.String, java.lang.String)
	 */
	public void createQueue(String queueName, String targetAddress) throws IllegalStateException {
		if(!this.initialized) this.refreshTopology();
		//1. Check if the name already exist in the topology
		if(this.queueMonitorMap.containsKey(queueName)){
			// the queue already exist
			throw new IllegalStateException("The queue name "+ queueName+" already exists");
		}
		if(!this.hostManagerMap.containsKey(targetAddress)){
			// the target address is not registered as node of the cluster (no Host manager running)
			throw new IllegalStateException("the target address "+ targetAddress+" is not registered as a " +
								"node of the cluster (no Host manager running)");
		}
		//The name does not exist yet
		//2. Create the entry in the global config
		globalConfigReq.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + queueName+","+targetAddress).getBytes(), 0);
		String result= new String(globalConfigReq.recv(0));
		if(Integer.parseInt(result) != RoQConstant.CONFIG_CREATE_QUEUE_OK){
			throw new IllegalStateException("The queue creation for  "+ queueName+" failed on the global configuration server");
		}
		//3. Sends the create event to the hostConfig manager thread
		ZMQ.Socket hostSocket = this.hostManagerMap.get(targetAddress);
		hostSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + queueName).getBytes(), 0);
		String resultHost= new String(hostSocket.recv(0));
		if(Integer.parseInt(resultHost) != RoQConstant.CONFIG_CREATE_QUEUE_OK){
			//4. Roll back
			//If the answer is not confirmed, we should remove back
			//the entry in the global config and throw an exception
			removeQFromGlobalConfig(queueName);
			throw new IllegalStateException("The queue creation for  "+ queueName+" failed on the local host server");
		}else{
			logger.info("Created queue "+ queueName +" on local host "+targetAddress);
		}
	}

	/**
	 * Remove the configuration from the configuration:
	 * <br>
	 * 1. Remove from local cache - clean socket<br>
	 * 2. Remove the configuration from global configuration
	 * @param queueName the name of the queue to remove.
	 * @return the monitor host to send the remove request
	 */
	private String removeQFromGlobalConfig(String queueName) {
		//1. Clean local cache
		String monitorHost = this.queueMonitorMap.remove(queueName);
		//2. Ask the global configuration to remove the queue
		globalConfigReq.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + queueName).getBytes(), 0);
		String result= new String(globalConfigReq.recv(0));
		if(Integer.parseInt(result) != RoQConstant.CONFIG_CREATE_QUEUE_OK){
			logger.error("Error when removing the queue "+queueName + " from global config", 
					new IllegalStateException("The queue creation for  "+ queueName+" failed on the global configuration server"));
		}
		return monitorHost;
	}

	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#removeQueue(java.lang.String)
	 */
	public boolean removeQueue(String queueName) {
		//1. Get the monitor address &  Remove the entry in the global configuration
		String host = removeQFromGlobalConfig(queueName);
		//2. Send the remove message to the monitor
		//TODO Send the remove message to the monitor
		return false;
	}

	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#refreshTopology()
	 */
	public void refreshTopology() {
		// Load the topology config
		globalConfigReq.send((Integer.toString(RoQConstant.INIT_REQ) + "," + this.factoryID).getBytes(), 0);
		// The configuration should load all information about the Local host
		// managers = system topology
		logger.debug("Sending topology global config request...");
		byte[] configuration = globalConfigReq.recv(0);
		List<String> hostManagers = RoQUtils.getInstance().deserializeObject(configuration);
		if (globalConfigReq.hasReceiveMore()) {
			//The logical queue config is sent in the part 2
			byte[] qConfiguration = globalConfigReq.recv(0);
			queueMonitorMap = RoQUtils.getInstance().deserializeObject(qConfiguration);
			}
		logger.info("Getting configuration with "+ hostManagers.size() +" Host managers and "+ queueMonitorMap.size()+" queues");
		this.initialized = true;
		updateLocalConfiguration(hostManagers);
	}

	/**
	 * Update the local configuration according to the refreshed list of host managers.
	 * According to the delta it removes or adds new sockets.
	 * <p>
	 * This method connects directly all the host managers. If really to many host 
	 * we can imagine to connect the host only when we need, a kind a lazy loading, but it 
	 * is not yet implemented.
	 * 
	 * @param hostManagers the new list of host manager
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
			logger.debug("Removing to "+ hostToRemove);
			ZMQ.Socket socket = this.hostManagerMap.remove(hostToRemove);
			socket.close();
		}
		// 2. To add
		for (String hostAddress : hostManagers) {
			if (!this.hostManagerMap.containsKey(hostManagers)) {
				// Must create one
				toAdd.add(hostAddress);
			}
		}
		for (String hostToadd : toAdd) {
			try {
			ZMQ.Socket socket = context.socket(ZMQ.REQ);
			String address= "tcp://"+hostToadd+":5100";
			logger.debug("Connect to "+ address);
			socket.connect(address);
			this.hostManagerMap.put(hostToadd, socket);
			logger.debug("Added host "+ hostToadd +" in the local configuration");
			} catch (Exception e) {
				logger.error("Error when connecting to host "+ hostToadd, e);
			}
		}
	}

	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#clean()
	 */
	public void clean() {
		logger.info("Cleaning the local cache");
		for(String host: this.hostManagerMap.keySet()){
			ZMQ.Socket socket = this.hostManagerMap.get(host);
			socket.close();
		}
		this.hostManagerMap.clear();
		this.queueMonitorMap.clear();
	}
}
