package org.roq.simulation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.roq.simulation.HALib.ClusterStateMaker;
import org.roq.simulation.test.RoQDockerTestCase;

import com.spotify.docker.client.DockerException;


public class TestGCMFailover extends RoQDockerTestCase {
		
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
	
	@Test
	public void testLeaderLostAndRecover() {
		try {
			// Create 2 queues
			initQueue("testQ0");
			initQueue("testQ1");
			
			// Provoking a fail-over by A & B & C
			
			// Pause and unpause zookeeper
			launcher.pauseZookeeper(30000);
			
			// Wait for leadLost
			Thread.sleep(15000);
						
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
		
	@Test
	public void testIdempotentQueueTransactionAndHCM() {
		try {
			String qName = "testIdempotentQ0";
			
			// Put the cluster in the state 1 Q not finished
			ArrayList<String> hcmList = launcher.getHCMAddressList();
			String hcmAddress = hcmList.get(0);
			String zkConnectionString = launcher.getZkConnectionString();		
			
			if (!(ClusterStateMaker.createQueueOnHCM(hcmAddress, qName) 
					&& ClusterStateMaker.createQueueTransaction(hcmAddress, zkConnectionString, qName)))
				throw new Exception("State not well created...");
			
			// Create the Q, check if the q has been recovered properly
			assertEquals(initQueue(qName), true);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				launcher.saveLogs(); // Save containers logs
			} catch (IOException | DockerException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testIdempotentQueueTransaction() {
		try {
			String qName = "testIdempotentQ1";
			
			// Put the cluster in the state 1 Q not finished
			ArrayList<String> hcmList = launcher.getHCMAddressList();
			String hcmAddress = hcmList.get(0);
			String zkConnectionString = launcher.getZkConnectionString();		
			
			if (!ClusterStateMaker.createQueueTransaction(hcmAddress, zkConnectionString, qName))
				throw new Exception("State not well created...");
			
			// Create the Q, check if the q has been recovered properly
			assertEquals(initQueue(qName), true);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				launcher.saveLogs(); // Save containers logs
			} catch (IOException | DockerException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
