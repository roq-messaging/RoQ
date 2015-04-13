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
package org.roqmessaging.testload;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.loaders.SenderLoader;

/**
 * Class Test117a
 * <p>
 * Description: Test the first scenario defined in the Issue #117 for checking
 * the static behavior. ## Test 1: starting a producer after the cold start
 * condition 1. Starting a test load with 2 Publishers and 3 Subscribers. The
 * load must be > 10% than the MAX exchange limit in order to overcome the cold
 * start condition. <br>
 * 2. After 1 min, when we are sure that the cold start condition is not
 * applicable : we create an exchange and we start a new producer and we check
 * the allocation.
 * 
 * @author sskhiri
 */
public class Test117a extends RoQTestCase {
	// The logger
	private Logger logger = Logger.getLogger(Test117a.class);

	@Test
	public void test117a() throws InterruptedException {
		// The Qname
		String qName = "test117-Q";
		// Init 1. create the test queue
		super.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP(), new ArrayList<String>(), false);
		// Init 2. let the queue start
		Thread.sleep(2000);
		//3. Attach a subscriber
		createSubscriber(qName);
		// The limit is 82.5 Mi byte/min
		// 10% of the limit is 8.25Mi byte/min => 137k byte/sec = 140 kb/s
		// With a rate of 15000 msg/sec of 10 byte we can reach the limit
		// 3. Start the test
		Timer timerLoad = new Timer("Loader Publisher");
		SenderLoader loader1 = new SenderLoader(15000, 10, launcher.getZkServerAddress(), qName);
		SenderLoader loader2 = new SenderLoader(15000, 10, launcher.getZkServerAddress(), qName);
		logger.info("TEST 117-------------------------------------Start Test--------------------------");
		timerLoad.schedule(loader1, 50, 1000);
		timerLoad.schedule(loader2, 50, 1000);
		// Wait for the the second part of the test
		Thread.sleep(100000);
		logger.info("TEST 117-------------------------------------Starting The exchange----------------------------");
		// create an exchange
		super.factory.createExchange(qName, RoQUtils.getInstance().getLocalIP(), "TEST5");
		Thread.sleep(30000);
		// Start a producer
		logger.info("TEST 117-------------------------------------Starting The producer----------------------------");
		SenderLoader loader3 = new SenderLoader(100, 5,launcher.getZkServerAddress(), qName);
		// Schedule it, the run will be called soon
		timerLoad.schedule(loader3, 50, 1000);
		Thread.sleep(30000);
		//Close all
		timerLoad.cancel();
		super.factory.removeQueue(qName);
		if(super.subscriberConnection!=null) super.subscriberConnection.close();
		loader1.shutDown();
		loader2.shutDown();
		loader3.shutDown();

	}
	
/**
 * Create a subscriber to this queue
 */
private void createSubscriber(String qName) {
	IRoQConnectionFactory conFactory = new RoQConnectionFactory(launcher.getZkServerAddress());
	try {
		subscriberConnection = conFactory.createRoQSubscriberConnection(qName, "test");
	
		// Open the connection to the logical queue
		subscriberConnection.open();
		// Register a message listener
		IRoQSubscriber subs = new IRoQSubscriber() {
			private int count = 0;
			public void onEvent(byte[] msg) {
				count++;
				if(count>300000){
					logger.info("Got "+300000+" message");
					count =0;
				}
			}
		};
		//Se the subscriber logic for this connection
		subscriberConnection.setMessageSubscriber(subs);
	} catch (ConnectException | IllegalStateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

}
