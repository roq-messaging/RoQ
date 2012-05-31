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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roq.simulation.RoQAllLocalLauncher;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.LogicalQFactory;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Class TestMngtController
 * <p> Description: This test will cover the functional tests of the management server
 * 
 * @author sskhiri
 */
public class TestMngtController {
	private RoQAllLocalLauncher launcher = null;
	private Logger logger = Logger.getLogger(TestMngtController.class);
	private LogicalQFactory factory = null;
	
	//under test
	private MngtController mngtController =null;
	
	 /**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.launcher = new RoQAllLocalLauncher();
		this.launcher.setConfigPeriod(1500);
		this.launcher.setUp(true);
		this.factory = new LogicalQFactory(RoQUtils.getInstance().getLocalIP().toString());
		this.mngtController = this.launcher.getMngtController();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.factory.clean();
		this.launcher.tearDown();
	}


	@Test
	public void test() {
		try {
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

}
