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
package org.roqmessaging.clientlib.factory;

/**
 * Interface IRoQLogicalQueueFactory
 * <p> Description: represents the Global monitor manager. As those manager are intended to be stateless, a 
 * better design pattern is to let client manage directly the connection through the Local host thread.
 * 
 * @author sskhiri
 */
public interface IRoQLogicalQueueFactory {
	
	/**
	 * Creates a logical queue.
	 * @param queueName the queue name
	 * @param targetAddress the address to create the queue
	 * @param recoveryMod the q must be recovered
	 * @throws IllegalStateException if a queue already exist with this name, the name must be unique for 
	 * the complete cluster.
	 */
	public int createQueue(String queueName, String targetAddress, boolean recoveryMod) throws IllegalStateException;
	
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
	
	/**
	 * @param queueName the name of the logical queue
	 * @param targetAddress the target address in which we are going to create the exchange. 
	 * @return true if the creation was OK
	 */
	public boolean createExchange(String queueName, String targetAddress, String exchangeID);
	
	
	/**
	 * As the queue factory will keep a local cache of the topology, we enable to refresh the cache.
	 */
	public void refreshTopology();
	
	/**
	 * Must be called by the client to clean the local connection and local data cache.
	 */
	public void clean();

}
