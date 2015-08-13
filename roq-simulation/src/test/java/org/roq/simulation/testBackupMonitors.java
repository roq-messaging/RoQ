package org.roq.simulation;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;
import org.roq.simulation.test.RoQDockerTestCase;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.utils.Time;

public class testBackupMonitors extends RoQDockerTestCase {
	
	@Test
	public void testQueueWithBackupFailover() {
		try {
			// Select host which handle the master monitor
			ArrayList<String> hostAddresses = launcher.getHCMAddressList();
			String targetAddress = hostAddresses.get(0);
			String targetID = launcher.getHCMList().get(0);
			
			// Launch two additional hcms
			// in order to have enough HCM for replication
			launcher.launchHCM();
			launcher.launchHCM();
			
			hostAddresses = launcher.getHCMAddressList();
			String targetAddress2 = hostAddresses.get(2);
			
			Thread.sleep(8000);
			
			// Check if we have already three monitors
			assertEquals(3, hostAddresses.size());
			
			// Create a queue will be use to test monitor failover
			initQueue("testQ0", targetAddress);
			// Create a queue will be use to test standby monitor replacement
			initQueue("testQ1", targetAddress2);
			
			Thread.sleep(4000);
			launcher.launchHCM(); // one to test if the RF is maintained
			Thread.sleep(10000);
			// Kill the container which owns the Master
			launcher.stopHCMByID(targetID);
			System.out.println(Time.currentTimeSecs()%100);
			// Wait for recovery
			Thread.sleep(60000);
			
			// now we establish a connection with the monitor
			// in order to verify the failover mechanism
			System.out.println(Time.currentTimeSecs()%100);
		} catch (Exception e) {
			System.out.println("an error has occured: ");
			e.printStackTrace();
		}
		
		
		// 3. Create a subscriber
		IRoQConnectionFactory factory;
		try {
			factory = new RoQConnectionFactory(launcher.getZkConnectionString());
		
			// add a subscriber
			IRoQSubscriberConnection subConnection = factory.createRoQSubscriberConnection("testQ0", "key");
			// Open the connection to the logical queue
			subConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					String content = new String(msg, 0, msg.length);
					logger.info(content);
					assert content.startsWith("hello");
				}
			};
			subConnection.setMessageSubscriber(subs);
			
			IRoQConnection connection = factory.createRoQConnection("testQ0");
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
			
			Thread.sleep(10000);
			
			connection.close();
			subConnection.close();
			
			rmQueue("testQ0");
			rmQueue("testQ1");
			
		} catch (Exception e) {
			assertEquals(false, true);
		}		
		
	}
	
	
	@Test
	public void testQueueWithFailoverDuringPUBSUB() {
		try {
			// Select host which handle the master monitor
			ArrayList<String> hostAddresses = launcher.getHCMAddressList();
			String targetAddress = hostAddresses.get(0);
			String targetID = launcher.getHCMList().get(0);
			
			// Launch two additional hcms
			// in order to have enough HCM for replication
			launcher.launchHCM();
			launcher.launchHCM();
			
			hostAddresses = launcher.getHCMAddressList();
			String targetAddress2 = hostAddresses.get(2);
			
			Thread.sleep(8000);
			
			// Check if we have already three monitors
			assertEquals(3, hostAddresses.size());
			
			// Create a queue will be use to test monitor failover
			initQueue("testQ0", targetAddress);
			// Create a queue will be use to test standby monitor replacement
			initQueue("testQ1", targetAddress2);
			
			Thread.sleep(4000);
			launcher.launchHCM(); // one to test if the RF is maintained
			Thread.sleep(10000);
			
			IRoQConnectionFactory factory = new RoQConnectionFactory(launcher.getZkConnectionString());;
			
			// add a subscriber
			IRoQSubscriberConnection subConnection = factory.createRoQSubscriberConnection("testQ0", "key");
			// Open the connection to the logical queue
			subConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					String content = new String(msg, 0, msg.length);
					logger.info(content);
					assert content.startsWith("hello");
				}
			};
			subConnection.setMessageSubscriber(subs);

			// Create a publisher and send message in a thread
			IRoQConnection connection = factory.createRoQConnection("testQ0");
			MessageSender sender = new MessageSender(connection, 160);
			Thread senderThread = new Thread(sender);
			senderThread.start();
			
			// wait for connection establishment
			Thread.sleep(10000);
			
			// Kill the container which owns the Master
			launcher.stopHCMByID(targetID);
			System.out.println(Time.currentTimeSecs()%100);
			
			// Wait for recovery
			Thread.sleep(60000);
			
			// now we establish a connection with the monitor
			// in order to verify the failover mechanism
			System.out.println(Time.currentTimeSecs()%100);
			
			senderThread.join();
			
			connection.close();
			subConnection.close();
			
			rmQueue("testQ0");
			rmQueue("testQ1");
			
		} catch (Exception e) {
			assertEquals(false, true);
		}		
		
	}
	
	@Test
	public void testRemoveQueueWithBackup() {
		try {
			// Select host which handle the master monitor
			ArrayList<String> hostAddresses = launcher.getHCMAddressList();
			String targetAddress = hostAddresses.get(0);
			
			// Launch two additional hcms
			// in order to have enough HCM for replication
			launcher.launchHCM();
			launcher.launchHCM();
			
			Thread.sleep(8000);
			
			hostAddresses = launcher.getHCMAddressList();
			// Check if we have already three monitors
			assertEquals(3, hostAddresses.size());
			
			// Create a queue
			initQueue("testQ0", targetAddress);
			
			Thread.sleep(10000);
			
			// remove a queue
			rmQueue("testQ0");

		} catch (Exception e) {
			System.out.println("an error has occured: ");
			e.printStackTrace();
		}
		
	}

}
