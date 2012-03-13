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

import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQSubscriberConnectionFactory;
import org.roqmessaging.core.RoQSubscriberConnection;

/**
 * Class RoQSubscriberConnectionFactory
 * <p> Description: implementation of the factory. Create and instanciate an subscriber connection.
 * 
 * @author sskhiri
 */
public class RoQSubscriberConnectionFactory implements IRoQSubscriberConnectionFactory {

	/**
	 * Create the Subscriber connection with the key
	 * @see org.roqmessaging.clientlib.factory.IRoQSubscriberConnectionFactory#createRoQConnection()
	 */
	public IRoQSubscriberConnection createRoQConnection(String key) {
		return new RoQSubscriberConnection(key, 0);
	}

}
