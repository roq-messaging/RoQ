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
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class GlobalConfigurationManager
 * <p> Description: responsible for handling the global configuration. This class must run 
 * within a thread. In the future this class will share its data through a data grid.
 * 
 * @author sskhiri
 */
public class GlobalConfigurationManager implements Runnable {
	private volatile boolean running;
	//ZMQ config
	private ZMQ.Socket clientReqSocket = null;
	private ZMQ.Context context;
	
	//Configuration data: list of host manager (1 per RoQ Host)
	private ArrayList<String> hostManagerAddresses = null;
	//Define the location of the monitor of each queue (Name, monitor address)
	private HashMap<String, String> queueLocations=null;
	//Define the location of the monitor stat address for each queue (Name, monitor stat address)
	private HashMap<String, String> queueStatLocation = null;
	
	private Logger logger = Logger.getLogger(GlobalConfigurationManager.class);
	
	/**
	 * Constructor.
	 */
	public GlobalConfigurationManager() {
		this.hostManagerAddresses = new ArrayList<String>();
		this.logger.info("Started global config Runnable");
		this.queueLocations = new HashMap<String, String>();
		this.queueStatLocation = new HashMap<String, String>();
		this.context = ZMQ.context(1);
		this.clientReqSocket = context.socket(ZMQ.REP);
		this.clientReqSocket.bind("tcp://*:5000");
		this.running = true;
	}

	/**
	 * Main run
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		this.running = true;
		//ZMQ init
		ZMQ.Poller items = context.poller(3);
		items.register(this.clientReqSocket);
		//2. Start the main run of the monitor
		while (this.running) {
			items.poll(2000);
			if (items.pollin(0)){ //Comes from a client
				logger.debug("Receiving request...");
				String  info[] = new String(clientReqSocket.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);
				logger.debug("Start analysing info code = "+ infoCode);
				switch (infoCode) {
				
				//init request from client that want to receive a local cache of configuration
				case RoQConstant.INIT_REQ:
					// A client is asking fof the topology of all local host
					// manager
					logger.debug("Recieveing init request from a client ");
					byte[] serialised = RoQUtils.getInstance().serialiseObject(this.hostManagerAddresses);
					logger.debug("Sending back the topology - list of local host");
					this.clientReqSocket.send(serialised, ZMQ.SNDMORE);
					this.clientReqSocket.send(RoQUtils.getInstance().serialiseObject(this.queueLocations), 0);
					break;
					
				//A create queue request
				case RoQConstant.CONFIG_CREATE_QUEUE:
					logger.debug("Recieveing create Q request from a client ");
					if (info.length == 4) {
						logger.debug("The request format is valid we 3 part:  "+ info[1] +" "+ info[2]+ " "+ info[3]);
						// The logical queue config is sent int the part 2
						String qName = info[1];
						String monitorHost = info[2];
						String statMonitorHost = info[3];
						// register the queue
						addQueueName(qName, monitorHost);
						addQueueStatMonitor(qName, statMonitorHost);
						this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_OK).getBytes(), 0);
					}else{
							logger.error("The create queue request sent does not contain 3 part: ID, quName, Monitor host");
							this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL).getBytes(), 0);
						}
					break;
					
					//A add host request
				case RoQConstant.CONFIG_ADD_HOST:
					logger.debug("Recieveing ADD HOST request from a client ");
					if (info.length == 2) {
						logger.debug("The request format is valid ");
						// The logical queue config is sent int the part 2
						addHostManager(info[1]);
					}
					break;
					
					//A remove host request
				case RoQConstant.CONFIG_REMOVE_HOST:
					logger.debug("Recieveing REMOVE HOST request from a client ");
					if (info.length == 2) {
						logger.debug("The request format is valid ");
						// The logical queue config is sent int the part 2
						removeHostManager(info[1]);
					}
					break;
					
					//A remove host request
				case RoQConstant.CONFIG_GET_HOST_BY_QNAME:
					logger.debug("Recieveing GET HOST request from a client ");
					if (info.length == 2) {
						logger.debug("The request format is valid - Asking for translating  "+ info[1]);
						if(this.queueLocations.containsKey(info[1])){
							logger.debug("Answering back:"+ this.queueLocations.get(info[1])+","+this.queueStatLocation.get(info[1]));
							this.clientReqSocket.send((this.queueLocations.get(info[1])+","+this.queueStatLocation.get(info[1])).getBytes(), 0);
						}else{
							logger.warn(" No logical queue as:"+info[1]);
							this.clientReqSocket.send(("").getBytes(), 0);
						}
					}
					break;
				}
				
			}
		}
		this.clientReqSocket.close();
	}
	
	
	/**
	 * @param qName the name of the logical queue
	 * @param statMonitorHost the stat port address of the corresponding monitor
	 */
	public void addQueueStatMonitor(String qName, String statMonitorHost) {
		this.queueStatLocation.put(qName, statMonitorHost);
		logger.debug("Adding stat monitor address for "+ qName +" @"+ statMonitorHost +" in global configuration");
		
	}


	/**
	 * @param hosr the ip address of the host to remove.
	 */
	public void removeHostManager(String host) {
		if(this.hostManagerAddresses.remove(host)) logger.info("Removed host successfully "+ host);
		else  logger.error("Removed host failed on the hashmap "+ host);
	}


	/**
	 * Stop the active thread
	 */
	public void  shutDown(){
		this.running = false;
		this.logger.info("Shutting down config server");
	}
	
	/**
	 * Add a host manager address to the array.
	 * @param host the host to add (ip address), the port is defined by default as there is only 1 Host 
	 * manager per host machine.
	 */
	public void addHostManager(String host){
		this.logger.info("Adding new host manager reference : "+ host);
		if (!hostManagerAddresses.contains(host)){
			hostManagerAddresses.add(host);
		}
	}
	
	/**
	 * @param qName the logical queue name
	 * @param host as tcp://ip:port, tcp://ip:statport
	 */
	public void addQueueName(String qName, String host){
		this.queueLocations.put(qName, host);
		logger.debug("Created queue "+ qName +" @"+ host +" in global configuration");
	}

}
