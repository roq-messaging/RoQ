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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Class UnitTestManagement
 * <p> Description: Unit test for the management.
 * 
 * @author sskhiri
 */
public class UnitTestManagement {
	private Logger logger = Logger.getLogger(UnitTestManagement.class);
	
	//under test
	private MngtController mngtController =null;
	private GlobalConfigurationManager globalConfigurationManager = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		//Clean the DB
		Class.forName("org.sqlite.JDBC");
		String dbName = "SampleManagement.db";
		Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
		Statement statement = connection.createStatement();
		statement.setQueryTimeout(10);  // set timeout to 30 sec.

		// Drop table if exist - clean the file
		statement.executeUpdate("drop table if exists Hosts");
		statement.executeUpdate("drop table if exists Configuration");
		statement.executeUpdate("drop table if exists Queues");
		
		//Start the config
		globalConfigurationManager = new GlobalConfigurationManager();
		globalConfigurationManager.setConfigPeriod(3000);
		new Thread(globalConfigurationManager).start();
		mngtController = new MngtController("localhost", dbName);
		new Thread(mngtController).start();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.globalConfigurationManager.getShutDownMonitor().shutDown();
		this.mngtController.getShutDownMonitor().shutDown();
		Thread.sleep(2000);
	}

	@Test
	public void test() {
		try {
			// 1. Add host
			this.globalConfigurationManager.addHostManager("127.0.0.1");
			this.globalConfigurationManager.addHostManager("127.0.0.2");
			this.globalConfigurationManager.addHostManager("127.0.0.3");
			
			// 2. Add queues
			this.globalConfigurationManager.addQueueName("queue1", "127.0.0.1");
			this.globalConfigurationManager.addQueueLocation("queue1", "127.0.0.1");
			this.globalConfigurationManager.addQueueStatMonitor("queue1", "127.0.0.1");
			this.globalConfigurationManager.addQueueName("queue2", "127.0.0.2");
			this.globalConfigurationManager.addQueueLocation("queue2", "127.0.0.2");
			this.globalConfigurationManager.addQueueStatMonitor("queue2", "127.0.0.2");
			this.globalConfigurationManager.addQueueName("TestQueue", "227.0.0.1");
			this.globalConfigurationManager.addQueueLocation("TestQueue", "127.0.0.1");
			this.globalConfigurationManager.addQueueStatMonitor("TestQueue", "127.0.0.1");

			// 3. Wait for update
			Thread.sleep(5000);

			//4. Check queues
			logger.debug("Checking queues size at management");
			ArrayList<QueueManagementState>queues = this.mngtController.getStorage().getQueues();
			Assert.assertEquals(3, queues.size());
			ArrayList<String> hosts = this.mngtController.getStorage().getHosts();
			Assert.assertEquals(2, hosts.size());
			
			//5. Removes Qs
			logger.debug("Checking queues size at management aftrer remove");
			this.globalConfigurationManager.removeQueue("TestQueue");
			Thread.sleep(5000);
			QueueManagementState testQueue = this.mngtController.getStorage().getQueue("TestQueue");
			Assert.assertTrue(!testQueue.isRunning());
			
			ArrayList<QueueManagementState>allQs = this.mngtController.getStorage().getQueues();
			Assert.assertEquals(3, allQs.size());
			
			
			this.globalConfigurationManager.removeQueue("queue2");
			Thread.sleep(5000);
			this.mngtController.getStorage().getQueues();
			QueueManagementState q2 = this.mngtController.getStorage().getQueue("queue2");
			Assert.assertEquals(false, q2.isRunning());
			
		} catch (InterruptedException e) {
			logger.error("Error while waiting", e);
		} catch (SQLException e) {
			logger.error("Error while exec SQL", e);
		}

	}

}
