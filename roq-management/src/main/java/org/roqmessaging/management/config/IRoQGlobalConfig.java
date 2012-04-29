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
package org.roqmessaging.management.config;

/**
 * Interface IRoQGlobalConfig
 * <p> Description: This configuration manager keeps the track of all queue configuration among the cluster. It tracks
 *  for each queue: the monitor address.
 * 
 * @author sskhiri
 */
public interface IRoQGlobalConfig {
	
	/**
	 * Add a logical information to the global configuration.
	 * @param queueName the name of the logical queue
	 * @param monitorAddress the address of the monitor thread running
	 * @param monitorPort the port of the monitor, as we can have multiple logical queues started on the same host.
	 * @return true if the creation process was OK.
	 */
	public boolean addNewLogicalQueue(String queueName, String monitorAddress, String monitorPort);
	
	/**
	 * Checks whether this name already exist in the whole topology.
	 * @param queueName the name of the logical queue
	 * @return true is already exist
	 */
	public boolean doesExistLogicalQueue(String queueName);

}
