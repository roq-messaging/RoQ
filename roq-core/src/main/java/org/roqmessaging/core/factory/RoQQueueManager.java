package org.roqmessaging.core.factory;

import java.net.ConnectException;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.client.IRoQQueueManagement;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.RoQGCMConnection;
import org.roqmessaging.zookeeper.RoQZKSimpleConfig;
import org.roqmessaging.zookeeper.RoQZooKeeper;

public class RoQQueueManager implements IRoQQueueManagement {
	private RoQGCMConnection gcmConnection;

	private RoQZooKeeper zk;
	private Logger logger = Logger.getLogger(RoQQueueManager.class);
	
	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param configManager the zookeeper IP addresses, the default port 5000 will be applied
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQQueueManager(String zkAddresses) {
		zk = new RoQZooKeeper(zkAddresses);
		zk.start();
		gcmConnection = new RoQGCMConnection(zk, 10, 5000);
	}
	
	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param configManager the zookeeper IP addresses, the default port 5000 will be applied
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQQueueManager(String zkAddresses, int maxRetry, int timeout) {
		zk = new RoQZooKeeper(zkAddresses);
		zk.start();
		gcmConnection = new RoQGCMConnection(zk, maxRetry, timeout);
	}

	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param cfg Zookeeper config
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQQueueManager(RoQZKSimpleConfig cfg) {
		zk = new RoQZooKeeper(cfg);
		zk.start();
		gcmConnection = new RoQGCMConnection(zk, 10, 5000);
	}

	@Override
	/**
	 * Create a new logical queue on the cluster
	 * @param the name of the queue to create
	 */
	public boolean createQueue(String queueName)
			throws IllegalStateException, ConnectException {
		// Open socket and init BSON request
		BSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_CREATE_QUEUE_AUTOMATICALLY);
		// Send request
		byte[] responseBytes = gcmConnection.sendRequest(BSON.encode(request), 5003);
		// Get Response
		BSONObject result = BSON.decode(responseBytes);
		// Close socket
		if ((Integer)result.get("RESULT") ==  RoQConstant.FAIL) {
			logger.error("The queue creation for  " + queueName
					+ " failed. Reason: " + ((String) result.get("COMMENT")));
			throw  new IllegalStateException("The queue removal process failed @ Management Controller");
		} else if ((Integer)result.get("RESULT") == RoQConstant.EXCHANGE_LOST){
			logger.error("The queue creation for  " + queueName
					+ " failed. Reason: " + "the leader has changed");
			return false;
		} else {
			logger.info("Queue " + queueName + " has been created");
			return true;
		}
	}
	
	@Override
	/**
	 * Remove a queue from the cluster
	 * @param the name of the queue to remove
	 */
	public boolean removeQueue(String queueName) 
			throws IllegalStateException, ConnectException {
		// Open socket and init BSON request
		BasicBSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_REMOVE_QUEUE);
		// Get Response
		byte[] responseBytes = gcmConnection.sendRequest(BSON.encode(request), 5003);
		BSONObject result = BSON.decode(responseBytes);
		// Close socket
		if ((Integer)result.get("RESULT") ==  RoQConstant.FAIL) {
			throw  new IllegalStateException("The queue removal process failed @ Management Controller");
		} else if ((Integer)result.get("RESULT") == RoQConstant.EXCHANGE_LOST){
			logger.error("The queue creation for  " + queueName
					+ " failed. Reason: " + "the leader has changed");
			return false;
		} else {
			logger.info("Queue has been removed @ Management Controller");
			return true;
		}		
	}
	
	@Override
	/**
	 * Remove a queue from the cluster
	 * @param the name of the queue to remove
	 */
	public boolean queueExists(String queueName) 
			throws IllegalStateException, ConnectException {
		// Open socket and init BSON request
		BasicBSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_QUEUE_EXISTS);
		// Get Response
		byte[] responseBytes = gcmConnection.sendRequest(BSON.encode(request), 5003);
		BSONObject result = BSON.decode(responseBytes);
		if((Integer)result.get("RESULT") ==  RoQConstant.FAIL){
			return false;
		} else {
			return true;
		}		
	}
	
	
	
	@Override
	/**
	 * TODO: Actually BSON ask for a host to perform
	 * this action.. It probably must be able to
	 * find the host via the Qname
	 */
	public boolean startQueue(String queueName, String host) 
			throws IllegalStateException, ConnectException {
		// Open socket and init BSON request
		BasicBSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_START_QUEUE);
		// Get Response
		byte[] responseBytes = gcmConnection.sendRequest(BSON.encode(request), 5003);
		BSONObject result = BSON.decode(responseBytes);
		if((Integer)result.get("RESULT") ==  RoQConstant.FAIL){
			throw  new IllegalStateException("The queue start process failed @ Management Controller");
		} else if ((Integer)result.get("RESULT") == RoQConstant.EXCHANGE_LOST){
			logger.error("The queue creation for  " + queueName
					+ " failed. Reason: " + "the leader has changed");
			return false;
		} else {
			logger.info("Queue has been started @ Management Controller");
			return true;
		}
	}

	@Override
	/**
	 * Stop a running Queue
	 * @param the name of the queue to stop
	 */
	public boolean stopQueue(String queueName) 
			throws IllegalStateException, ConnectException {
		// Open socket and init BSON request
		BasicBSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_STOP_QUEUE);
		// Get Response
		byte[] responseBytes = gcmConnection.sendRequest(BSON.encode(request), 5003);
		int response = Integer.parseInt(new String(responseBytes));
		if(response == RoQConstant.FAIL){
			throw  new IllegalStateException("The queue stop process failed @ Management Controller");
		} else if (response == RoQConstant.EXCHANGE_LOST){
			logger.error("The queue creation for  " + queueName
					+ " failed. Reason: " + "the leader has changed");
			return false;
		} else {
			logger.info("Queue has been stopped @ Management Controller");
			return true;
		}		
	}
	
	public void close() {
		zk.close();
	}

}
