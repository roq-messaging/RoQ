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
package org.roqmessaging.management.infinispan;

import org.junit.Ignore;
import org.roqmessaging.management.GlobalConfigurationState;
import org.roqmessaging.management.cache.IRoQCacheManager;
import org.roqmessaging.management.cache.RoQInfinispanCacheManager;

/**
 * Class TestCacheProcess
 * <p> Description: Test the cache on two different processes.
 * 
 * @author sskhiri
 */
@Ignore
public class TestCacheProcess {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			// Init state
			GlobalConfigurationState stateDAO = new GlobalConfigurationState();
			stateDAO.getQueueHostLocation().put("queue1", "127.0.0.1");
			stateDAO.getQueueHostLocation().put("queue2", "127.0.0.1");
			stateDAO.getHostManagerAddresses().add("127.0.0.1");
			stateDAO.getHostManagerAddresses().add("127.0.0.2");
			stateDAO.getHostManagerMap().put("queue1", null);
			stateDAO.getQueueMonitorMap().put("queue1", "tcp://127.0.0.1:5050");
			stateDAO.getQueueMonitorMap().put("queue2", "tcp://127.0.0.1:5052");
			stateDAO.getQueueMonitorStatMap().put("queue1", "tcp://127.0.0.1:5055");
			stateDAO.getQueueMonitorStatMap().put("queue1", "tcp://127.0.0.1:5058");

			// Cache 1
			IRoQCacheManager cacheManager = new RoQInfinispanCacheManager();

			// Store in cache
			cacheManager.cacheState(stateDAO);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
