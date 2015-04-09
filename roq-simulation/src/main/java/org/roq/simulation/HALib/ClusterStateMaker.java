package org.roq.simulation.HALib;

import java.util.ArrayList;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.management.zookeeper.RoQZooKeeperClient;
import org.roqmessaging.management.zookeeper.RoQZooKeeperConfig;
import org.zeromq.ZMQ;

public class ClusterStateMaker {
	private final static Logger logger = Logger.getLogger(ClusterStateMaker.class);
	
	/**
	 * This method create the Q on the HCM but not on the GCM
	 */
	public static boolean createQueueOnHCM (String host, String qName) {
		logger.info("HCM creation");
		ZMQ.Socket hcmSocket = getSocket(host, "5100");
		
		// Send QCreation process to HCM
		hcmSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + qName).getBytes(), 0);
		byte[] response = hcmSocket.recv(0);
		hcmSocket.close();
		String[] resultHost = new String(response).split(",");
		if (Integer.parseInt(resultHost[0]) == RoQConstant.CONFIG_REQUEST_OK) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Create an exchange on the provided HCM
	 * @param gcm
	 * @param host
	 * @param qName
	 * @param transactionID
	 * @return
	 */
	public static boolean createExchangeOnHCM (String gcm, String host, String qName, String transactionID) {
		logger.info("XCHange creation");
		ZMQ.Socket gcmSocket = getSocket(gcm, "5003");
		// Open socket and init BSON request
		BSONObject request = new BasicBSONObject();
		request.put("QName", qName);
		request.put("Host", host);
		request.put("TransactionID", transactionID);
		request.put("CMD", RoQConstant.BSON_CONFIG_CREATE_XCHANGE);
		// Send request
		gcmSocket.send(BSON.encode(request), 0);
		byte[] response = gcmSocket.recv(0);
		gcmSocket.close();
		// Get Response
		BSONObject result = BSON.decode(response);
		// Close socket
		if ((Integer)result.get("RESULT") ==  RoQConstant.FAIL) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Create an exchange transaction in ZK
	 * @param host
	 * @param zkConnectionString
	 * @param transID
	 * @return
	 */
	public static boolean createExchangeTransaction (String host, String zkConnectionString, String transID) {
		logger.info("xChange transaction");
		// Create the zookeeper transaction node
		RoQZooKeeperClient zk = createZkClient(zkConnectionString);
		zk.initZkClusterNodes();
		zk.start();
		zk.createExchangeTransaction(transID, host);
		zk.close();
		return true;
	}
	
	/**
	 * Create a transaction in ZK for queue Qname
	 */
	public static boolean createQueueTransaction (String host, String zkConnectionString, String qName) {
		logger.info("Zk transaction Q");
		// Create the zookeeper transaction node
		RoQZooKeeperClient zk = createZkClient(zkConnectionString);
		zk.initZkClusterNodes();
		zk.start();
		zk.createQTransaction(qName, host, new ArrayList<String>());
		zk.close();
		return true;
	}
	
	/**
	 * Get the number of Exchanges for a given HCM
	 * @param host
	 * @return
	 */
	public static int exchangeCountOnHCM(String host) {
		logger.info("XCHange count");
		ZMQ.Socket hcmSocket = getSocket(host, "5100");
		
		// Send Informations Request to HCM
		hcmSocket.send((Integer.toString(RoQConstant.CONFIG_INFO_EXCHANGE)).getBytes(), 0);
		hcmSocket.recv(0);
		byte[] response = hcmSocket.recv(0);
		hcmSocket.recv(0);
		hcmSocket.close();
		return Integer.parseInt(new String(response));
	}
	
	/**
	 * Get a ZMQ socket to communicate with a server
	 * @param host
	 * @return socket to HCM
	 */
	private static ZMQ.Socket getSocket(String host, String port) {
		// Init ZKSocket
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket socket = context.socket(ZMQ.REQ);
		String address = "tcp://" + host + ":" + port;
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
