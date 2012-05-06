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

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
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

			// Drop table if exist - clean the file
			statement.executeUpdate("drop table if exists Hosts");
			statement.executeUpdate("drop table if exists Configuration");
			statement.executeUpdate("drop table if exists Queues");
			// insert tuples
			MngtServerStorage facade = new MngtServerStorage(connection);

			facade.addRoQHost("127.0.0.1");
			facade.addRoQHost("127.0.0.2");
			// Assert.assertEquals(1, facade.addRoQHost("127.0.0.1"));
			// Assert.assertEquals(2, facade.addRoQHost("127.0.1.2"));

			facade.addConfiguration("Configuration1", 100000, 2000);
			facade.addConfiguration("Configuration2", 5000, 2000);
			facade.addQueueConfiguration("Queue1", 1, 2, true);
			facade.addQueueConfiguration("Queue2", 2, 2, false);

			// Query example
			ResultSet rs = statement.executeQuery("select * from Queues");
			while (rs.next()) {
				// read the result set
				logger.debug("name = " + rs.getString("name") + ", id = " + rs.getInt("idQueues") + ", Config = "
						+ rs.getInt("ConfigRef") + ", State = " + rs.getString("State"));
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
			rs = statement.executeQuery("select name, State, IP_Address" + " from Queues, Hosts "
					+ "where Queues.MainhostRef=Hosts.idHosts AND name='" + name + "';");
			logger.debug(("select name, State, IP_Address" + " from Queues, Hosts "
					+ "where Queues.MainhostRef=Hosts.idHosts AND Queues.name='" + name + "';"));
			while (rs.next()) {
				// read the result set
				logger.debug("name = " + rs.getString("name"));
			}
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
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

	@Test
	public void testUpdate() throws Exception {
		logger.debug("-> Test the update of new configuration");
		MngtServerStorage facade = new MngtServerStorage(DriverManager.getConnection("jdbc:sqlite:" + this.dbName));
		// Test state queue query
		ArrayList<QueueManagementState> queues = facade.getQueues();
		Assert.assertEquals(2, queues.size());
		// Test update new configuration
		// Test 1 the queues 2 has restarted and Queue3 has been added by code
		HashMap<String, String> newConfig = new HashMap<String, String>();
		newConfig.put("Queue2", "127.0.1.2");
		newConfig.put("Queue3", "127.0.1.1");
		facade.updateConfiguration(newConfig);

		// Check whether the Queue2 is running
		QueueManagementState q2State = facade.getQueue("Queue2");
		Assert.assertNotNull(q2State);
		Assert.assertEquals(true, q2State.isRunning());
		// Check whether the Queue3 has been added
		QueueManagementState q3State = facade.getQueue("Queue3");
		Assert.assertNotNull(q3State);
		Assert.assertEquals("127.0.1.1", q3State.getHost());
		Assert.assertEquals(true, q3State.isRunning());
		// Check whether the Queue1 has been set to stop
		QueueManagementState q1State = facade.getQueue("Queue1");
		Assert.assertNotNull(q1State);
		Assert.assertEquals(false, q1State.isRunning());
		facade.getQueues();
	}

}
