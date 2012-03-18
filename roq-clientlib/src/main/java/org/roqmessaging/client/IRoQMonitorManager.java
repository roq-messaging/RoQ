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
package org.roqmessaging.client;

/**
 * Interface IRoQMonitorManager
 * <p> Description: responsible to manage all the monitors of each logical queue. It also exposes a provisioning
 *  API to create new logical queues.
 * 
 * @author sskhiri
 */
public interface IRoQMonitorManager {
	
	/**
	 * Creates a logical queue.
	 * @param queueName the queue name
	 * @param targetAddress the address to create the queue
	 * @throws IllegalStateException if a queue already exist with this name, the name must be unique for 
	 * the complete cluster.
	 */
	public void createQueue(String queueName, String targetAddress) throws IllegalStateException;
	
	/**
	 * Removes a logical queue.
	 * @param queueName the name of the queue to remove.
	 * @return true if we the deletion was OK.
	 */
	public boolean removeQueue(String queueName);

}
