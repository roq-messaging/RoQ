package org.roq.simulation;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;
import org.roq.simulation.test.RoQDockerTestCase;
import org.roqmessaging.utils.Time;

public class testBackupMonitors extends RoQDockerTestCase {
	
	@Test
	public void testQueueWithBackupCreation() {
		try {
			// Select host which handle the master monitor
			ArrayList<String> hostAddresses = launcher.getHCMAddressList();
			String targetAddress = hostAddresses.get(0);
			String targetID = launcher.getHCMList().get(0);
			
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
			
			Thread.sleep(4000);
			
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
