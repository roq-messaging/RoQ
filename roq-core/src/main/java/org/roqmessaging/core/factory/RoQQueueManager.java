package org.roqmessaging.core.factory;

import java.net.ConnectException;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.client.IRoQQueueManagement;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.zookeeper.RoQZKSimpleConfig;
import org.roqmessaging.zookeeper.RoQZooKeeper;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class RoQQueueManager implements IRoQQueueManagement {
	//The global config server address 
	private String configServer = null;
	// ZMQ config
	private Context context= null;
	//The socket to the global config
	private Socket globalConfigReq;
	
	// TODO: Following params in a config file
	// Number of times that the client retry the request
	private int maxRetry = 5;
	// the rcv timeout of ZMQ
	private int timeout = 5000;
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
	}

	/**
	 * Build  a connection Factory and takes the location of the global configuration manager as input
	 * @param cfg Zookeeper config
	 * @param maxRetry the number of times that the client try to process the request
	 * @param the timeout between each retry
	 */
	public RoQQueueManager(RoQZKSimpleConfig cfg) {
		zk = new RoQZooKeeper(cfg);
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
		byte[] responseBytes = sendRequest(BSON.encode(request));
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
		byte[] responseBytes = sendRequest(BSON.encode(request));
		int response = Integer.parseInt(new String(responseBytes));
		if(response == RoQConstant.FAIL){
			throw  new IllegalStateException("The queue removal process failed @ Management Controller");
		} else if (response == RoQConstant.EXCHANGE_LOST){
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
		byte[] responseBytes = sendRequest(BSON.encode(request));
		int response = Integer.parseInt(new String(responseBytes));
		if(response == RoQConstant.FAIL){
			throw  new IllegalStateException("The queue start process failed @ Management Controller");
		} else if (response == RoQConstant.EXCHANGE_LOST){
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
		byte[] responseBytes = sendRequest(BSON.encode(request));
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
	
	
	/**
	 * Removes the socket connection to the global config manager
	 */
	private void closeSocketConnection() {
		this.logger.debug("Closing factory socket");
		this.globalConfigReq.setLinger(0);
		this.globalConfigReq.close();
	}

	/**
	 * Initialise the socket connection.
	 */
	private void initSocketConnection() 
				throws IllegalStateException {
		// Get the active master address
		this.configServer = zk.getGCMLeaderAddress();
		if (configServer == null) 
			throw new IllegalStateException("No GCM found");
		logger.info("Active GCM address: " + this.configServer);
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://"+this.configServer+":5003");
		globalConfigReq.setReceiveTimeOut(timeout);
	}

	/**
	 * This method sends a request to the GCM.
	 * First it get the active GCM, then it sent the request
	 * If the request fails because leader has changed or the connection
	 * cannot be established (timeout), the request is resent up to maxRetry times.
	 * @param request
	 * @return response
	 */
	private byte[] sendRequest (byte[] request) throws ConnectException {
		// The configuration should load the information about the monitor corresponding to this queue
		byte[] response = null;
		String responseString = "";
		int retry = 0;
		// If the request has failed, we retry until to reach the maxRetry
		do {
			try {
				initSocketConnection();
				globalConfigReq.send(request, 0);
				response = globalConfigReq.recv(0);
				if (response != null)
					responseString = new String(response);
				closeSocketConnection();
			} catch (IllegalStateException e) {
				try {
					logger.info("GCM not found");
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} finally {
				retry++;
			}
		} while ((response == null || responseString.equals(Integer.toString(RoQConstant.EVENT_LEAD_LOST))) 
				&& retry < maxRetry);
		if (response == null || responseString.equals(Integer.toString(RoQConstant.EVENT_LEAD_LOST)))
			throw new ConnectException("Failed to process the request @ GCM");
		return response;
	}

}
