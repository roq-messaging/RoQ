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
package org.roqmessaging.management;

import org.roqmessaging.client.IRoQMonitorManager;

/**
 * Class GlobalMonitorManager
 * <p> Description: Responsible of the logical queue Life cycle management.
 * 
 * @author sskhiri
 */
public class GlobalMonitorManager implements IRoQMonitorManager  {

	/**
	 * 
	 * @see org.roqmessaging.client.IRoQMonitorManager#createQueue(java.lang.String, java.lang.String)
	 */
	public void createQueue(String queueName, String targetAddress) throws IllegalStateException {
		//1. Check if the name already exist in the topology
		//2. Create the entry in the global config
		//3. Sends the create event to the hostConfig manager thread
		//4. If the answer is not confirmed, we should remove back the entry in the global config and throwing an exception
	}

	/**
	 * @see org.roqmessaging.client.IRoQMonitorManager#removeQueue(java.lang.String)
	 */
	public boolean removeQueue(String queueName) {
		//1. Get the monitor address
		//2. Remove the entry in the global configuration
		//3. Send the remove message to the monitor
		return true;
	}

}
