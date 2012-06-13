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

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.roqmessaging.management.GlobalConfigurationState;
import org.roqmessaging.management.cache.IRoQCacheManager;
import org.roqmessaging.management.cache.RoQInfinispanCacheManager;

/**
 * Class TestDistributedCache
 * <p>
 * Description: Test the infinispan cache for the GCM.
 * 
 * @author sskhiri
 */
//@Ignore
public class TestDistributedCache {

	/**
	 * Basic cache tests.
	 */
	@Test
	public void test() {
		GlobalConfigurationState stateDAO = new GlobalConfigurationState();
		stateDAO.getQueueHostLocation().put("queue1", "127.0.0.1");
		stateDAO.getQueueHostLocation().put("queue2", "127.0.0.1");
		Cache<String, GlobalConfigurationState> cache = new DefaultCacheManager().getCache();
		// Add a entry
		cache.put("State", stateDAO);
		// Validate the entry is now in the cache
		Assert.assertEquals(1, cache.size());
		Assert.assertTrue(cache.containsKey("State"));
		// Remove the entry from the cache
		GlobalConfigurationState v = cache.remove("State");
		// Validate the entry is no longer in the cache
		Assert.assertEquals(stateDAO.getQueueHostLocation().get("queue1"),v.getQueueHostLocation().get("queue1"));
	}
	
	/**
	 * Test the RoQ cache with infinispan
	 * @throws Exception
	 */
	@Test
	public void testRoQCache() throws Exception {
		//Init state
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
		
		//Cache 1
		IRoQCacheManager cacheManager = new RoQInfinispanCacheManager();
		//Cache 2
		IRoQCacheManager cacheManager2 = new RoQInfinispanCacheManager();
		
		cacheManager.cacheState(stateDAO);
		Thread.sleep(16000);
		
		//Get the state
		GlobalConfigurationState cached = cacheManager2.getCachedState();
		Assert.assertEquals(stateDAO.getQueueHostLocation().get("queue1"),cached.getQueueHostLocation().get("queue1"));
		Assert.assertEquals(stateDAO.getHostManagerAddresses().get(0),cached.getHostManagerAddresses().get(0));
		Assert.assertEquals(stateDAO.getHostManagerMap().get("queue1"), cached.getHostManagerMap().get("queue1") );
		Assert.assertEquals(stateDAO.getQueueMonitorMap().get("queue1"), cached.getQueueMonitorMap().get("queue1") );
		Assert.assertEquals(stateDAO.getQueueMonitorStatMap().get("queue1"), cached.getQueueMonitorStatMap().get("queue1") );
		
	}

}
