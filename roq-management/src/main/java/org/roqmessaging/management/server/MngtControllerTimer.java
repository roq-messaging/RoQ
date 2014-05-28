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

import java.util.ArrayList;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.server.state.QueueManagementState;
import org.zeromq.ZMQ;

/**
 * Class MngtControllerTimer
 * <p> Description: Attached to the {@linkplain MngtController}, this element is responsible for
 *  publishing periodically the configuration to any 3rd party management console in non-java
 *  dependent protocol.
 * 
 * @author sskhiri
 */
public class MngtControllerTimer extends TimerTask {
	//The reference to the magement controller
	private MngtController controller = null;
	//ZMQ init
	private ZMQ.Socket mngtPubSocket = null;
	private ZMQ.Context context;
	//The Serialiazer
	private IRoQSerializer serializer = null;
	//logger
	private Logger logger = Logger.getLogger(MngtControllerTimer.class);
	
	/**
	 * Init variable and open a pub socket on the given port
	 * @param period the publication period
	 * @param controller the handle to the controller
	 * @param port the port to publish on
	 */
	public MngtControllerTimer(int period, MngtController controller, int port) {
		super();
		//init variable
		this.controller = controller;
		this.serializer = new RoQBSONSerializer();
		//ZMQ init
		this.context = ZMQ.context(1);
		this.mngtPubSocket = context.socket(ZMQ.PUB);
		this.mngtPubSocket.bind("tcp://*:"+port);
	}

	/**
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		logger.debug("Sending stored topology");
		try {
			// 1. Get the configuration
			ArrayList<QueueManagementState> queues =  this.controller.getStorage().getQueues();
			ArrayList<String> hosts = this.controller.getStorage().getHosts();
			
			// 2. Serialization &  Publish the configuration
			this.mngtPubSocket.send(this.serializer.serialiseCMDID(RoQConstant.MNGT_UPDATE_CONFIG), ZMQ.SNDMORE);
			this.mngtPubSocket.send(this.serializer.serialiseQueues(queues), ZMQ.SNDMORE);
			this.mngtPubSocket.send(this.serializer.serialiseHosts(hosts), 0);
			
		} catch (Exception e) {
			logger.error("Error while sending MNGT config to management console", e);
		}
	}
	
	/**
	 * @see java.util.TimerTask#cancel()
	 */
	@Override
	public boolean cancel() {
		logger.info("Stopping the Management controller publisher");
		try {
			this.mngtPubSocket.close();
		} catch (Exception e) {
			logger.error("Error when closing socket", e);
		}
		return super.cancel();
	}
}
