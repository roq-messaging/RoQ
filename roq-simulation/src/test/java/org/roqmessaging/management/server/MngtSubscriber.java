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
package org.roqmessaging.management.server;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class MngtSubscriber
 * <p> Description: Simulate a client process that listens for the managment controller.
 * 
 * @author sskhiri
 */
public class MngtSubscriber implements Runnable, IStoppable {
	//ZMQ configuration
	private ZMQ.Context context = null;
	//KPI socket
	private ZMQ.Socket mngtSubSocket = null;
	//Define whether the thread must continue to run
	private volatile boolean active = true;
	//the looger
	private Logger logger = Logger.getLogger(MngtSubscriber.class);
	
	/**
	 * 
	 */
	public MngtSubscriber() {
		// 1. ZMQ Init
		this.context = ZMQ.context(1);
		// 2. Register a socket to the stat monitor
		mngtSubSocket = context.socket(ZMQ.SUB);
		mngtSubSocket.connect("tcp://"+RoQUtils.getInstance().getLocalIP()+":5004");
		mngtSubSocket.subscribe("".getBytes());
		logger.debug(" connected to tcp://"+RoQUtils.getInstance().getLocalIP()+":5004");

	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		ZMQ.Poller poller = new ZMQ.Poller(1);
		poller.register(mngtSubSocket);
		
		while (active){
			poller.poll(150);
			if(poller.pollin(0)){
				//Stat coming from the KPI stream
				BSONObject statObj = BSON.decode(mngtSubSocket.recv(0));
				logger.debug(statObj.toString());
				Assert.assertEquals(true, statObj.containsField("CMD"));
				if(mngtSubSocket.hasReceiveMore()){
					statObj = BSON.decode(mngtSubSocket.recv(0));
					logger.debug(statObj.toString());
					Assert.assertEquals(true, statObj.containsField("Queues"));
				}
				if(mngtSubSocket.hasReceiveMore()){
					statObj = BSON.decode(mngtSubSocket.recv(0));
					logger.debug(statObj.toString());
					Assert.assertEquals(true, statObj.containsField("Hosts"));
				}
			}
		}
		this.mngtSubSocket.close();
	}

	/* (non-Javadoc)
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		this.active = false;
		
	}

	/* (non-Javadoc)
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return this.getClass().getName();
	}

}
