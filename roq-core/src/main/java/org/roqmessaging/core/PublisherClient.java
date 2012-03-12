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
package org.roqmessaging.core;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.state.PublisherConfigState;
import org.zeromq.ZMQ;

/**
 * Class PublisherClient
 * <p> Description: Implementation of the publisher client library. 
 * <br> As the publisher client could potentially be in another thread, we prefer separating the 
 * configuration thread connected to the monitor and the sending thread that belongs to the caller.
 * 
 * @author sskhiri
 */
public class PublisherClient implements IRoQPublisher {
	//The Thread that manages the publisher configuration
	private PubConfigThread configThread = null;
	//Logger
	private Logger logger = Logger.getLogger(PublisherClient.class);
	
	/**
	 * Initiatilisation of the class
	 */
	public PublisherClient() {
		startConfigThread();
	
	}

	/**
	 * Start the configuration thread
	 */
	private void startConfigThread() {
		//TODO Hard coded configuration, this should be in a java.property file
		this.configThread = new PubConfigThread("localhost", false);
		Thread mainThread = new Thread(configThread);
		mainThread.start();
	
	}

	/**
	 * @see org.roqmessaging.client.IRoQPublisher#sendMessage(byte[], byte[])
	 */
	public boolean sendMessage(byte[] key, byte[] msg) throws IllegalStateException {
		//1. Get the configuration state
		PublisherConfigState configState = this.configThread.getConfiguration();
		if(configState.isValid()){
			//2. If OK send the message
			configState.getExchPub().send(key, ZMQ.SNDMORE);
			configState.getExchPub().send(configState.getPublisherID().getBytes(), ZMQ.SNDMORE);

			if (configState.isTimeStampServer()) {
				configState.getExchPub().send(msg, ZMQ.SNDMORE);
				configState.getExchPub().send(getTimestamp(), 0);
			}else {
				configState.getExchPub().send(msg, 0);
			}
			return true;
		}else{
			logger.error("The publisher configuration for "+configState.getPublisherID()+" is not valid " +
					"when sending the message");
			return false;
		}
	}
	
	private byte[] getTimestamp() {
		return (Long.toString(System.currentTimeMillis()) + " ").getBytes();
	}

}
