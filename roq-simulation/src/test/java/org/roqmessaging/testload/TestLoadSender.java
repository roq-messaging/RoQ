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
package org.roqmessaging.testload;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.loaders.SenderLoader;

/**
 * Class TestLoadTest
 * <p>
 * Description: Validate the test load components. The Junit test will simulate
 * the queue under test.
 * 
 * @author sskhiri
 */
public class TestLoadSender extends RoQTestCase {
	private Logger logger = Logger.getLogger(TestLoadSender.class);
	private int limit = 100000;
	
	/**
	 * Test the sender loader by creating one and checking the message sent.
	 */
	@Test
	public void testLoaderSender() throws Exception{
		// The queue under test
		String qName = "performance_test";
		// 1. Create the queue
		this.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP());
		// 2. Create the sender: rate, payload, GCM, q name 
		TimerTask sender = new SenderLoader(10, 1, RoQUtils.getInstance().getLocalIP(), qName);
		//3. Attach 1 subscriber
		this.attachSUbscriber(qName);
		// 4. Launch the sender
		Timer timerLoad = new Timer("Loader");
		timerLoad.schedule(sender, 2000, 1000);
		try {
			Thread.sleep(10000);
			// KIll the timer
			timerLoad.cancel();
			Thread.sleep(1000);
			this.factory.removeQueue(qName);
			this.subscriberConnection.close();
		} catch (InterruptedException e) {
			logger.debug(e);
		}
	}
	
	
	/**
	 * Attach a subscriber to the queue on the topic "key". The test must send 
	 * a java-encoded verison of "hello".
	 * @param qName the queue to attach the subs.
	 */
	protected void attachSUbscriber(String qName) {
		IRoQConnectionFactory factory = new RoQConnectionFactory(launcher.getConfigurationServer());
		// add a subscriber
		subscriberConnection = factory.createRoQSubscriberConnection(qName, "test");
		// Open the connection to the logical queue
		subscriberConnection.open();
		// Register a message listener
		IRoQSubscriber subs = new IRoQSubscriber() {
			private int count = 0;
			public void onEvent(byte[] msg) {
				count++;
				if(count>limit){
					logger.debug("Got "+ limit+"  message of "+msg.length +" byte" );
					count =0;
				}
				
			}
		};
		subscriberConnection.setMessageSubscriber(subs);
		
	}

}
