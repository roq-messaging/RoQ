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

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roq.simulation.management.client.MngClient;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.LogicalQFactory;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;

/**
 * Class TestScalingStatMonitor
 * <p> Description: Test the scaling monitor and its stat subscription.
 * 
 * @author sskhiri
 */
public class TestScalingStatMonitor extends RoQTestCase {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private MngClient mngClient = null;
	
	@Test
	public void test() {
		try {
			logger.info("Main test Scaling process");
			
			//Init the management client
			this.mngClient = new MngClient(RoQUtils.getInstance().getLocalIP());
			
			// Let the host self register to the global configuration
			Thread.sleep(3000);
			// 1. Create a Queue
			IRoQLogicalQueueFactory logicalQFactory = new LogicalQFactory(launcher.getConfigurationServer());
			logicalQFactory.createQueue("queue1", RoQUtils.getInstance().getLocalIP());
			
			// 2.Let the Process start and binding port
			Thread.sleep(3000);

			// 3. Create a subscriber
			IRoQConnectionFactory factory = new RoQConnectionFactory(launcher.getConfigurationServer());
			// add a subscriber
			IRoQSubscriberConnection subConnection = factory.createRoQSubscriberConnection("queue1", "key");
			// Open the connection to the logical queue
			subConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					String content = new String(msg, 0, msg.length);
					assert content.startsWith("hello");
				}
			};
			subConnection.setMessageSubscriber(subs);

			// 4. Create a publisher// Add a publisher
			// Creating the connection
			IRoQConnection connection = factory.createRoQConnection("queue1");
			connection.open();
			// Creating the publisher and sending message
			IRoQPublisher publisher = connection.createPublisher();
			// Wait for the connection is established before sending the first
			// message
			connection.blockTillReady(10000);

			// 5 Sending the message
			logger.info("Sending MESSAGES ...");
			for (int i = 0; i < 500; i++) {
				publisher.sendMessage("key".getBytes(), ("hello" + i).getBytes());
			}
			
			logger.info("ON  WAIT ...");
			Thread.sleep(3000);

			// 3 Wait &. Check the content
			logger.info("Sending MESSAGES ...");
			for (int i = 0; i < 1000; i++) {
				publisher.sendMessage("key".getBytes(), ("hello" + i).getBytes());
			}
			Thread.sleep(3000);
			
			//ADD an autoscaling rule for this queue
			AutoScalingConfig cfg = createAutoscalingRules("testConfg");
			//Check that the autocaling process is notified on his pull socket
			mngClient.testAutoScaling("queue1", cfg);
			
			logger.info("ON  WAIT 2...");
			Thread.sleep(3000);

			// 3 b. Wait &. Check the content
			logger.info("Sending MESSAGES ...");
			for (int i = 0; i < 1000; i++) {
				publisher.sendMessage("key".getBytes(), ("hello V2" + i).getBytes());
			}
			Thread.sleep(3000);
			
			// End close connection
			connection.close();
			subConnection.close();
			
			//Client close
			mngClient.close();

			// Delete the queue
			logicalQFactory.removeQueue("queue1");
			Thread.sleep(2000);
			

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param cfgName the configuration name
	 * @return dummy auto scaling rule with the specified name
	 */
	private AutoScalingConfig createAutoscalingRules(String cfgName) {
		AutoScalingConfig config = new AutoScalingConfig();
		config.setName(cfgName);
		HostScalingRule hRule = new HostScalingRule(30, 40);
		config.setHostRule(hRule);
		XchangeScalingRule xRule =  new XchangeScalingRule(20000, 0);
		config.setXgRule(xRule);
		LogicalQScalingRule qRule = new LogicalQScalingRule(10000, 100000);
		config.setqRule(qRule);
		return config;
	}
}
