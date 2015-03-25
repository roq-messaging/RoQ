package org.roq.simulation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.roq.simulation.test.RoQDockerTestCase;

import com.spotify.docker.client.DockerException;


public class TestGCMFailover extends RoQDockerTestCase {
	
	@Test
	public void testDataConsistency() {
		try {
			// Create 2 queues
			initQueue("testQ0");
			initQueue("testQ1");
			
			// Provoking a fail-over by
			
			// A: Get current GCM ID
			ArrayList<String> gcmList = launcher.getGCMList();
			
			String id = gcmList.get(0);
			
			// B: Creating a new GCM
			launcher.launchGCM();
			// Removing the active one
			launcher.stopGCMByID(id);
						
			// Check if queues always exist
			assertEquals(true, queueExists("testQ0"));
			assertEquals(true, queueExists("testQ0"));
						
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			queueManager.close();
			connection.close();
			try {
				launcher.saveLogs();
			} catch (IOException | DockerException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
