package org.roqmessaging.management.zookeeper;


import java.util.ArrayList;
import java.util.Collection;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.zookeeper.Metadata.HCM;
import org.zeromq.ZMQ;

public class HcmListener implements ServiceCacheListener {
	private Logger logger = Logger.getLogger(HcmListener.class);
	private RoQZooKeeperClient roQZooKeeperClient;
	private Collection<HCM> lastState;
	private ZMQ.Socket requestSocket;
	private ZMQ.Context context;
	
	public HcmListener(RoQZooKeeperClient roQZooKeeperClient) {
		super();
		this.roQZooKeeperClient = roQZooKeeperClient;
		context = ZMQ.context(1);
		this.requestSocket = context.socket(ZMQ.REQ);
		this.requestSocket.connect("tcp://"+RoQUtils.getInstance().getLocalIP()+":5003");
	}

	@Override
	public void stateChanged(CuratorFramework client,
			ConnectionState newState) {
		if (GlobalConfigurationManager.hasLead)
			logger.info("hcm cache state changed");
		else 
			logger.info("hcm cache state changed, perform no action: NOT LEADER");
	}

	private void closeSocketConnection() {
		this.logger.debug("Closing factory socket");
		this.requestSocket.setLinger(0);
		this.requestSocket.close();
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
								
								BSONObject request = new BasicBSONObject();
								request.put("Host", hcm.address);
								request.put("CMD", RoQConstant.EVENT_HCM_FAILURE);
								// Send request
								requestSocket.send(BSON.encode(request), 0);
								byte[] responseBytes = requestSocket.recv(0);
								BSONObject result = BSON.decode(responseBytes);
								if ((Integer)result.get("RESULT") ==  RoQConstant.FAIL) {
									logger.error("Failed to failover on another monitor");
								}
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
