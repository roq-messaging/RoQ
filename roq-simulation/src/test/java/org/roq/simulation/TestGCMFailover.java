package org.roq.simulation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.roq.simulation.test.RoQDockerTestCase;

import com.spotify.docker.client.DockerException;


public class TestGCMFailover extends RoQDockerTestCase {
	
	private volatile int QueueNumber = 0;
	
	@Test
	public void testDataConsistency() {
		try {
			// Create 2 queues
			initQueue("testQ0");
			initQueue("testQ1");
			
			// Provoking a fail-over by A & B & C
			
			// A: Get current GCM ID
			ArrayList<String> gcmList = launcher.getGCMList();
			String id = gcmList.get(0);
			
			// B: Creating a new GCM
			launcher.launchGCM();
			// C: Removing the active one
			launcher.stopGCMByID(id);
						
			// Check if queues always exist
			assertEquals(true, queueExists("testQ0"));
			assertEquals(true, queueExists("testQ0"));
						
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				launcher.saveLogs();
			} catch (IOException | DockerException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
//	@Test
//	public void testIdempotentQueue() {
//		try {
//			int alreadyExists = 0;
//			
//			// Retrying until we have a idempotentQueue creation
//			while (QueueNumber - alreadyExists == 0) {
//				alreadyExists = 0;
//				
//				// Start threads which create Queue
//				QueueCreatorThread QCreation = new QueueCreatorThread();
//				Thread QCreationThread = new Thread(QCreation);
//				QCreationThread.start();
//				
//				// Let the thread create Q
//				Thread.sleep(15000);
//				
//				// Provoking a fail-over by A & B & C
//				
//				// A: Get current GCM ID
//				ArrayList<String> gcmList = launcher.getGCMList();
//				String id = gcmList.get(0);
//				
//				// C: Removing the active one
//				launcher.stopGCMByID(id);
//				
//				// Stop to create queue
//				// Wait a minute for thread stop
//				QCreationThread.wait();
//				
//				// B: Creating a new GCM
//				launcher.launchGCM();
//				
//				// Recreate the Queue via the new GCM
//				// Normally we will have at least one recovery process
//				for (int i = 0; i <= QueueNumber; i++) {
//					try {
//						initQueue("Queue" + i);
//					} catch (IllegalStateException e) {
//						// We catch the exception that means that the 
//						// Queue already exists
//						alreadyExists++;
//						logger.info("Queue" + i + " was already created");
//					}
//				}
//				tearDown();
//				if ((QueueNumber + 1) - alreadyExists == 0)
//					setUp();
//			}
//			
//			logger.info("" + (QueueNumber - alreadyExists) + " recovery process done");
//			
//			// Check if all these queues exists
//			for (int i = 0; i < QueueNumber; i++) {
//				assertEquals(true, queueExists("Queue" + i));
//			}	
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				launcher.saveLogs(); // Save containers logs
//			} catch (IOException | DockerException | InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}
	
	
	/**
	 * Queue creator
	 * @author benjamin
	 *
	 */
	class QueueCreatorThread implements Runnable {
		@Override
		public void run() {
			try {
				QueueNumber = 0;
				while (true) {						
					initQueue("Queue" + QueueNumber);
					QueueNumber++;
				}
			} catch (Exception e) {
				// Will fail an exit the loops
				// Due to TimeOut & maxTry Reached
				// e.printStackTrace();
			}
		
		}
	}

}
