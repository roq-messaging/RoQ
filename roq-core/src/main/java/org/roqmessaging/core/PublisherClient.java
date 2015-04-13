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
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.state.PublisherConfigState;
import org.zeromq.ZMQ;

import com.google.common.primitives.Longs;

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
	private PublisherConnectionManager connectionManager = null;
	//Logger
	private Logger logger = Logger.getLogger(PublisherClient.class);
	//Define whether we need to add a time stamp in the message
	private boolean timeStp = false;
	
	/**
	 * Initiatilisation of the class
	 */
	public PublisherClient(PublisherConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	/**
	 * @throws InterruptedException we sleep when relocating publisher
	 * @see org.roqmessaging.client.IRoQPublisher#sendMessage(byte[], byte[])
	 */
	public boolean sendMessage(byte[] key, byte[] msg) throws IllegalStateException {
		//1. Get the configuration state
		PublisherConfigState configState = this.connectionManager.getConfiguration();
		try {
			blockTillReady();
		} catch (InterruptedException e) {
			logger.error(e);
		}
		ZMQ.Socket pubSocket = configState.getExchPub();
		boolean result = true;
		if(configState.isValid()){
			//2. If OK send the message
			result &= pubSocket.send(key, ZMQ.SNDMORE);
			result &= pubSocket.send(RoQSerializationUtils.stringToBytesUTFCustom(configState.getPublisherID()), ZMQ.SNDMORE);
			// pubSocket.send(configState.getPublisherID().getBytes(), ZMQ.SNDMORE);
						
			if (this.timeStp) {
				result &= pubSocket.send(msg, ZMQ.SNDMORE);
				result &= pubSocket.send(getTimestamp(), 0);
			}else {
				result &= pubSocket.send(msg, 0);
			}
			return result;
		}else{
			logger.error("The publisher configuration for "+configState.getPublisherID()+" is not valid " +
					"when sending the message");
			return false;
		}
	}
	
	/**
	 * Blocks in case of relocation
	 * @throws InterruptedException
	 */
	private void blockTillReady() throws InterruptedException {
		int wait=0;
		while(this.connectionManager.isRelocating()){
			if(wait==10) throw new IllegalStateException("The relocating state is during more than 100ms!!");
			Thread.sleep(10);
			wait++;
		}
		
	}

	/**
	 * @return the encoded time stamp
	 */
	private byte[] getTimestamp() {
//		return (Long.toString(System.currentTimeMillis()) + " ").getBytes();
		return Longs.toByteArray(System.currentTimeMillis());
	}

	/**
	 * @see org.roqmessaging.client.IRoQPublisher#addTimeStamp(boolean)
	 */
	public void addTimeStamp(boolean add) {
		this.timeStp=add;
		logger.info("A time stamp of "+ this.getTimestamp().length +" will be added as a message part.");
	}

}
