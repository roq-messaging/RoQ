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
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;

/**
 * Class RoQPublisherConnection
 * <p> Description: Handle to the {@linkplain PublisherConnectionManager} and manages its life cycle management
 * through API offer to the client.
 * 
 * @author sskhiri
 */
public class RoQPublisherConnection implements IRoQConnection {
	//The Thread that manages the publisher configuration
	private PublisherConnectionManager connectionManager = null;
	//Logger
	private Logger logger = Logger.getLogger(RoQPublisherConnection.class);

	/** 
	 * Starts the connection manager thread.
	 * @see org.roqmessaging.client.IRoQConnection#open()
	 */
	public void open() {
		//TODO Hard coded configuration, this should be in a java.property file
		this.connectionManager = new PublisherConnectionManager("localhost", false);
		Thread mainThread = new Thread(connectionManager);
		mainThread.start();

	}

	/**
	 * Close the connection by stopping the connection manager thread.
	 * @see org.roqmessaging.client.IRoQConnection#close()
	 */
	public void close() {
		this.logger.debug("Closing the Publisher Manager thread");
		this.connectionManager.shutDown();

	}

	/**
	 * Creates a Publisher and gives it the connection manager thread reference.
	 * @see org.roqmessaging.client.IRoQConnection#createPublisher()
	 */
	public IRoQPublisher createPublisher() throws IllegalStateException {
		if(this.connectionManager == null) throw new IllegalStateException("The connection were not openned");
		IRoQPublisher publisher = new PublisherClient(this.connectionManager);
		return publisher;
	}

	/**
	 * @return true if the connection is ready
	 * @see org.roqmessaging.client.IRoQConnection#isReady()
	 */
	public boolean isReady() throws IllegalStateException {
		if(this.connectionManager == null) throw new IllegalStateException("The connection were not openned");
		return this.connectionManager.getConfiguration().isValid();
	}
	
	//Private methods
	
}
