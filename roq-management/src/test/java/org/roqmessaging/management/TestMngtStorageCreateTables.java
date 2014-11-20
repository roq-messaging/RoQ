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
package org.roqmessaging.management;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.IAutoScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;
import org.roqmessaging.management.server.MngtServerStorage;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Class TestMngtStorageCreateTables
 * <p>
 * Description: Test the SQL lite storage feature and schemas creation
 * 
 * @author sskhiri
 */
public class TestMngtStorageCreateTables {
	private String dbName = "sampleMngt.db";

	private Logger logger = Logger.getLogger(TestMngtStorageCreateTables.class);

	@Test
	public void testCreate() {
		// load the sqlite-JDBC driver using the current class loader
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			logger.error(e1);
		}

		Connection connection = null;
		try {
			logger.debug("-> Test the SQL DB creation & query");
			// create a database connection
			connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbName);
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(10);  // set timeout to 30 sec.

			// insert tuples
			MngtServerStorage facade = new MngtServerStorage(connection);
			// Drop table if exist - clean the file
			facade.formatDB();
			//Add fake DB
			addConfiguration(facade);
			
			// Query example
			ResultSet rs = statement.executeQuery("select * from Queues");
			while (rs.next()) {
				// read the result set
				logger.debug("name = " + rs.getString("name") + ", id = " + rs.getInt("idQueues") + ", Config = "
						+ rs.getInt("ConfigRef") + ", State = " + rs.getString("State") +", IP Ref: "+rs.getString("MainhostRef"));
				Assert.assertNotNull(rs.getString("name"));
				Assert.assertNotNull(rs.getString("State"));
				Assert.assertNotNull(rs.getInt("ConfigRef"));
			}
			rs = statement.executeQuery("select * from Hosts");
			while (rs.next()) {
				// read the result set
				logger.debug("address = " + rs.getString("IP_Address") + ", id = " + rs.getInt("idHosts"));
				Assert.assertNotNull(rs.getString("IP_Address"));
				Assert.assertNotNull(rs.getInt("idHosts"));
			}
			rs = statement.executeQuery("select * from Configuration");
			while (rs.next()) {
				// read the result set
				logger.debug("name = " + rs.getString("name") + ", id = " + rs.getInt("idConfiguration")
						+ ", MAX event  = " + rs.getInt("MAX_EVENT_EXCHANGE"));
				Assert.assertNotNull(rs.getString("name"));
				Assert.assertNotNull(rs.getInt("idConfiguration"));
			}
			String name = "Queue2";
			rs = statement.executeQuery("select name, State, IP_Address, MainhostRef" + " from Queues, Hosts "
					+ "where Queues.MainhostRef=Hosts.IP_Address AND name='" + name + "';");
			logger.debug(("select name, State, IP_Address" + " from Queues, Hosts "
					+ "where Queues.MainhostRef=Hosts.idHosts AND Queues.name='" + name + "';"));
			while (rs.next()) {
				// read the result set
				logger.debug("name = " + rs.getString("name")+", IP: "+ rs.getString("MainhostRef"));
			}
		} catch (SQLException e) {
			logger.error("Error during storage test", e);
			System.err.println(e.getMessage());
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (SQLException e) {
				// connection close failed.
				System.err.println(e);
			}
		}
	}

	/**
	 * This methods creates a complete fake configuration for tests.
	 * @param facade the management storage facade.
	 * @throws SQLException 
	 */
	private void addConfiguration(MngtServerStorage facade) throws SQLException {
		//Create an empty configuration with 2 hosts
		HashMap<String, String> emptyCfg = new HashMap<String, String>();
		//Host config
		List<String> hosts = new ArrayList<String>();
		hosts.add("127.0.0.2");
		hosts.add("127.0.0.1");
		hosts.add("127.0.0.3");
		facade.updateConfiguration(emptyCfg, hosts);
		
		facade.addConfiguration("Configuration1", 100000, 2000);
		facade.addConfiguration("Configuration2", 5000, 2000);
		
		//Test the auto scaling rule storage
		facade.addAutoScalingRule(new HostScalingRule(50, 40));
		facade.addAutoScalingRule(new LogicalQScalingRule(10000, 0));
		int ruleXchange1 =facade.addAutoScalingRule(new XchangeScalingRule(10000, 0));
		logger.debug("inserted a rule exchange with ID ="+ruleXchange1);
		List<IAutoScalingRule> rules =  facade.getAllAutoScalingRules();
		Assert.assertEquals(3, rules.size());
		
		facade.removeAutoScalingRule(rules.get(0));
		 rules =  facade.getAllAutoScalingRules();
		Assert.assertEquals(2, rules.size());
		
		facade.removeAutoScalingRule(rules.get(0));
		rules =  facade.getAllAutoScalingRules();
		Assert.assertEquals(1, rules.size());
		
		//Test the auto scaling config storage
		logger.info("Testing auto scaling storage");
		
		facade.addAutoScalingRule(new LogicalQScalingRule(10000, 0));
		facade.addAutoScalingRule(new XchangeScalingRule(10000, 0));
		facade.addAutoScalingRule(new LogicalQScalingRule(20000, 0));
		facade.addAutoScalingRule(new XchangeScalingRule(20000, 0));
		facade.addAutoScalingRule(new LogicalQScalingRule(30000, 0));
		facade.addAutoScalingRule(new XchangeScalingRule(30000, 0));
		facade.addAutoScalingConfig("conf1", 1, 2, 3);//host1, queue 2, xchange 3
		facade.addAutoScalingConfig("conf2", 1, 1, 3);
		facade.addAutoScalingConfig("conf3", 0, 0, 3);
		AutoScalingConfig autoScalingConfig1 = facade.getAutoScalingCfg("conf1");
		Assert.assertEquals(1, autoScalingConfig1.getHostRule().getID());
		Assert.assertEquals(2, autoScalingConfig1.getqRule().getID());
		Assert.assertEquals(3, autoScalingConfig1.getXgRule().getID());
		
		facade.addQueueConfiguration("Queue1", "127.0.0.1", 2, true, "conf1");
		facade.addQueueConfiguration("Queue2", "127.0.0.2", 2, false,"conf2");
		facade.updateAutoscalingQueueConfig("Queue2", "conf1");
		facade.updateAutoscalingQueueConfig("Queue1", "conf2");
		
		
	}

	@Test
	public void testUpdate() throws Exception {
		logger.debug("-> Test the update of new configuration");
		MngtServerStorage facade = new MngtServerStorage(DriverManager.getConnection("jdbc:sqlite:" + this.dbName));
		facade.formatDB();
		//Add the fake configuration
		addConfiguration(facade);
		// Test state queue query
		ArrayList<QueueManagementState> queues = facade.getQueues();
		Assert.assertEquals(2, queues.size());
		// Test update new configuration
		// Test 1 the queues 2 has restarted and Queue3 has been added by code
		HashMap<String, String> newConfig = new HashMap<String, String>();
		newConfig.put("Queue2", "127.0.0.2");
		newConfig.put("Queue3", "127.0.0.1");
		//Host config
		List<String> hosts = new ArrayList<String>();
		hosts.add("127.0.0.2");
		hosts.add("127.0.0.1");
		hosts.add("127.0.0.4");
		//The host 3 should have been removed
		facade.updateConfiguration(newConfig, hosts);
		
		// Check whether the host 3 has been created
		Assert.assertNotNull(facade.getHost("127.0.0.4"));
		Assert.assertNull(facade.getHost("127.0.0.3"));
		Assert.assertEquals(3, facade.getHosts().size());
		
		// Check whether the Queue2 is running
		QueueManagementState q2State = facade.getQueue("Queue2");
		Assert.assertNotNull(q2State);
		Assert.assertEquals(true, q2State.isRunning());
		// Check whether the Queue3 has been added
		QueueManagementState q3State = facade.getQueue("Queue3");
		Assert.assertNotNull(q3State);
		Assert.assertEquals("127.0.0.1", q3State.getHost());
		Assert.assertEquals(true, q3State.isRunning());
		// Check whether the Queue1 has been set to stop
		QueueManagementState q1State = facade.getQueue("Queue1");
		Assert.assertNotNull(q1State);
		Assert.assertEquals(false, q1State.isRunning());
		facade.getQueues();
		
		// Test the queue removal
		facade.getQueues();
		facade.removeQueue("Queue2");
		facade.removeQueue("Queue3");
	    queues = facade.getQueues();
		Assert.assertEquals(1, queues.size());
		//rest remove
		facade.removeHosts();
		//Check get hosts
		Assert.assertEquals(0, facade.getHosts().size());
	}
	
}
