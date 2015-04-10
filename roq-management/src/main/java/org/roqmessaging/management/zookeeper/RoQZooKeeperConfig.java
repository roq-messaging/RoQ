package org.roqmessaging.management.zookeeper;

import org.roqmessaging.zookeeper.RoQZKSimpleConfig;

public class RoQZooKeeperConfig extends RoQZKSimpleConfig{
	// ZNode used to register GlobalConfigurationManager instances
	// and perform leader election.
	public String znode_gcm = "gcm";
	// ZNode used to register HostConfigManager instances
	public String znode_hcm = "hcm-list";
	// Parent ZNode for all queues
	public String znode_queues = "queues";
	// ZNode used to store the cloud configuration
	public String znode_cloud = "cloud";
	// ZNode used to store a queue's scaling config
	// Path is thus /"RoQ"/"queues"/queue/"scaling"
	public String znode_scaling = "scaling";
	// Path to transaction node for queues
	public String znode_queue_transactions = "queueTransactions";
	
	public String znode_hcm_remove_transactions = "hcmRMTransactions";
	// Path to transaction node for exchanges
	public String znode_exchange_transactions = "xchangeTransactions";
	
}
