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
package org.roqmessaging.core.factory;

import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.RoQPublisherConnection;

/**
 * Class RoQConnectionFactory
 * <p> Description: Create the connection object that will manage the connection between client and exchanges.
 * 
 * @author sskhiri
 */
public class RoQConnectionFactory implements IRoQConnectionFactory {

	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQConnectionFactory#createRoQConnection()
	 */
	public IRoQConnection createRoQConnection() {
		return new RoQPublisherConnection();
	}

}
