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

import org.roqmessaging.client.IRoQConnection;

/**
 * Interface  IRoQConnectionFactory
 * <p> Description: factory that create the RoQConnection.
 * 
 * @author sskhiri
 */
public interface IRoQConnectionFactory {
	
	/**
	 * Instantiates a connection. Notice that the connection will need to connect to an active Exchange. At startup this 
	 * could take few seconds before beeing ready.
	 * @param monitorHost the host in which the monitor is running
	 * @param basePort the port on which the monitor has been started
	 * @return a connection that can be used to send messages.
	 */
	public IRoQConnection createRoQConnection(String monitorHost, int basePort);

}
