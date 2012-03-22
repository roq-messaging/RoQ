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

import org.apache.log4j.Logger;
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class LogicalQueueFactory
 * <p> Description: Responsible of the logical queue Life cycle management.
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
	private ArrayList<String> hostManagers = null;
	//Name, monitor location
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
		//The name does not exist yet
		//2. Create the entry in the global config
		globalConfigReq.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + queueName+","+targetAddress).getBytes(), 0);
		String result= new String(globalConfigReq.recv(0));
		if(Integer.parseInt(result) != RoQConstant.CONFIG_CREATE_QUEUE_OK){
			throw new IllegalStateException("The queue creation for  "+ queueName+" failed on the global configuration server");
		}
		//3. Sends the create event to the hostConfig manager thread
		//TODO  Sends the create event to the hostConfig manager thread
		//4. If the answer is not confirmed, we should remove back the entry in the global config and throw an exception

	}

	/* (non-Javadoc)
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#removeQueue(java.lang.String)
	 */
	public boolean removeQueue(String queueName) {
		//1. Get the monitor address
		//2. Remove the entry in the global configuration
		//3. Send the remove message to the monitor
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
		hostManagers = RoQUtils.getInstance().deserializeObject(configuration);
		if (globalConfigReq.hasReceiveMore()) {
			//The logical queue config is sent int the part 2
			byte[] qConfiguration = globalConfigReq.recv(0);
			queueMonitorMap = RoQUtils.getInstance().deserializeObject(qConfiguration);
			}
		logger.info("Getting configuration with "+ hostManagers.size() +" Host managers and "+ queueMonitorMap.size()+" queues");
		this.initialized = true;
	}
}
