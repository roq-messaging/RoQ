package org.roqmessaging.management.zookeeper;


import java.util.ArrayList;
import java.util.Collection;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.log4j.Logger;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.zookeeper.Metadata.HCM;

public class HcmListener implements ServiceCacheListener {
	private Logger logger = Logger.getLogger(HcmListener.class);
	private RoQZooKeeperClient roQZooKeeperClient;
	private Collection<HCM> lastState;
	
	public HcmListener(RoQZooKeeperClient roQZooKeeperClient) {
		super();
		this.roQZooKeeperClient = roQZooKeeperClient;
	}

	@Override
	public void stateChanged(CuratorFramework client,
			ConnectionState newState) {
		if (GlobalConfigurationManager.hasLead)
			logger.info("hcm cache state changed");
		else 
			logger.info("hcm cache state changed, perform no action: NOT LEADER");
	}

	@Override
	public void cacheChanged() {
		try {
			Collection<HCM> currentState = roQZooKeeperClient.getHCMList();
			if (lastState != null) {
				if (GlobalConfigurationManager.hasLead) {
					logger.info("hcm cache changed");
					Collection<HCM> hcmLosts = new ArrayList<HCM>();
					for (HCM hcm : lastState) {
						if (!currentState.contains(hcm))
							hcmLosts.add(hcm);
					}
					if (!hcmLosts.isEmpty()) {
						for (HCM hcm : hcmLosts) {
							// Check if the HCM has been explicitely removed or not
							if (roQZooKeeperClient.hcmRemoveTransactionExists(hcm) == null) {
								logger.info("hcm failed: " + hcm.address);
								// TODO trigger monitor recovery
								roQZooKeeperClient.removeHcmRemoveTransaction(hcm);
							} else {
								logger.info("hcm removed explicitely: " + hcm.address);
								roQZooKeeperClient.removeHcmRemoveTransaction(hcm);
							}
						}
					}
				}
				else {
					logger.info("hcm cache changed, perform no action: NOT LEADER");
				}
			}
			lastState = currentState;
		} catch (Exception e) {
			logger.error("failed to handle HCM lost: " + e);
		}
	}
	
}
