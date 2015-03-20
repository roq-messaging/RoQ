package org.roqmessaging.client;

import java.net.ConnectException;

public interface IRoQQueueManagement {
	/**
	 * Creates a logical queue automatically.
	 * @param queueName the queue name
	 * @param targetAddress the address to create the queue
	 * @throws IllegalStateException if a queue already exist with this name, the name must be unique for 
	 * the complete cluster.
	 */
	public boolean createQueue(String queueName) throws IllegalStateException, ConnectException;
	
	/**
	 * Removes a logical queue.
	 * @param queueName the name of the queue to remove.
	 * @return true if we the deletion was OK.
	 */
	public boolean removeQueue(String queueName) throws IllegalStateException, ConnectException; 
	
	/**
	 * Starts an existing logical queue.
	 * @param queueName the name of the queue to start.
	 * @param host the address to create the queue
	 * @return true if we the queue was successfully started.
	 * @throws ConnectException 
	 * @throws IllegalStateException 
	 */
	public boolean startQueue(String queueName, String host) throws IllegalStateException, ConnectException;
	
	/**
	 * Stops an existing logical queue.
	 * @param queueName the name of the queue to stop.
	 * @return true if we the queue was successfully stopped.
	 */
	public boolean stopQueue(String queueName) throws IllegalStateException, ConnectException;
	
}
