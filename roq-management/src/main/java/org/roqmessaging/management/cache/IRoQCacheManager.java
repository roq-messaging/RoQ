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

import org.roqmessaging.management.GlobalConfigurationState;

/**
 * Interface IRoQCacheManager
 * <p> Description: interface managing the integration with the underlying cache
 * for managing the state of the {@linkplain GlobalConfigurationState}
 * 
 * @author sskhiri
 */
public interface IRoQCacheManager {
	
	/**
	 * @return the global configuration state build up from cached objects.
	 */
	public GlobalConfigurationState getCachedState();
	
	/**
	 * Cache the configurations state.
	 * @param state the state to cache
	 */
	public void cacheState(GlobalConfigurationState state);
	
	/**
	 * Stop and clean the current cache.
	 */
	public void stopCache();

}
