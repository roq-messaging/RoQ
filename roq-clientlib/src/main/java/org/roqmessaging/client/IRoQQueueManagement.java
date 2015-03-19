package org.roqmessaging.client;

public interface IRoQQueueManagement {
	/**
	 * Creates a logical queue automatically.
	 * @param queueName the queue name
	 * @param targetAddress the address to create the queue
	 * @throws IllegalStateException if a queue already exist with this name, the name must be unique for 
	 * the complete cluster.
	 */
	public boolean createQueue(String queueName) throws IllegalStateException;
	
	/**
	 * Removes a logical queue.
	 * @param queueName the name of the queue to remove.
	 * @return true if we the deletion was OK.
	 */
	public boolean removeQueue(String queueName); 
	
	/**
	 * Starts an existing logical queue.
	 * @param queueName the name of the queue to start.
	 * @param host the address to create the queue
	 * @return true if we the queue was successfully started.
	 */
	public boolean startQueue(String queueName, String host);
	
	/**
	 * Stops an existing logical queue.
	 * @param queueName the name of the queue to stop.
	 * @return true if we the queue was successfully stopped.
	 */
	public boolean stopQueue(String queueName);
	
}
