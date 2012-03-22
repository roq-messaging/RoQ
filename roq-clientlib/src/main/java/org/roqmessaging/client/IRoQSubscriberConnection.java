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
 * Class IRoQSubscriberConnection
 * <p> Description: interface giving access to the logical by the subscriber side. We need to maintain a 
 * connection with the queue because new exchange can appear or existing one can disappear. Then,
 * we need to maintain a connection thread.
 * 
 * @author Sabri Skhiri
 */
public interface IRoQSubscriberConnection {
	
	/**
	 * Open the connection and launch a connection management thread.
	 */
	public void open();
	
	/**
	 * Close the connection by stopping the connection management thread.
	 */
	public void close() throws IllegalStateException;
	
	/**
	 * Add a subscriber to the internal list.
	 * @param subscriber the message subscriber.
	 */
	public void setMessageSubscriber(IRoQSubscriber subscriber) throws IllegalStateException;
	

}
