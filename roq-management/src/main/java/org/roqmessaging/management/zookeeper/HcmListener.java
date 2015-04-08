package org.roqmessaging.management.zookeeper;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.log4j.Logger;
import org.roqmessaging.management.GlobalConfigurationManager;

public class HcmListener implements ServiceCacheListener {
	private Logger logger = Logger.getLogger(HcmListener.class);

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
		// TODO Auto-generated method stub
		if (GlobalConfigurationManager.hasLead)
			logger.info("hcm cache changed");
		else 
			logger.info("hcm cache changed, perform no action: NOT LEADER");
	}
	
}
