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
package org.roqmessaging.management.server;

import java.sql.SQLException;
import java.util.ArrayList;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roq.simulation.management.client.MngClient;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;
import org.roqmessaging.management.server.state.QueueManagementState;
import org.zeromq.ZMQ;

/**
 * Class TestMngtController
 * <p> Description: This test will cover the functional tests of the management server
 * 
 * @author sskhiri
 */
public class TestMngtController extends RoQTestCase {
	private Logger logger = Logger.getLogger(TestMngtController.class);
	private MngtController mngtController = null;

	@Test
	public void test() {
		try {
			this.mngtController = this.launcher.getMngtController();
			logger.info("---> Test 1 create  2 Qs");
			// 1. Create Q
			 this.factory.createQueue("queue1", RoQUtils.getInstance().getLocalIP().toString());
			 this.factory.createQueue("queueTest", RoQUtils.getInstance().getLocalIP().toString());
			
			 //2. Start the test class
			MngtSubscriber subscriber = new MngtSubscriber();
			new Thread(subscriber).start();
			
			//3. Sleep for test
			Thread.sleep(5000);
			
			//4. Test queues
			logger.info("---> Test 2 check  2 Qs");
			ArrayList<QueueManagementState> queues = mngtController.getStorage().getQueues();
			Assert.assertEquals(2, queues.size());
			
			logger.info("---> Test 3 check  3 Qs");
			 this.factory.createQueue("queue2", RoQUtils.getInstance().getLocalIP().toString());
			//3. Sleep for test
			Thread.sleep(5000);
			Assert.assertEquals(3,  mngtController.getStorage().getQueues().size());
			Assert.assertEquals(true,  mngtController.getStorage().getQueue("queue2").isRunning());
			
			logger.info("---> Test 4 Remove  1Q and check 2 Qs");
			this.factory.removeQueue("queue1");
			Thread.sleep(5000);
			Assert.assertEquals(3,  mngtController.getStorage().getQueues().size());
			Assert.assertEquals(false,  mngtController.getStorage().getQueue("queue1").isRunning());
			
			//Clean all
			this.factory.removeQueue("queueTest");
			this.factory.removeQueue("queue2");
			this.mngtController.getShutDownMonitor().shutDown();
			Thread.sleep(3000);
			
		} catch (InterruptedException e) {
			logger.error("Error here", e);
		} catch (SQLException e) {
			logger.error("Error here due to SQL storage", e);
		}
	}
	
	/**
	 * Test the BSON interface exposed by the management controller
	 * @throws Exception
	 */
	@Test
	public void testBsonRequest() throws Exception {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket mngtREQSocket = context.socket(ZMQ.REQ);
		mngtREQSocket.connect("tcp://localhost:5003");
		
		//1. define the queue
		String qName = "testQ1";
		
		//2.  Create a client management & sending command request
		MngClient client = new MngClient("localhost");
		client.testCreate(qName);
		
		//3. Test the message sending
		attachSUbscriber(qName);
		IRoQPublisher publisher = attachPublisher(qName);
		sendMsg(publisher);
		
		//4. Remove the queue
		client.testRemove(qName);
		
		//Phase 2 Test the stop
		qName = "testQ2";
		//1.  Create a queue
		client.testCreate(qName);
		
		//2. Stop the queue
		client.testStop(qName);
		
		//3. Stop the queue
		client.testStart(qName);
		
		//4. Remove the queue
		client.testRemove(qName);
				
	}
	
	/**
	 * Test the auto scaling configuration creation at the management controller level.
	 * @throws Exception
	 */
	@Test
	public void testAutoScalingRequest() throws Exception {
		MngClient client = new MngClient("localhost");
		//1. Test the queue creation
		//Phase 2 Test the stop
		String qName = "testQ2";
		
		//1.  Create a queue
		client.testCreate(qName);
		
		//2. Test the auto scaling configuration
		AutoScalingConfig config = new AutoScalingConfig();
		config.setName("confTest");
		HostScalingRule hRule = new HostScalingRule(30, 40);
		config.setHostRule(hRule);
		XchangeScalingRule xRule =  new XchangeScalingRule(20000, 0);
		config.setXgRule(xRule);
		LogicalQScalingRule qRule = new LogicalQScalingRule(10000, 100000);
		config.setqRule(qRule);
		client.testAutoScaling(qName, config);
		client.testSameAutoScaling(qName, config);
		
		//3. Stop the queue
		client.testStop(qName);
		
		//4. Stop the queue
		client.testStart(qName);
		
		//5. Remove the queue
		client.testRemove(qName);
		
	}

}
