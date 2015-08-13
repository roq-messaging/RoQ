package org.roqmessaging.zookeeper;

public class RoQZKSimpleConfig {
	// Comma-separated list of host:port pairs, each corresponding to a ZooKeeper server.
	//   example: 127.0.0.1:2181
	public String servers;
	// Path is thus /"RoQ"/"queues"/queue/"scaling"
	public String znode_leaderAddress = "gcmLeader";
	// Name of the znode used as the root for all read/writes in ZooKeeper
	//   example: "RoQ"
	public String namespace = "RoQ";
	// Path to the exchanges for the monitors
	public String znode_queues_exchanges = "queuesExchanges";
}
