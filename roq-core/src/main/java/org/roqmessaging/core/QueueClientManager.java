/**
 * Copyright 2012 EURANOVA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.roqmessaging.core;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQQueueManager;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * Class QueueManagerClient
 * <p> Description: Implementation of the queueManager client library. 
 * <br> Allows to send queue creation/destruction messages to
 * the management controller.
 * 
 * @author bvanmelle
 */
public class QueueClientManager implements IRoQQueueManager {
	//The global config server address 
	private String configServer = null;
	// ZMQ config
	private Context context= null;
	//The socket to the global config
	private Socket mngmtControllerReq;
	
	private Logger logger = Logger.getLogger(QueueClientManager.class);
	
	/**
	 * Build  a queue Manager and takes the location of the global configuration manager as input
	 * @param configManager the config manager IP address
	 */
	public QueueClientManager(String configManager) {
		this.configServer= configManager;
	}

	@Override
	/**
	 * Create a new logical queue on the cluster
	 * @param the name of the queue to create
	 */
	public void createQueue(String queueName)
			throws IllegalStateException {
		// Open socket and init BSON request
		initSocketConnection();
		BSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_CREATE_QUEUE_AUTOMATICALLY);
		// Send request
		mngmtControllerReq.send(BSON.encode(request), 0);
		// Get Response
		BSONObject result = BSON.decode(mngmtControllerReq.recv(0));
		// Close socket
		closeSocketConnection();
		if ((Integer)result.get("RESULT") ==  RoQConstant.FAIL) {
			logger.error("The queue creation for  " + queueName
					+ " failed. Reason: " + ((String) result.get("COMMENT")));
			throw  new IllegalStateException("The queue removal process failed @ Management Controller");
		}
		else {
			logger.info("Queue " + queueName + " has been created");
		}
	}

	@Override
	/**
	 * Remove a queue from the cluster
	 * @param the name of the queue to remove
	 */
	public void removeQueue(String queueName) 
			throws IllegalStateException {
		// Open socket and init BSON request
		initSocketConnection();
		BasicBSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_REMOVE_QUEUE);
		// Send request
		mngmtControllerReq.send(BSON.encode(request), 0);
		// Get Response
		byte[] responseBytes = mngmtControllerReq.recv(0);
		int response = Integer.parseInt(new String(responseBytes));
		closeSocketConnection();
		if(response == RoQConstant.FAIL){
			throw  new IllegalStateException("The queue removal process failed @ Management Controller");
		} else {
			logger.info("Queue has been removed @ Management Controller");
		}		
	}
	
	@Override
	/**
	 * TODO: Actually BSON ask for a host to perform
	 * this action.. It probably must be able to
	 * find the host via the Qname
	 */
	public void startQueue(String queueName) 
			throws IllegalStateException {
		// Open socket and init BSON request
		initSocketConnection();
		BasicBSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_START_QUEUE);
		// Send request
		mngmtControllerReq.send(BSON.encode(request), 0);
		// Get Response
		byte[] responseBytes = mngmtControllerReq.recv(0);
		int response = Integer.parseInt(new String(responseBytes));
		closeSocketConnection();
		if(response == RoQConstant.FAIL){
			throw  new IllegalStateException("The queue start process failed @ Management Controller");
		} else {
			logger.info("Queue has been started @ Management Controller");
		}
	}

	@Override
	/**
	 * Stop a running Queue
	 * @param the name of the queue to stop
	 */
	public void stopQueue(String queueName) 
			throws IllegalStateException {
		// Open socket and init BSON request
		initSocketConnection();
		BasicBSONObject request = new BasicBSONObject();
		request.put("QName", queueName);
		request.put("CMD", RoQConstant.BSON_CONFIG_STOP_QUEUE);
		// Send request
		mngmtControllerReq.send(BSON.encode(request), 0);
		// Get Response
		byte[] responseBytes = mngmtControllerReq.recv(0);
		int response = Integer.parseInt(new String(responseBytes));
		closeSocketConnection();
		if(response == RoQConstant.FAIL){
			throw  new IllegalStateException("The queue stop process failed @ Management Controller");
		} else {
			logger.info("Queue has been stopped @ Management Controller");
		}		
	}
	
	/**
	 * Removes the socket connection to the global configuration manager
	 */
	private void closeSocketConnection() {
		this.logger.debug("Closing factory socket");
		this.mngmtControllerReq.setLinger(0);
		this.mngmtControllerReq.close();
		
	}

	/**
	 * Initializes the socket connection.
	 */
	private void initSocketConnection() {
		context = ZMQ.context(1);
		mngmtControllerReq = context.socket(ZMQ.REQ);
		mngmtControllerReq.connect("tcp://"+this.configServer+":5003");
		
	}

}
