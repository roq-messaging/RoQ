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
import org.zeromq.ZMQ;


/**
 * Class HostConfigManager
 * <p> Description: Responsible for the local management of the queue elements. For each host it will track the list 
 * of monitors and exchanges.
 * Notice that the host config manager implement a server pattern that exposes services to the monitor management.
 * 
 * @author sskhiri
 */
public class HostConfigManager implements Runnable {
	
	//ZMQ config
	private ZMQ.Socket clientReqSocket = null;
	private ZMQ.Context context;
	//Logger
	private Logger logger = Logger.getLogger(HostConfigManager.class);
	//Host manager config
	private volatile boolean running = false;
	//the base port for front port
	private int baseMonitortPort = 5500;
	private int baseStatPort = 5800;
	
	private int baseFrontPort = 5500;
	private int baseBackPort = 5800;
	//Local configuration maintained by the host manager
	// [qName, the monitor]
	private HashMap<String, String> qMonitorMap = null;
	//[qName, list of Xchanges]
	private HashMap<String, List<String>> qExchangeMap = null;
	
	
	/**
	 * Constructor
	 */
	public HostConfigManager(){
		// ZMQ Init
		this.context = ZMQ.context(1);
		this.clientReqSocket = context.socket(ZMQ.REP);
		this.clientReqSocket.bind("tcp://*:5100");
		this.qExchangeMap = new HashMap<String, List<String>>();
		this.qMonitorMap = new HashMap<String, String>();
	}
	
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		this.running = true;
		//ZMQ init
		ZMQ.Poller items = context.poller(1);
		items.register(this.clientReqSocket);
		
		//2. Start the main run of the monitor
		while (this.running) {
			items.poll(2000);
			if (items.pollin(0)){ //Comes from a client
				logger.debug("Receiving Incoming request @host...");
				String[]  info = new String(clientReqSocket.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);
				logger.debug("Start analysing info code = "+ infoCode);
				switch (infoCode) {
				
				//Receive a create queue request on the local host manager (likely from the LogicalQFactory
				case RoQConstant.CONFIG_CREATE_QUEUE:
					logger.debug("Recieveing create Q request from a client ");
					if (info.length == 2) {
						String qName = info[1];
						logger.debug("The request format is valid with 2 parts, Q to create:  "+qName);
						//TODO Launch a monitor
						boolean monitorOK = startNewMonitorProcess(qName);
						//TODO Launch an exchange if possible not in the same JVM
						boolean xChangeOK = startNewExchangeProcess(qName);
						//if OK send OK
						if(monitorOK& xChangeOK) this.clientReqSocket.send(
								Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_OK).getBytes(), 0);
						else
							logger.error("The create queue request has failed at the monitor host,check logs");
						this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL).getBytes(), 0);
					}else{
							logger.error("The create queue request sent does not contain 3 part: ID, quName, Monitor host");
							this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL).getBytes(), 0);
						}
					break;
				}
			}
		}
		this.clientReqSocket.close();
	}

	/**
	 * Start a new exchange process
	 * <p>
	 * 1. Check the number of local xChange present in the host
	 * 2. Start a new xChange with port config + nchange
	 * @param qName the name of the queue to create
	 * @return true if the creation process worked well
	 */
	private boolean startNewExchangeProcess(String qName) {
		//1. Get the number of installed queues on this host
		int number = 0;
		for (String q_i : this.qExchangeMap.keySet()) {
			List<String> xChanges = this.qExchangeMap.get(q_i);
			number+= xChanges.size();
		}
		logger.debug(" This host contains already )"+ number +" Exchanges");
		int frontPort = this.baseFrontPort+number;
		int backPort = this.baseBackPort+number;
		String argument =frontPort+" "+ backPort +" tcp://localhost:"+(this.baseMonitortPort+this.qMonitorMap.size()) +
				" tcp://localhost:"+ (this.baseStatPort+this.qMonitorMap.size());
		logger.info(" Starting Xchange script with "+ argument);
		//2. Launch script
		
		return true;
	}


	/**
	 * Start a new Monitor process
	 * <p>
	 * 1. Check the number of local monitor present in the host
	 * 2. Start a new monitor with port config + nMonitor
	 * @param qName the name of the queue to create
	 * @return true if the creation process worked well
	 */
	private boolean startNewMonitorProcess(String qName) {
		//1. Get the number of installed queues on this host
		int number = this.qMonitorMap.size();
		int frontPort = this.baseMonitortPort+number;
		int statPort = this.baseStatPort+number;
		logger.debug(" This host contains already )"+ number +" Monitor");
		String argument =  frontPort +" " + statPort;
		logger.debug("Starting monitor process by script launch on "+argument);
		this.qMonitorMap.put(qName, ("tcp://localhost:"+frontPort));
		//2. Launch script
//		ProcessBuilder pb = new ProcessBuilder(" ./src/main/resources/startMonitor.sh ", argument);
//		 pb.directory(new File("myDir"));
//		 try {
//			Process p = pb.start();
//		} catch (IOException e) {
//			logger.error("Error while executing script", e);
//		}

		return true;
	}

	/**
	 * 
	 */
	public void shutDown() {
		this.running=false;
	}

}
