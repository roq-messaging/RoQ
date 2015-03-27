package org.roq.simulation;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQQueueManagement;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.factory.RoQQueueManager;

public class TestProcessesRecovery  {
	protected RoQAllLocalLauncher launcher = null;
	protected IRoQQueueManagement qManagementfactory;
	protected IRoQConnectionFactory factory;
	private Logger logger = Logger.getLogger(TestProcessesRecovery.class);
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.launcher = new RoQAllLocalLauncher();
		this.launcher.setConfigFile("testGCM.properties");
		this.launcher.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		qManagementfactory.close();
		factory.close();
		this.launcher.tearDown();
		Thread.sleep(3000);
	}
	
	@Test
	public void testMonitorRecover() {
		String qName ="queueTestRecovery";
		try {
			// 1. Create a Queue
			
			qManagementfactory = new RoQQueueManager(launcher.getZkServerAddress());
			assertEquals(this.qManagementfactory.createQueue(qName), true);
			
			// 3. Create a subscriber
			factory = new RoQConnectionFactory(launcher.getZkServerAddress());
			IRoQSubscriberConnection subConnection = factory.createRoQSubscriberConnection(qName, "key");
			// Open the connection to the logical queue
			subConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					String content = new String(msg, 0, msg.length);
					assertEquals(content.startsWith("hello"), true);
				}
			};
			subConnection.setMessageSubscriber(subs);
			
			//kill the monitor process
			Thread.sleep(60000);

			// 4. Create a publisher// Add a publisher
			// Creating the connection
			IRoQConnection connection = factory.createRoQConnection(qName);
			connection.open();
			// Creating the publisher and sending message
			IRoQPublisher publisher = connection.createPublisher();
			// Wait for the connection is established before sending the first
			// message
			connection.blockTillReady(10000);

			// 5 Sending the message
			logger.info("Sending MESSAGES ...");
			for (int i = 0; i < 500; i++) {
				publisher.sendMessage("key".getBytes(), ("hello" + i).getBytes());
			}
			
			// End close connection
			connection.close();
			subConnection.close();

			// Delete the queue
			assertEquals(this.qManagementfactory.removeQueue(qName), true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
