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

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
 
/**
 * An Infinispan listener that simply logs cache entries being created and
 * removed
 * 
 * @author Pete Muir
 */
@Listener
public class LoggingListener {
 
   private Log log = LogFactory.getLog(LoggingListener.class);
 
   @CacheEntryCreated
   public void observeAdd(CacheEntryCreatedEvent<?, ?> event) {
      if (!event.isPre()) // So that message is only logged after operation succeeded
         log.infof("Cache entry with key %s added in cache %s", event.getKey(), event.getCache());
   }
 
   @CacheEntryRemoved
   public void observeRemove(CacheEntryRemovedEvent<?, ?> event) {
      log.infof("Cache entry with key %s removed in cache %s", event.getKey(), event.getCache());
   }
 
}