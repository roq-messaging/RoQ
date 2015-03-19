package org.roqmessaging.management;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

/**
 * This class implements the interface LeaderLatchListener
 * of Curator. It allows to register a listener which
 * handle master change when active master is running.
 * That allows to handle the ZK connection lost.
 * 
 * @author BVM
 *
 */
public class GlobalConfigLeaderListener implements LeaderLatchListener {
	@Override
	public void notLeader() {
		// Notifies that the GCM has lost the lead
		GlobalConfigurationManager.hasLead = false;
	}
	
	@Override
	public void isLeader() {
		// Notifies that the GCM has the lead again
		GlobalConfigurationManager.hasLead = true;
		
	}

}
