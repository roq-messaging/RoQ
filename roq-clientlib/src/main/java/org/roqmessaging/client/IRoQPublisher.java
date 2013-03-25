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
 * Interface RoQPublisher
 * <p> Description: Client API that client must know to send a message. 
 * 
 * @author sskhiri
 */
public interface IRoQPublisher {
	
	/**
	 * @param key the message key
	 * @param msg the message to send
	 * @throws IllegalStateException thrown if the configuration state is not valid when sending the message.
	 * @return true if the configuration is valid
	 */
	public boolean sendMessage(byte[] key , byte[] msg ) throws IllegalStateException;
	
	/**
	 * @param add define whether we need to add a time stamp as a multi part message:
	 * [key, payload, timestamp]
	 */
	public void addTimeStamp(boolean add);

}
