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
package org.roq.simulation;

import java.net.ConnectException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.LogicalQFactory;

/**
 * Class TestClientAPI
 * <p> Description: This test must be manually launched only when the {@link RoQAllLocalLauncher} has 
 * been started before in command line as:
 * 	java -Djava.library.path=/usr/local/lib -cp roq-simulation-1.0-SNAPSHOT-jar-with-dependencies.jar org.roq.simulation.RoQAllLocalLauncher
 * 
 * @author sskhiri
 */
@Ignore
public class TestClientAPI {
	private LogicalQFactory factory = null;
	private Logger logger = Logger.getLogger(TestClientAPI.class);

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.factory = new LogicalQFactory(RoQUtils.getInstance().getLocalIP().toString());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.factory.clean();
	}

	@Test
	public void test() {
		try {
			// TODO CANNOT WORK, NEED A ZK SERVER...
			// 1. Create Q
			 this.factory.createQueue("queue1", RoQUtils.getInstance().getLocalIP().toString(), new ArrayList<String>(), false);
			// 2. Create subscriber
			IRoQConnectionFactory connectionFactory = new RoQConnectionFactory(RoQUtils.getInstance().getLocalIP()
					.toString());
			IRoQSubscriberConnection subscriberConnection = connectionFactory.createRoQSubscriberConnection("queue1",
					"key");
			subscriberConnection.open();
			subscriberConnection.setMessageSubscriber(new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					logger.info("On message:" + new String(msg));
				}
			});
			// 3. Create publisher
			IRoQConnection pubConnection = connectionFactory.createRoQConnection("queue1");
			pubConnection.open();
			IRoQPublisher publisher = pubConnection.createPublisher();
			pubConnection.blockTillReady(10000);
			publisher.sendMessage("key".getBytes(),"Hello test 1".getBytes());
			Thread.sleep(1000);
			//4.  Clean all
			pubConnection.close();
			subscriberConnection.close();
			this.factory.removeQueue("queue1");
			// 5. Wait
			Thread.sleep(2000);
		} catch (InterruptedException | ConnectException | IllegalStateException e) {
			logger.error(e);
		}
	}

}
