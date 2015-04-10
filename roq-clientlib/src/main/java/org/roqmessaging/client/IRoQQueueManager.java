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
Interface IRoQQueueManager
 * <p> Description: Client API that client must know to manage queues. 
 * 
 * @author bvanmelle
 */
public interface IRoQQueueManager {
	
	public void createQueue(String queueName) throws IllegalStateException;
	
	public void removeQueue(String queueName) throws IllegalStateException;
	
	public void startQueue(String queueName) throws IllegalStateException;
	
	public void stopQueue(String queueName) throws IllegalStateException;
}
