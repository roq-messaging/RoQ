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
package org.roqmessaging.clientlib.factory;

import java.net.ConnectException;

import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQSubscriberConnection;

/**
 * Interface  IRoQConnectionFactory
 * <p> Description: factory that create the RoQConnection.
 * 
 * @author sskhiri
 */
public interface IRoQConnectionFactory {
	
	/**
	 * Instantiates a connection. Notice that the connection will need to connect to an active Exchange. At startup this 
	 * could take few seconds before beefing ready.
	 * @param qn the name of the logical queue
	 * @throws ConnectException 
	 * @return a connection that can be used to send messages.
	 */
	public IRoQConnection createRoQConnection(String qName) throws IllegalStateException, ConnectException;
	
	/**
	 * Instantiates a connection. Notice that the connection will need to connect to an active Exchange. At startup this 
	 * could take few seconds before being ready.
	 * @param qName the queue logical name to bind
	 * @param key the subscription key
	 * @return a connection that can be used to receive  messages.
	 * @throws ConnectException 
	 */
	public IRoQSubscriberConnection createRoQSubscriberConnection(String qName, String key) throws IllegalStateException, ConnectException;
	
	/**
	 * close ZK
	 */
	public void close();

}
