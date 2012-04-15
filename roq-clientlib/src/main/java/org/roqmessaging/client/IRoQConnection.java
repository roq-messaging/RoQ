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
 * Interface IRoQConnection
 * <p> Description: represents the connection between a publisher or listener and the logical queue.
 * 
 * @author sskhiri
 */
public interface IRoQConnection {
	
	/**
	 * Opens the connection. This method will launch the connection configuration management thread.
	 */
	public void open();
	
	/**
	 * Closes the connection by stopping all configuration managment thread.
	 */
	public void close();
	
	/**
	 * @return a publisher using the current connection to send messages.
	 */
	public IRoQPublisher  createPublisher() throws IllegalStateException;
	
	/**
	 * @return true if the connection is ready
	 */
	public boolean isReady() throws IllegalStateException ;

	/**
	 * This method blocks till the connection is ready, if not it returns false.
	 * @param timeOut
	 * @return true if the connection is ready, false if it goes on time out.
	 */
	public boolean blockTillReady(int timeOut);

}
