package org.roqmessaging.management.zookeeper;

public class RoQZooKeeperConfig {
	// Comma-separated list of host:port pairs, each corresponding to a ZooKeeper server.
	//   example: 127.0.0.1:2181
	public String servers;
	// Name of the znode used as the root for all read/writes in ZooKeeper
	//   example: "RoQ"
	public String namespace = "RoQ";
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
}
