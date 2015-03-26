package org.roq.simulation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;
import org.roq.simulation.HALib.ClusterStateMaker;
import org.roq.simulation.test.RoQDockerTestCase;

public class TestGCMFailover extends RoQDockerTestCase {
		
	@Test
	/**
	 * This test if the Q data are consistent after a failover
	 */
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
		}
	}
		
	@Test
	/**
	 * This test check if a queue insertion is idempotent,
	 * ie. if a crash of the GCM occurs during the queue insertion
	 * process, a second request will recover the partially 
	 * created queue
	 */
	public void testIdempotentQueueTransactionAndHCM() {
		try {
			String qName = "testIdempotentQ0";
			
			// Put the cluster in the state 1 Q not finished
			ArrayList<String> hcmList = launcher.getHCMAddressList();
			String hcmAddress = hcmList.get(0);
			String zkConnectionString = launcher.getZkConnectionString();		
			
			// Setup cluster state for test
			if (!(ClusterStateMaker.createQueueOnHCM(hcmAddress, qName) 
					&& ClusterStateMaker.createQueueTransaction(hcmAddress, zkConnectionString, qName)))
				throw new Exception("State not well created...");
			
			// Create the Q, check if the q has been recovered properly
			assertEquals(initQueue(qName), true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/**
	 * Same as previous one but with another inital state
	 * This test check if a queue insertion is idempotent,
	 * ie. if a crash of the GCM occurs during the queue insertion
	 * process, a second request will recover the partially 
	 * created queue
	 */
	public void testIdempotentQueueTransaction() {
		try {
			String qName = "testIdempotentQ1";
			
			// Put the cluster in the state 1 Q not finished
			ArrayList<String> hcmList = launcher.getHCMAddressList();
			String hcmAddress = hcmList.get(0);
			String zkConnectionString = launcher.getZkConnectionString();		
			
			// Setup cluster state for test
			if (!ClusterStateMaker.createQueueTransaction(hcmAddress, zkConnectionString, qName))
				throw new Exception("State not well created...");
			
			// Create the Q, check if the q has been recovered properly
			assertEquals(initQueue(qName), true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	/**
	 * The same as idempotent queue 
	 * but for the exchange creation process
	 */
	public void testIdempotentExchange() {
		try {
			String qName = "testIdempotentQ2";
			initQueue(qName);
			
			// Put the cluster in the state 1 Q not finished
			ArrayList<String> hcmList = launcher.getHCMAddressList();
			String hcmAddress = hcmList.get(0);
			ArrayList<String> gcmList = launcher.getGCMAddressList();
			String gcmAddress = gcmList.get(0);
			String zkConnectionString = launcher.getZkConnectionString();
			
			
			ClusterStateMaker.createExchangeTransaction(hcmAddress, zkConnectionString, "id000");
			// Must create the transaction on the good hcm because it get it from the transaction
			ClusterStateMaker.createExchangeOnHCM(gcmAddress, "0.5445.1544", qName, "id000");
			
			// Must not create transactions
			ClusterStateMaker.createExchangeOnHCM(gcmAddress, "0.5445.1544", qName, "id000");
			ClusterStateMaker.createExchangeOnHCM(gcmAddress, "0.5445.1544", qName, "id000");
			
			// The initial Exchange created with the queue + the second that we made
			assertEquals(2, ClusterStateMaker.exchangeCountOnHCM(hcmAddress));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
