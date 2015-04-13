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
import java.util.List;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.loaders.SenderLoader;

/**
 * Class Test117b
 * <p> Description: tesy the issue #117 use case b:
 * <br>
 * ## Test 2: Dynamic allocation
 * 1. Starting a test load with 4 Publishers (x, y, z, w) and 3 Subscribers.
 *  The rate of X, Y, Z are 20 % (each) of the rate of W. After 2 mins 
 *  the total throughput in b/min must be > than the exchange limit and should triger a re-balance of W.
 *  2. Starting a new Exchange after 1 min. Without any publisher connected.
 *  3. We should observe the migration of the publisher w to exchange 2. 
 *  If Rate x is denoted Rx then,
 *  <br> Rw =x, Rx= 0.2 x, Ry= 0.2x, Rz= 0.2x
 *  <br> Rtot = 1.6x in kb/s
 *  <br> The max limit is 82,5Mib/min=> 1375kb/sec
 *  <br> Rtot > 1375 kb/sec => 1.6x kb/s > 1400kb/s => x >875
 *  <br> With 2 subscriber the load is multiplied by 2, then, we only need x >437.5
 *  
 *  <p> Total load definition for the test case:
 *  Size of message 10b
 *  Rw =44.000, Rx= 8.800, Ry= 8.800, Rz= 8.800
 *   * 
 * @author sskhiri
 */
public class Test117b extends RoQTestCase {
	// The logger
	private Logger logger = Logger.getLogger(Test117b.class);
	//Connection list to remove at the end of the test
	private List<IRoQSubscriberConnection> connectionList = new ArrayList<IRoQSubscriberConnection>();

	@Test
	public void test117b() throws Exception{
		// The Qname
		String qName = "test117b-Q";
		// Init 1. create the test queue
		super.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP(), new ArrayList<String>(), false);
		// Init 2. let the queue start
		Thread.sleep(2000);
		//3. Attach 2 subscriber
		createSubscriber(qName);
		createSubscriber(qName);
		//Adding 1 to be sure we are out of the limit
		createSubscriber(qName);
		// 3. Start the test
		Timer timerLoad = new Timer("Loader Publisher");
		SenderLoader loader1 = new SenderLoader(44000, 100, launcher.getZkServerAddress(), qName);
		SenderLoader loader2 = new SenderLoader(8800, 100, launcher.getZkServerAddress(), qName);
		SenderLoader loader3 = new SenderLoader(8800, 100, launcher.getZkServerAddress(), qName);
		SenderLoader loader4 = new SenderLoader(8800, 100, launcher.getZkServerAddress(), qName);
		logger.info("TEST 117b-------------------------------------Start Test--------------------------");
		timerLoad.schedule(loader1, 50, 1000);
		timerLoad.schedule(loader2, 50, 1000);
		timerLoad.schedule(loader3, 50, 1000);
		timerLoad.schedule(loader4, 50, 1000);
		// Wait for the the second part of the test
		Thread.sleep(1000);
		logger.info("TEST 117b-------------------------------------Starting The exchange----------------------------");
		logger.info("TEST 117b: waiting for relocation...");
		// create an exchange
		super.factory.createExchange(qName, RoQUtils.getInstance().getLocalIP(), "TEST6");
		Thread.sleep(120000);
		// Start a producer
		//Close all
		timerLoad.cancel();
		super.factory.removeQueue(qName);
		for (IRoQSubscriberConnection con_i : this.connectionList) {
			con_i.close();
		}
		loader1.shutDown();
		loader2.shutDown();
		loader3.shutDown();
		loader4.shutDown();
		Thread.sleep(2000);
	}
	
/**
 * Create a subscriber to this queue
 */
private void createSubscriber(String qName) {
	IRoQConnectionFactory conFactory = new RoQConnectionFactory(launcher.getZkServerAddress());
	IRoQSubscriberConnection subscriberConnection;
	try {
		subscriberConnection = conFactory.createRoQSubscriberConnection(qName, "test");
	
		connectionList.add(subscriberConnection);
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
		// The subscriber logic for this connection
		subscriberConnection.setMessageSubscriber(subs);
	} catch (ConnectException | IllegalStateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

}
