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
	
	/**
	 * Constructor
	 */
	public HostConfigManager(){
		// ZMQ Init
		this.context = ZMQ.context(1);
		this.clientReqSocket = context.socket(ZMQ.REP);
		this.clientReqSocket.bind("tcp://*:5100");
		this.running = true;
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
				
				//Receive a create queue request on the local host manager
				case RoQConstant.CONFIG_CREATE_QUEUE:
					logger.debug("Recieveing create Q request from a client ");
					if (info.length == 2) {
						String qName = info[1];
						logger.debug("The request format is valid with 2 parts, Q to create:  "+qName);
						//TODO Launch a monitor
						//TODO Launch an exchange if possible not in the same JVM
						//if OK send OK
						this.clientReqSocket.send(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_OK).getBytes(), 0);
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
	 * 
	 */
	public void shutDown() {
		this.running=false;
	}

}
