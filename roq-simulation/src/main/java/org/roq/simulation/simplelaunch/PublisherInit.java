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
package org.roq.simulation.simplelaunch;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.interfaces.IStoppable;

/**
 * Class PublisherInit
 * <p>
 * Description: This class is used to test a simple RoQ deployment on cluster.
 * It will initiate a subscriber, create a queue and will wait until we kill it.
 * 
 * @author sskhiri
 */
public class PublisherInit implements Runnable, IStoppable {
	private Logger logger = Logger.getLogger(PublisherInit.class);
	private volatile boolean active = true;
	private String qName = null;
	private String gcmAddress = null;
	private IRoQSubscriberConnection subscriberConnection = null;

	/**
	 * 1. Create A connection to the queue name 2. Register a Subscriber that
	 * will live in this thread
	 * 
	 * @param qName
	 *            the name of the queue to connect
	 * @param gcmAddress
	 *            the global configuration manager address
	 */
	public PublisherInit(String qName, String gcmAddress) {
		this.qName = qName;
		this.gcmAddress = gcmAddress;
		// Create subscriber
		IRoQConnectionFactory connectionFactory = new RoQConnectionFactory(gcmAddress);
		subscriberConnection = connectionFactory.createRoQSubscriberConnection(qName, "key");
		subscriberConnection.open();
		subscriberConnection.setMessageSubscriber(new IRoQSubscriber() {
			public void onEvent(byte[] msg) {
				logger.info("On message:" + new String(msg));
			}
		});
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		logger.info("Stoping Subscriber & removing queue");
		this.active = false;
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return this.getClass().getName();
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active
	 *            the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;

	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// Create a publisher on the connection
		// 1. Creating the connection
		IRoQConnectionFactory factory = new RoQConnectionFactory(this.gcmAddress);
		IRoQConnection connection = factory.createRoQConnection(this.qName);
		connection.open();
		// 2. Creating the publisher and sending message
		IRoQPublisher publisher = connection.createPublisher();
		// Wait for the connection is established before sending the first
		// message
		connection.blockTillReady(10000);
		// Send message while is active
		while (this.active) {
			publisher.sendMessage("key".getBytes(), "hello".getBytes());
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		logger.info("Stoping publisher thread");
		connection.close();
		this.subscriberConnection.close();

	}
}