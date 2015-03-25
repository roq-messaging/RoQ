package org.roq.simulation.HALib;

import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.management.zookeeper.RoQZooKeeperClient;
import org.roqmessaging.management.zookeeper.RoQZooKeeperConfig;
import org.zeromq.ZMQ;

public class ClusterStateMaker {
	
	/**
	 * This methode create the Q on the HCM but not on the GCM
	 */
	public static boolean createQueueOnHCM (String host, String qName) {
		ZMQ.Socket hcmSocket = getHCMSocket(host);
		
		// Send QCreation process to HCM
		hcmSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + qName).getBytes(), 0);
		byte[] response = hcmSocket.recv(0);
		String[] resultHost = new String(response).split(",");
		if (Integer.parseInt(resultHost[0]) == RoQConstant.CONFIG_CREATE_QUEUE_OK) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * This method creates a state in which the
	 * transaction is set in ZK an the queue
	 * is started on the HCM but has not been
	 * registered in ZK.
	 * 
	 * Allow to test the case where the 
	 * GCM crash after it had create a queue 
	 * on the HCM 
	 */
	public static boolean createQueueTransaction (String host, String zkConnectionString, String qName) {
		// Create the zookeeper transaction node
		RoQZooKeeperClient zk = createZkClient(zkConnectionString);
		zk.createQTransaction(qName, host);
		return true;
	}
	
	/**
	 * Get a socket to communicate with HCM
	 * @param host
	 * @return socket to HCM
	 */
	private static ZMQ.Socket getHCMSocket(String host) {
		// Init ZKSocket
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket socket = context.socket(ZMQ.REQ);
		String address = "tcp://" + host + ":5100";
		socket.connect(address);
		return socket;
	}
	
	/**
	 * Get a zookeeper client instance
	 * @param zkConnectionString
	 * @return ZkCLient
	 */
	private static RoQZooKeeperClient createZkClient(String zkConnectionString) {
		RoQZooKeeperConfig config = new RoQZooKeeperConfig();
		config.servers = zkConnectionString;
		return new RoQZooKeeperClient(config);
	}
}
