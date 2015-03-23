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
package org.roqmessaging.management;

import java.net.ConnectException;

import org.apache.curator.test.TestingServer;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.internal.FileConfigurationReader;
import org.roqmessaging.management.config.internal.GCMPropertyDAO;

/**
 * Class TestLogicalQueue
 * <p>
 * Description: Test the logical queue factory methods.
 * 
 * @author sskhiri
 */
public class TestLogicalQueue {
	private static final Logger logger = Logger.getLogger(TestLogicalQueue.class);
	private GlobalConfigurationManager configurationManager = null;
	private LogicalQFactory factory = null;
	private HostConfigManager hostConfigManager = null;

	private TestingServer zkServer;
	private String configFile = "testGCM.properties";
	
	/**
	 * Helper method to create an instance of {@link GlobalConfigurationManager}
	 * based on the provided configuration file and zookeeper connection string.
	 * The function takes care to overwrite the "zkConfig.servers" property
	 * with the one provided to ensure that the GCM connects to the correct
	 * zookeeper server.
	 * 
	 * @param configFile       the gcm configuration file
	 * @param zkConnectString  the comma-separated string of ip:port zookeeper addresses
	 * @return                 a new instance of {@link GlobalConfigurationManager}
	 * @throws Exception
	 */
	private GlobalConfigurationManager createGCM(String configFile, String zkConnectString) throws Exception {
		logger.info("Creating GCM");
		// Ignore the "zk.servers" property defined in the configuration file
		// and overwrite it with the connection string provided by the TestingServer class.
		GCMPropertyDAO gcmConfig = new FileConfigurationReader().loadGCMConfiguration(configFile);
		gcmConfig.zkConfig.servers = zkConnectString;
		
		CloudConfig cloudConfig = new FileConfigurationReader().loadCloudConfiguration(configFile);
		return new GlobalConfigurationManager(gcmConfig, cloudConfig);
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// 1. Start the configuration
		logger.info("Initial setup Start global config thread");
		logger.info("Start global config...");
		
		zkServer = new TestingServer();
		
		if (configurationManager == null) {
			configurationManager = createGCM(configFile, zkServer.getConnectString());
			Thread configThread = new Thread(configurationManager);
			configThread.start();
		}
		logger.info("Start host config...");
		if (hostConfigManager == null) {
			hostConfigManager = new HostConfigManager("HCM.properties");
			Thread hostThread = new Thread(hostConfigManager);
			hostThread.start();
		}
		logger.info("Start factory config...");
		if (factory == null)
			factory = new LogicalQFactory(RoQUtils.getInstance().getLocalIP().toString());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.configurationManager.getShutDownMonitor().shutDown();
		this.hostConfigManager.getShutDownMonitor().shutDown();
		this.factory.clean();
		this.zkServer.close();
		Thread.sleep(3000);
	}

	@Test
	public void testCreateQueueRequest() {
		// Let 1 sec to init the thread
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error("Error while waiting", e);
		}
		logger.info("Test refresh");
		factory.refreshTopology();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.error("Error while waiting", e);
		}
		// 1. Add new host - now it is automatic
		String host = RoQUtils.getInstance().getLocalIP();
		try {
			// 2. test the  Q
			testQueue("queue1");
			//testQueue("queue2");
			
			//3. Create exchange
			this.factory.createExchange("queue1", host, "TEST1");
			Thread.sleep(4000);
			this.factory.removeQueue("queue1");
			//this.factory.removeQueue("queue2");
			Thread.sleep(4000);
			// factory.createQueue("Sabri2", host);
		} catch (IllegalStateException e) {
			logger.error("Error while waiting", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * @param qName the name of the queue to test
	 * @throws InterruptedException 
	 */
	private void testQueue(String qName) throws InterruptedException {
		logger.info("**********Testing Q "+ qName+"***************");
		String host = RoQUtils.getInstance().getLocalIP();
		factory.createQueue(qName, host, false);
		//Wait for the queue ready
		Thread.sleep(2000);
		//Add a subscriber
		createSubscriber(qName, "key", host);
		
		// Add a publisher
		RoQConnectionFactory factory = new RoQConnectionFactory("localhost");
		// 1. Creating the connection
		IRoQConnection connection;
		try {
			connection = factory.createRoQConnection(qName);
		
			connection.open();
			// 2. Creating the publisher and sending message
			IRoQPublisher publisher = connection.createPublisher();
			//3. Wait for the connection is established before sending the first message
			connection.blockTillReady(10000);
			//4. Sending the message
			publisher.sendMessage("key".getBytes(), "hello".getBytes());
	
			Thread.sleep(500);
		} catch (ConnectException | IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Create a RoQ subscriber
	 * @param qName the name of the queue
	 * @param key the key to filter
	 * @param configurationServer the address of the configuration server
	 * @return a RoQ subscriber
	 */
	private IRoQSubscriber createSubscriber(String qName, final String key, String configurationServer) {
		IRoQConnectionFactory factory = new RoQConnectionFactory(configurationServer);
		// add a subscriber
		IRoQSubscriberConnection subConnection = null;
		IRoQSubscriber subs = null;
		try {
			subConnection = factory.createRoQSubscriberConnection(qName, key);
			// 2. Open the connection to the logical queue
			subConnection.open();
			
			// 3. Register a message listener
			subs = new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					String content = new String(msg, 0, msg.length);
					assert content.equals("hello");
					logger.info("In message lIstener recieveing :" + content + " on Key :"+ key);
				}
			};
			subConnection.setMessageSubscriber(subs);
		} catch (ConnectException | IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return subs;
	}

}
