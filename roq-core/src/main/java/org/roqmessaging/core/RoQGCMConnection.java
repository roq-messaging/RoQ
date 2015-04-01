package org.roqmessaging.core;

import java.net.ConnectException;

import org.apache.log4j.Logger;
import org.apache.zookeeper.server.Request;
import org.roqmessaging.zookeeper.RoQZooKeeper;
import org.zeromq.ZMQ;

/***
 * This class is used to request the GCM by using ZK
 * When a request it sends, ZK is contacted first 
 * in order to get the address of the leader
 * @author benjamin
 *
 */
public class RoQGCMConnection {
	private Logger logger = Logger.getLogger(RoQGCMConnection.class);
	// ZMQ
	private ZMQ.Socket requestSocket = null; 
	private ZMQ.Context context;
	// Retry & timeout
	private int maxRetry;
	private int timeout;
	// Zookeeper client
	private RoQZooKeeper zkClient;
	
	/**
	 * Allows to force request shutdown
	 */
	public volatile boolean active = true;
	
	/**
	 * @param zkClient, a ZooKeeper instance (started if possible)
	 * @param maxRetry, the number of retry for a given request before failing
	 * @param timeout the max time to receive the response of the request
	 */
	public RoQGCMConnection(RoQZooKeeper zkClient, int maxRetry, int timeout) {
		this.maxRetry = maxRetry;
		this.timeout = timeout;
		this.zkClient = zkClient;
	}
	
	/**
	 * Removes the socket connection to the global config manager
	 */
	private void closeSocketConnection() {
		this.logger.debug("Closing factory socket");
		this.requestSocket.setLinger(0);
		this.requestSocket.close();
	}

	/**
	 * Initialise the socket connection.
	 */
	private void initSocketConnection(int port) 
			throws IllegalStateException {
		// Get the active master address
		String currentGCM = zkClient.getGCMLeaderAddress();
		if (currentGCM == null) 
			throw new IllegalStateException("GCM node not found");
		logger.info("Active GCM address: " + currentGCM);
		context = ZMQ.context(1);
		this.requestSocket = context.socket(ZMQ.REQ);
		this.requestSocket.connect("tcp://"+currentGCM+":"+port);
		this.requestSocket.setReceiveTimeOut(timeout);
	}

	/**
	 * This method sends a request to the GCM.
	 * First it gets the active GCM, then it sends the request
	 * If the request fails because leader has changed or the connection
	 * cannot be established (timeout), the request is resent up to maxRetry times.
	 * @param request
	 * @return response
	 * @throws ConnectException 
	 */
	public byte[]  sendRequest (byte[] request, int port) throws ConnectException {
		// The configuration should load the information about the monitor corresponding to this queue
		byte[] response = null;
		String responseString = "";
		int retry = 0;
		// If the request has failed, we retry until to reach the maxRetry
		do {
			try {
				if (retry > 0) {
					logger.info("GCM not found");
					Thread.sleep(3000); // Wait between two requests
				}
				//if (requestSocket == null)
					initSocketConnection(port);
				this.requestSocket.send(request, 0);
				response = this.requestSocket.recv(0);
				if (response != null) {
					responseString = new String(response);
				}
			} catch (Exception e1) {
				logger.info("Zk failed to return the GCM Address");
			} finally {
				//if ((response == null || responseString.equals(Integer.toString(RoQConstant.EVENT_LEAD_LOST))) && this.requestSocket != null) {
					closeSocketConnection();
					this.requestSocket = null;
				//}
				retry++;
			}
		} while (((response == null || responseString.equals(Integer.toString(RoQConstant.EVENT_LEAD_LOST))) 
				&& retry < maxRetry) && this.active);
		if (this.active && (response == null || responseString.equals(Integer.toString(RoQConstant.EVENT_LEAD_LOST))))
			throw new ConnectException("Failed to process the request @ GCM");
		return response;
	}
	
}
