package org.roqmessaging.zookeeper;



import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryOneTime;
import org.apache.log4j.Logger;

public class RoQZooKeeper {
	protected CuratorFramework client;
	protected RoQZKSimpleConfig cfg;
	private final Logger log = Logger.getLogger(getClass());
	
	public RoQZooKeeper(String zkAddresses) {
		cfg = new RoQZKSimpleConfig();
		cfg.servers = zkAddresses;
		
		// Start a Curator client, through which we can access ZooKeeper
		// Note: retry policy should be made configurable
		RetryPolicy retryPolicy = new RetryOneTime(1000);
				
		client = CuratorFrameworkFactory.builder()
				.connectString(zkAddresses)
				.namespace(cfg.namespace)
				.retryPolicy(retryPolicy)
				.build();
	}

	public RoQZooKeeper(RoQZKSimpleConfig config) {		
		// Start a Curator client, through which we can access ZooKeeper
		// Note: retry policy should be made configurable
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
		RoQZKHelpers.ZK_BASE = config.namespace;
		cfg = config;
		client = CuratorFrameworkFactory.builder()
					.connectString(config.servers)
					.retryPolicy(retryPolicy)
					.namespace(config.namespace)
					.build();	
	}
	
	// Start the client and leader election
	public void start() {
		log.info("");
		client.start();
	}
	
	public void close() {
		log.info("");
		client.close();
	}

	// Remove the znode if it exists.
	public String getGCMLeaderAddress() {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_leaderAddress);
		String gcm = RoQZKHelpers.getDataString(client, path);
		return gcm;
	}
	
	
	
}
