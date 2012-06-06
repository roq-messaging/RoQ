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
package org.roq.simulation.test;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.roq.simulation.RoQAllLocalLauncher;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.LogicalQFactory;

/**
 * Class RoQTestCase
 * <p> Description: Thest case that provides methods for popluating a queue for test.
 * 
 * @author sskhiri
 */
public class RoQTestCase {
	protected RoQAllLocalLauncher launcher = null;
	private Logger logger = Logger.getLogger(RoQTestCase.class);
	protected LogicalQFactory factory = null;
	protected IRoQSubscriberConnection subscriberConnection = null;
	protected IRoQConnection connection = null;
	
	
	 /**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.logger.info("Setup TEST");
		this.launcher = new RoQAllLocalLauncher();
		this.launcher.setConfigFile("testGCM.properties");
		this.launcher.setUp();
		this.factory = new LogicalQFactory(RoQUtils.getInstance().getLocalIP().toString());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.logger.info("Tear Down TEST");
		this.factory.clean();
		this.launcher.tearDown();
	}


	

	/**
	 * Create a queue and attach a standard subscriber subscribing on key "key"
	 * @param qName the name of the queue
	 * @throws Exception
	 */
	public void initQueue(String qName) throws Exception {
		//1. Create a queue
		this.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP());
		//Let the queue starting
		//Thread.sleep(2000);
		//2. Subscribing and publishing a message
		attachSUbscriber(qName);
		IRoQPublisher publisher = attachPublisher(qName);
		sendMsg(publisher);
		//3. Removing the queue
		this.factory.removeQueue(qName);
		//4. Let the time to remove the queue
		this.logger.info("Removing "+qName);
		Thread.sleep(4000);
	}

	/**
	 * Sends message on the connected queue
	 * @param publisher the publisher to use
	 */
	protected void sendMsg(IRoQPublisher publisher) {
		logger.info("Sending MESSAGES ...");
		for (int i = 0; i < 500; i++) {
			publisher.sendMessage("key".getBytes(), ("hello" + i).getBytes());
		}
		
	}

	/**
	 * Create a publisher
	 * @param qName the name of the Q to publish
	 */
	protected IRoQPublisher attachPublisher(String qName) {
		IRoQConnectionFactory factory = new RoQConnectionFactory(launcher.getConfigurationServer());
		connection = factory.createRoQConnection(qName);
		connection.open();
		// Creating the publisher and sending message
		IRoQPublisher publisher = connection.createPublisher();
		// Wait for the connection is established before sending the first
		// message
		connection.blockTillReady(10000);
		return publisher;
		
	}

	/**
	 * Attach a subscriber to the queue
	 * @param qName the queue to attach the subs.
	 */
	protected void attachSUbscriber(String qName) {
		IRoQConnectionFactory factory = new RoQConnectionFactory(launcher.getConfigurationServer());
		// add a subscriber
		subscriberConnection = factory.createRoQSubscriberConnection(qName, "key");
		// Open the connection to the logical queue
		subscriberConnection.open();
		// Register a message listener
		IRoQSubscriber subs = new IRoQSubscriber() {
			public void onEvent(byte[] msg) {
				String content = new String(msg, 0, msg.length);
				assert content.startsWith("hello");
			}
		};
		subscriberConnection.setMessageSubscriber(subs);
		
	}

}
