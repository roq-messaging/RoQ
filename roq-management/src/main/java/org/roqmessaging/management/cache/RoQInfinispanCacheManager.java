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
package org.roqmessaging.management.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.management.GlobalConfigurationState;
import org.zeromq.ZMQ.Socket;

/**
 * Class RoQCacheManager
 * <p> Description: Manages the logic to interface the cache. It will explode the state 
 * in order to save it through different map and arrays.
 * 
 * @author sskhiri
 */
public class RoQInfinispanCacheManager implements IRoQCacheManager {
	//the infinispan cache
	private Cache<String, Object> cache = null;
	private LoggingListener cacheListener = null;
	
	/**
	 * Init the cache
	 */
	public RoQInfinispanCacheManager() {
		try {
			this.cache = new DefaultCacheManager("roq-infinispan-clustered-tcp.xml").getCache("roqcache");
		} catch (IOException e) {
			e.printStackTrace();
		}
		cacheListener = new LoggingListener();
		cache.addListener( cacheListener);
	}

	/**
	 * @see org.roqmessaging.management.cache.IRoQCacheManager#getCachedState()
	 */
	@SuppressWarnings("unchecked")
	public GlobalConfigurationState getCachedState() {
		GlobalConfigurationState state = new GlobalConfigurationState();
		state.setHostManagerAddresses((ArrayList<String>) this.cache.get(RoQConstant.CACHE_HOST_MNGER_ADDR));
		state.setHostManagerMap((HashMap<String, Socket>) this.cache.get(RoQConstant.CACHE_HOST_MNGER_MAP));
		state.setQueueHostLocation((HashMap<String, String>) this.cache.get(RoQConstant.CACHE_Q_HOST_LOCATION));
		state.setQueueMonitorMap((HashMap<String, String>) this.cache.get(RoQConstant.CACHE_Q_MONITOR_MAP));
		state.setQueueMonitorStatMap((HashMap<String, String>) this.cache.get(RoQConstant.CACHE_Q_MONITOR_STAT_MAP));
		return state;
	}

	/**
	 * @see org.roqmessaging.management.cache.IRoQCacheManager#cacheState(org.roqmessaging.management.GlobalConfigurationState)
	 */
	public void cacheState(GlobalConfigurationState state) {
		this.cache.put(RoQConstant.CACHE_HOST_MNGER_ADDR, state.getHostManagerAddresses());
		this.cache.put(RoQConstant.CACHE_HOST_MNGER_MAP, state.getHostManagerMap());
		this.cache.put(RoQConstant.CACHE_Q_HOST_LOCATION, state.getQueueHostLocation());
		this.cache.put(RoQConstant.CACHE_Q_MONITOR_MAP, state.getQueueMonitorMap());
		this.cache.put(RoQConstant.CACHE_Q_MONITOR_STAT_MAP, state.getQueueMonitorStatMap());
	}

	/**
	 * @see org.roqmessaging.management.cache.IRoQCacheManager#stopCache()
	 */
	public void stopCache() {
		this.cache.removeListener(this.cacheListener);
		this.cache.stop();
		
	}
}
