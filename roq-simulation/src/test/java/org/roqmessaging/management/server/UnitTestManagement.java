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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.curator.test.TestingServer;
import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.internal.FileConfigurationReader;
import org.roqmessaging.management.config.internal.GCMPropertyDAO;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.server.state.QueueManagementState;
import org.zeromq.ZMQ;

/**
 * Class UnitTestManagement
 * <p> Description: Unit test for the management. It tests:
 * the communication between the Global configuration manager and the Mng config server,
 *  the configuration broadcaster and the BSON encode/decode.
 * 
 * @author sskhiri
 */
public class UnitTestManagement {
	private Logger logger = Logger.getLogger(UnitTestManagement.class);
	
	//under test
	private GlobalConfigurationManager globalConfigurationManager = null;
	private TestingServer zkServer;
	private String configFile = "testGCM.properties";

	/**
	 * Helper method to create an instance of {@link GlobalConfigurationManager}
	 * based on the provided configuration file and zookeeper connection string.
	 * The function takes care to overwrite the "zkConfig.servers" property
	 * with the one provided to ensure that the GCM connects to the correct
	 * zookeeper server.
	 * 
	 * @param configFile       the gcm configuration file
	 * @param zkConnectString  the comma-separated string of ip:port zookeeper addresses
	 * @return                 a new instance of {@link GlobalConfigurationManager}
	 * @throws Exception
	 */
	private GlobalConfigurationManager createGCM(String configFile, String zkConnectString) throws Exception {
		logger.info("Creating GCM");
		// Ignore the "zk.servers" property defined in the configuration file
		// and overwrite it with the connection string provided by the TestingServer class.
		GCMPropertyDAO gcmConfig = new FileConfigurationReader().loadGCMConfiguration(configFile);
		gcmConfig.zkConfig.servers = zkConnectString;
		
		CloudConfig cloudConfig = new FileConfigurationReader().loadCloudConfiguration(configFile);
		return new GlobalConfigurationManager(gcmConfig, cloudConfig);
	}
	
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
		zkServer = new TestingServer();
		globalConfigurationManager = createGCM(configFile, zkServer.getConnectString());
		logger.info("GCM created");
		new Thread(globalConfigurationManager).start();
		
		//Launching a thread that listens the broadcast channel for management update
		new Thread(new ConfigListener()).start();
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.globalConfigurationManager.getShutDownMonitor().shutDown();
		this.zkServer.close();
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
			// TODO Fix this test. Addresses for monitor and stat monitor should be "tcp://ip:port".
			this.globalConfigurationManager.addQueue("queue1", "127.0.0.1", "127.0.0.1", "127.0.0.1", new ArrayList<String>());
			this.globalConfigurationManager.addQueue("queue2", "127.0.0.2", "127.0.0.2", "127.0.0.2", new ArrayList<String>());
			this.globalConfigurationManager.addQueue("TestQueue", "227.0.0.1", "127.0.0.1", "127.0.0.1", new ArrayList<String>());

			// 3. Wait for update
			Thread.sleep(5000);

			//4. Check queues
			logger.debug("Checking queues size at management");
			ArrayList<QueueManagementState>queues = this.globalConfigurationManager.getMngtController().getQueues();
			Assert.assertEquals(3, queues.size());
			ArrayList<String> hosts = this.globalConfigurationManager.getMngtController().getHosts();
			Assert.assertEquals(3, hosts.size());
			
			//5. Removes Qs
			logger.debug("Checking queues size at management aftrer remove");
			this.globalConfigurationManager.removeQueue("TestQueue");
			Thread.sleep(5000);
			
			ArrayList<QueueManagementState>allQs = this.globalConfigurationManager.getMngtController().getQueues();
			Assert.assertEquals(2, allQs.size());
			
			
			this.globalConfigurationManager.removeQueue("queue2");
			Thread.sleep(5000);
			this.globalConfigurationManager.getMngtController().getQueues();
			
		} catch (Exception e) {
			logger.error("Error while waiting", e);
		}

	}
	
	/**
	 * Class ConfigListener
	 * <p> Description: Test class used for simulating a management console that listen the update broadcast.
	 * 
	 * @author sskhiri
	 */
	protected class ConfigListener implements Runnable{
		private IRoQSerializer serializer = new RoQBSONSerializer();

		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			logger.debug("Starting Test config Listener ...");
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket sub = context.socket(ZMQ.SUB);
			// TODO Change port 5004 to port 5005, which is the one used by MngtControllerTimer
			sub.connect("tcp://"+RoQUtils.getInstance().getLocalIP()+":5004");
			sub.subscribe("".getBytes());
			
			//The message arrive in 3 parts
			//1. The ID
			byte [] infoCode = sub.recv(0);
			BSONObject codeObject = BSON.decode(infoCode);
			Assert.assertEquals(RoQConstant.MNGT_UPDATE_CONFIG, codeObject.get("CMD_ID"));
			Assert.assertEquals(true, sub.hasReceiveMore());
			logger.debug("CMD ID.... OK");
			//2. The queues
			byte [] encodedQ = sub.recv(0);
			Assert.assertEquals(true, sub.hasReceiveMore());
			List<QueueManagementState> queues = this.serializer.unSerializeQueues(encodedQ);
			logger.debug("There are " + queues.size()+" Qs");
			logger.debug("Q configuration... OK");
			//3. The hosts
			byte [] encodedH = sub.recv(0);
			List<String> hosts = this.serializer.unSerializeHosts(encodedH);
			logger.debug("There are " + hosts.size()+" Hosts");
			logger.debug("Hosts... OK");
			
			//Close all
			sub.close();
		}
		
	}

}
