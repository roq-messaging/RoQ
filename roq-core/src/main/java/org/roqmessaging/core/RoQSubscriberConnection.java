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

import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;

/**
 * Class RoQSubscriberConnection
 * <p> Description: Implement the life cycle management of the {@link SubscriberConnectionManager}.
 * 
 * @author sskhiri
 */
public class RoQSubscriberConnection implements IRoQSubscriberConnection {
	//The connection manager
	private SubscriberConnectionManager connectionManager = null;
	//The key
	private String key = null;
	//The subscriber ID
	private int subscriberID = 0;
	
	/**
	 * @param key the filtering key.
	 */
	public RoQSubscriberConnection(String key, int subscriberID) {
		this.key = key;
		this.subscriberID = subscriberID;
	}
	/**
	 * @see org.roqmessaging.client.IRoQSubscriberConnection#open()
	 */
	public void open() {
		//TODO Hard coded configuration, this should be in a java.property file
		this.connectionManager = new SubscriberConnectionManager("localhost", this.key, this.subscriberID, false);
		Thread mainThread = new Thread(connectionManager);
		mainThread.start();

	}

	/**
	 * Close the connection and shutdown the main connection thread.
	 * @see org.roqmessaging.client.IRoQSubscriberConnection#close()
	 */
	public void close() {
		this.connectionManager.shutdown();

	}

	/* (non-Javadoc)
	 * @see org.roqmessaging.client.IRoQSubscriberConnection#registerSubscriber(org.roqmessaging.client.IRoQSubscriber)
	 */
	public void registerSubscriber(IRoQSubscriber subscriber) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.roqmessaging.client.IRoQSubscriberConnection#removeSubscriber(org.roqmessaging.client.IRoQSubscriber)
	 */
	public boolean removeSubscriber(IRoQSubscriber subscriber) {
		// TODO Auto-generated method stub
		return false;
	}

}
