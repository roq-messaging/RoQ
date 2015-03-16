/**
 * Copyright 2013 EURANOVA
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
package org.roqmessaging.management.server;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roq.simulation.management.client.MngClient;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQPublisher;

/**
 * Class TestStartStopQ
 * <p> Description: Test the lifecycle of logical queue.
 * 
 * @author sskhiri
 */
public class TestStartStopQ extends RoQTestCase {

	/**
	 * Test the BSON interface exposed by the management controller
	 * @throws Exception
	 */
	@Test
	public void testBsonRequest() throws Exception {
		Logger logger = Logger.getLogger(TestStartStopQ.class);
		try {
			
		//1. define the queue
		String qName = "testQ1";
		
		//2.  Create a client management & sending command request
		MngClient client = new MngClient("localhost");
		client.testCreate(qName);
		Thread.sleep(2000);
		
		//3. Test the message sending
		attachSUbscriber(qName);
		IRoQPublisher publisher = attachPublisher(qName);
		logger.debug("Send message");
		sendMsg(publisher);
		
		//4. Remove the queue
		logger.debug("Debug Queue");
		client.testRemove(qName);
		
		//Phase 2 Test the start/stop
		qName = "testQ2";
		//1.  Create a queue
		logger.debug("Create queue");
		client.testCreate(qName);
		
		//2. Stop the queue
		client.testStop(qName);
		
		//3. Restart the queue
		client.testStart(qName);
		
		//4. Remove the queue
		client.testRemove(qName);
		
		//Phase 3 Test the automatic queues
		qName = "testQ3";
		//1.  Create a queue
		logger.debug("Create queue");
		client.testCreateAuto(qName);
		
		//2. Stop the queue
		client.testStop(qName);
		
		//3. Restart the queue
		client.testStart(qName);
		
		//4. Remove the queue
		client.testRemove(qName);
		
		
		
		//5. Test the get cloud property API
		client.testCloudPropertiesAPI();
		
		//6. Close client
		client.close();
		
		} catch (Exception e) {
			logger.error("Error when testing BSON request ", e);
		}
				
	}

}
