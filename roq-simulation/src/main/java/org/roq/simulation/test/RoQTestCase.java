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

import java.net.ConnectException;
import java.util.ArrayList;

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
 * <p> Description: Thest case that provides methods for populating a queue for test.
 * In the setup we launch the RoQ all launcher that will instantiate a complete RoQ infra,
 *  in addition we set the configuration file with the testGCM.properties.
 *  Finally we initiate a factory in case of queue or exchange creation. 
 *  
 *  Going further the case provides facility method to initiate queue and automatically
 *  attaching subscriber. Publisher can also be created easily. 
 * 
 * @author sskhiri
 */
public class RoQTestCase {
	protected RoQAllLocalLauncher launcher = null;
	protected Logger logger = Logger.getLogger(RoQTestCase.class);
	protected LogicalQFactory factory = null;
	protected IRoQSubscriberConnection subscriberConnection = null;
	protected IRoQConnection connection = null;
	
	
	 /**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.logger.info("Setup TEST");
		Thread.sleep(3000);
		this.launcher = new RoQAllLocalLauncher();
		this.launcher.setConfigFile("testGCM.properties");
		this.launcher.setUp();
		this.factory = new LogicalQFactory(RoQUtils.getInstance().getLocalIP().toString());
		Thread.sleep(1000);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.logger.info("Tear Down TEST");
		if(this.subscriberConnection!=null){
			subscriberConnection.close();
		}
		if(this.connection!=null){
			this.connection.close();
		}
		this.factory.clean();
		this.launcher.tearDown();
		Thread.sleep(4000);
	}
	

	/**
	 * Create a queue and attach a standard subscriber subscribing on key "key"
	 * @param qName the name of the queue
	 * @throws Exception
	 */
	public void initQueue(String qName) throws Exception {
		//1. Create a queue
		this.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP(), new ArrayList<String>(), false);
		//Let the queue starting
		Thread.sleep(2000);
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
		IRoQConnectionFactory factory = new RoQConnectionFactory(launcher.getZkServerAddress());
		IRoQPublisher publisher = null;
		try {
			connection = factory.createRoQConnection(qName);
		
			connection.open();
			// Creating the publisher and sending message
			publisher = connection.createPublisher();
			// Wait for the connection is established before sending the first
			// message
			connection.blockTillReady(10000);
		} catch (ConnectException | IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return publisher;
		
	}

	/**
	 * Attach a subscriber to the queue on the topic "key". The test must send 
	 * a java-encoded verison of "hello".
	 * @param qName the queue to attach the subs.
	 */
	protected void attachSUbscriber(String qName) {
		IRoQConnectionFactory factory = new RoQConnectionFactory(launcher.getZkServerAddress());
		// add a subscriber
		try {
			subscriberConnection = factory.createRoQSubscriberConnection(qName, "key");
		
			// Open the connection to the logical queue
			subscriberConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					//String content = new String(msg, 0, msg.length);
				}
			};
			subscriberConnection.setMessageSubscriber(subs);
		} catch (ConnectException | IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
