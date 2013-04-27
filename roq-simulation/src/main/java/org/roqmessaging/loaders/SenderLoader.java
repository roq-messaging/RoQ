/**
 * Copyright 2013 EURANOVA
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
package org.roqmessaging.loaders;

import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.interfaces.IStoppable;

/**
 * Class SenderLoader
 * <p> Description: The sender loader is responsible for sending messages at a specified rate.  
 * Every second it checks the rate and send messages till the number of sent message reach the rate. Once
 * it is done it just waits for the next wake-up call (next run).
 * 
 * @author sskhiri
 */
public class SenderLoader extends TimerTask implements IStoppable {
	//The load rate for this sender in [msg/s]
	private int rate =0;
	//Payload of message in [kb]
	private byte[] payload = null;
	//The RoQ publisher
	private IRoQPublisher publisher = null;
	//The connection to the queue, we need to keep an handle to close it.
	private IRoQConnection connection = null;
	//The address of the GCM
	private String configServerAddress = null;
	//The queue under test
	private String queueOnTest = "?";
	//The number of messages sent on this timer cycle
	private int sentMsg = 0;
	//The logger 
	private Logger logger = Logger.getLogger(SenderLoader.class.getCanonicalName());
	
	/**
	 * Create a sender process regulated for sending specific rate and payload.
	 * @param rate The load rate for this sender in [msg/s]
	 * @param payload The payload of each message in [kb]
	 * @param configServerAddress the address of the global confi server
	 * @param queue the queue under test
	 */
	public SenderLoader(int rate, int payload, String configServerAddress, String queue) {
		super();
		this.rate = rate;
		this.configServerAddress = configServerAddress;
		this.queueOnTest = queue;
		//Init the roq publisher
		initRoQpublisher();
		//Init the message payload
		//Remove the meta data size: as we send the time stamp and the key, we need to remove this size
		int sizeMD = (Long.toString(System.currentTimeMillis()) + " ").getBytes().length + "test".getBytes().length;
		this.payload = new byte[(payload-sizeMD>0)?(payload-sizeMD): payload];
		logger.debug("Starting load sender at a rate of "+ this.rate+"msg/s of "+this.payload+"kb");
	}

	/**
	 * Initializes the RoQ stuffs.
	 */
	private void initRoQpublisher() {
		//1. Creating the connection
		IRoQConnectionFactory factory = new RoQConnectionFactory(configServerAddress);
		connection = factory.createRoQConnection(this.queueOnTest);
		connection.open();
		//2. Creating the publisher and sending message
		publisher = connection.createPublisher();
		publisher.addTimeStamp(true);
	}

	/**
	 * Just send the message payload as fast possible.
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		logger.debug("Running send message");
		//Check if the connection is ready
		connection.blockTillReady(10000);
		//Reset the sent message
		this.sentMsg=0;
		//Send while reaching the rate
		while(this.sentMsg<this.rate){
			publisher.sendMessage("test".getBytes(), this.payload);
			this.sentMsg++;
		}
	}

	/**
	 * 
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		logger.info("Stopping Sender loader");
		this.connection.close();
		
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "Sender loader thread";
	}
	
	/**
	 * 
	 * @see java.util.TimerTask#cancel()
	 */
	@Override
	public boolean cancel() {
		this.shutDown();
		return super.cancel();
	}
	
	

}
