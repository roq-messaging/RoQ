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
package org.roqmessaging.loaders;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.management.LogicalQFactory;

/**
 * Class TestLoadController
 * <p> Description: This object is responsible for managing a test load. It will configure the test conditions 
 * according to the {@linkplain TestLoaderDecription}.
 * 
 * @author sskhiri
 */
public class TestLoadController {
	//The load description
	private TestLoaderDecription testDesc = null;
	//The queue factory
	private LogicalQFactory factory = null;
	//The Qname
	private String qName = "performance-test";
	//The GCM address
	private String gcmAddress = null;
	//handles on subscriber we created
	private List<IRoQSubscriberConnection> subscriberConnections = null;
	//The logger
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
	
	/**
	 * @param description the test load description
	 * @param gcmAddress the GCM address to connect
	 * @param queueHost the host address on which we need to create a queue
	 */
	public  TestLoadController(TestLoaderDecription description, String gcmAddress, String queueHost){
		this.testDesc = description;
		this.gcmAddress = gcmAddress;
		this.subscriberConnections = new ArrayList<IRoQSubscriberConnection>();
		//Init 0: create the logical Q factory
		this.factory = new LogicalQFactory(gcmAddress);
		//Init 1. create the test queue
		this.factory.createQueue(this.qName, queueHost);
	}
	
	/**
	 * initializes the test load with the test description
	 */
	public void start() {
		//0. Wait for the delay before starting (delay in second)
		try {
			Thread.sleep(this.testDesc.getDelay()*1000);
		//1. Creates the y subscriber
			attachSubscriber(this.testDesc.getMaxSub());
		//2. TODO According to the spawn rate, it creates the x senders
		//3. TODO  Wait for the test duration 
		} catch (InterruptedException e) {
			logger.error("Error when starting the load test",e);
		}
	}

	/**
	 * Create a certain number of subscribers and maintains a handle on the connection.
	 * @param maxSub the number of subscriber to create
	 */
	private void attachSubscriber(int maxSub) {
		IRoQConnectionFactory factory = new RoQConnectionFactory(this.gcmAddress);
		for (int i = 0; i < maxSub; i++) {
			IRoQSubscriberConnection subscriberConnection = factory.createRoQSubscriberConnection(qName, "test");
			// Open the connection to the logical queue
			subscriberConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				private int count = 0;
				public void onEvent(byte[] msg) {
					count++;
					if(count>1000){
						logger.debug("Got 1000 message of "+msg.length +" byte" );
						count =0;
					}
				}
			};
			subscriberConnection.setMessageSubscriber(subs);
		}
	}

}
