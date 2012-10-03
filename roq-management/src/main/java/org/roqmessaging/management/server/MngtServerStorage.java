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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.HostConfigManager;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.IAutoScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Class MngtServerController
 * <p>
 * Description: The controller is responsible for offering the Storage and
 * control features of the management API. In opposite to the
 * {@linkplain GlobalConfigurationManager} which only manages the runtime
 * queues, this controller is able to manage non executing queues.
 * 
 * @author sskhiri
 */
public class MngtServerStorage {
	private Logger logger = Logger.getLogger(MngtServerStorage.class);
	private Connection connection = null;
	// Prevent reetrant code
	private Lock lock = new ReentrantLock();
	//Autoscaling storage manager
	private AutoScalingRuleStorageManager ruleManager = null;

	/**
	 * Constructor.
	 */
	public MngtServerStorage(Connection connection) {
		this.connection = connection;
		ruleManager = new AutoScalingRuleStorageManager();
		initSchema();
	}

	/**
	 * Create the schema if it does exist yet, otherwise, does nothing.
	 */
	private void initSchema() {
		try {
			this.lock.lock();
			String dbLocation = null;
			try {
				// 1. Get the DB location
				dbLocation = connection.getMetaData().getURL();
				logger.info("Meta data:" + dbLocation);
				logger.info("Creating DB schemas if not created yet...");

				// 2 Init the schema
				Statement statement = connection.createStatement();
				statement.setQueryTimeout(10);  // set timeout to 10 sec.
				// Create scripts
				statement.executeUpdate("CREATE  TABLE IF NOT EXISTS `Hosts`"
						+ " (  `idHosts` INTEGER PRIMARY KEY AUTOINCREMENT ," + "  `IP_Address` VARCHAR(45) UNIQUE "
						+ " )");
				statement.executeUpdate("CREATE  TABLE IF NOT EXISTS `Configuration`"
						+ " (  `idConfiguration` INTEGER PRIMARY KEY AUTOINCREMENT ,	"
						+ "  `Name` VARCHAR(45) NOT NULL UNIQUE ,	  " + "`MAX_EVENT_EXCHANGE` MEDIUMTEXT NULL ,"
						+ " `MAX_PUB_EXCHANGE` MEDIUMTEXT NULL " + "  );");
				
				//Create the Autoscaling rule tables
				String createHostRuleTable ="CREATE  TABLE IF NOT EXISTS `AS_Host_Rules` ("+
				"  `rule_id` INTEGER PRIMARY KEY AUTOINCREMENT ,"+
				" `CPU_Limit` INTEGER NULL ,"+
				"  `RAM_Limit` INTEGER NULL "+	
				");";
				statement.executeUpdate(createHostRuleTable);
				
				//Create AS_Xchange_Rule table
				//Throughput per exchange
				String createASXchangeTable ="CREATE  TABLE IF NOT EXISTS `AS_Xchange_Rules` ("+
				"  `rule_id` INTEGER PRIMARY KEY AUTOINCREMENT ,"+
				" `Throughput` INTEGER NULL ,"+
				"  `Time_Spend` FLOAT NULL "+
				");";
				statement.executeUpdate(createASXchangeTable);
				
				//Create Auto-scaling Queue rule table
				//Either we define the number of Exchange per producer or Exchange number per Throughput
				String createASQTable ="CREATE  TABLE IF NOT EXISTS `AS_LogicalQueue_Rules` ("+
				"  `rule_id` INTEGER PRIMARY KEY AUTOINCREMENT ,"+
				"  `Producer_per_exchange_limit` INTEGER NULL, "+	
				"  `Throughput_per_exchange_limit` INTEGER NULL "+	
				");";
				statement.executeUpdate(createASQTable);
				
				//Create Auto-scaling Configuration table
				String createASConfigTable ="CREATE  TABLE IF NOT EXISTS `AutoScaling_Cfg` ("+
				"  `idConfig` INTEGER PRIMARY KEY AUTOINCREMENT ,"+
				 "`Name` VARCHAR(45) NOT NULL UNIQUE ,	 " +
				" `HostRuleID` INT NULL ,"+
				" `XchangeRuleID` INT NULL ,"+
				" `QueueRuleID` INT NULL ,"+
			    "  FOREIGN KEY(`HostRuleID`) REFERENCES `AS_Host_Rules` (rule_id),"+
			    "  FOREIGN KEY(`XchangeRuleID`) REFERENCES `AS_Xchange_Rule` (rule_id),"+
			    "  FOREIGN KEY(`QueueRuleID`) REFERENCES `AS_LogicalQueue_Rules` (rule_id)"+
				");";
				statement.executeUpdate(createASConfigTable);
				
				//Create Queues table
				statement.executeUpdate("CREATE  TABLE IF NOT EXISTS `Queues` "
						+ "( `idQueues`INTEGER PRIMARY KEY AUTOINCREMENT ," + 
						" `Name` VARCHAR(45) NOT NULL UNIQUE ,  "+
					    "`MainhostRef`  VARCHAR(45)  NOT NULL, " + 
						"`ConfigRef`  INT NOT NULL,  " + 
					    "`State` INT NOT NULL,"+
					    "`autoscalingCfg` VARCHAR(45) NULL,"+
						"  FOREIGN KEY(`MainhostRef`) REFERENCES `Hosts` (IP_Address),"+
					    " FOREIGN KEY(`ConfigRef`) REFERENCES `Configuration` (idConfiguration)," +
					    " FOREIGN KEY(`autoscalingCfg`) REFERENCES `AutoScaling_Cfg` (Name)" +
						")");
				logger.info("DB Created and initiated.");
				statement.close();
			} catch (SQLException e) {
				logger.error("Error when initiating the schema", e);
			}
		} finally {
			this.lock.unlock();
		}

	}
	
	/**
	 * Drop the table if they exit
	 */
	public void formatDB() {
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(10);  // set timeout to 30 sec.
			statement.executeUpdate("drop table if exists Hosts");
			statement.executeUpdate("drop table if exists Configuration");
			statement.executeUpdate("drop table if exists Queues");
			statement.executeUpdate("drop table if exists AS_Host_Rules");
			statement.executeUpdate("drop table if exists AS_Xchange_Rules");
			statement.executeUpdate("drop table if exists AS_LogicalQueue_Rules");
			statement.executeUpdate("drop table if exists AutoScaling_Cfg");
			statement.close();
			//Re-build schema
			initSchema();
		} catch (Exception e) {
			logger.error("Error while inserting new host", e);
		}
	}

	/**
	 * @param serverAddress
	 *            the address of the host on which the
	 *            {@linkplain HostConfigManager} is running.
	 * @return the row id of the inserted tuple
	 */
	public String addRoQHost(String serverAddress) {
		try {
			this.lock.lock();
			logger.info("Inserting 1 new host in configuration " + serverAddress);

			try {
				Statement statement = connection.createStatement();
				// set timeout to 10 sec.
				statement.setQueryTimeout(10);
				statement.execute("insert into Hosts  values(null, '" + serverAddress + "')");
				statement.close();
				return serverAddress;
			} catch (Exception e) {
				logger.error("Error while inserting new host", e);
			}
			return null;
		} finally {
			this.lock.lock();
		}
	}

	/**
	 * The configuration defines the maximum load accepted by an exchange
	 * 
	 * @param name
	 *            the configuration name
	 * @param maxEvent
	 *            the maximum event throughput per second and per exchange
	 * @param maxPub
	 *            the maximum number of publisher that an exchange can accept
	 */
	public void addConfiguration(String name, int maxEvent, int maxPub) {
		try {
			this.lock.lock();
			logger.info("Inserting 1 new Exchange configuration");
			logger.info("Inserting " + name + " " + maxEvent + " " + maxPub);
			try {
				Statement statement = connection.createStatement();
				// set timeout to 10 sec.
				statement.setQueryTimeout(10);
				statement.execute("insert into Configuration  values(null, '" + name + "'," + maxEvent + ", " + maxPub
						+ ")");
				statement.close();
			} catch (Exception e) {
				logger.error("Error whil inserting new configuration", e);
			}
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Add a queue configuration.
	 * 
	 * @param name
	 *            the logical queue name
	 * @param hostRef
	 *            the host reference (FK)
	 * @param configRef
	 *            the configuration reference (FK)
	 * @param state
	 *            define whether the queue is running or not
	 *  @param autoScalingConfigName the autoscaling configuration reference
	 */
	public void addQueueConfiguration(String name, String ipRef, int configRef, boolean state, String autoScalingConfigName) {
		try {
			this.lock.lock();
			logger.info("Inserting 1 new logical Q configuration");
			logger.info("Inserting " + name + " " + ipRef + " " + configRef + " " + state);
			try {
				Statement statement = connection.createStatement();
				// set timeout to 10 sec.
				statement.setQueryTimeout(10);
				statement.execute("insert into Queues  values(null, '" + 
						name + "','" + 
						ipRef + "', " + 
						configRef + ", " +
						(state ? 1 : 0) +", '"+ 
						 autoScalingConfigName+ "'" + 
						")");
				statement.close();
			} catch (Exception e) {
				logger.error("Error while inserting new configuration", e);
			}
		} finally {
			this.lock.unlock();
		}
	}
	
	/**
	 * @param qName the queue name to update the configuration
	 * @param autscalingConfigName the auto scaling configuration to point to.
	 */
	public void updateAutoscalingQueueConfig(String qName, String autscalingConfigName) {
		try {
			this.lock.lock();
			logger.info("updateing 1 new logical Q configuration with auto scaling configuration name");
			logger.info("Inserting in  " + qName + " config:" + autscalingConfigName);
			try {
				Statement statement = connection.createStatement();
				// set timeout to 10 sec.
				statement.setQueryTimeout(10);
				statement.execute("UPDATE Queues SET autoscalingCfg='"  + autscalingConfigName  +"' where name='"
						+ qName + "' ;");
				statement.close();
			} catch (Exception e) {
				logger.error("Error while inserting new configuration", e);
			}
		} finally {
			this.lock.unlock();
		}
	}
	
	/**Creates a rule in the autoscaling config table
	 * @param name the configuration name
	 * @param hostRuleRef the reference to the host rule, 0  if it is null
	 * @param qRuleRef the reference to Q rule,  0  if it is null
	 * @param xchangeRuleRef the reference to the xchange rule,  0  if it is null
	 */
	public void addAutoScalingConfig(String cfgName, int hostRuleRef, int qhostRuleRef, int xchangeRuleRef){
		try {
			this.lock.lock();
			logger.info("Inserting 1 new  autoscaling configuration");
			logger.info("Inserting " + hostRuleRef + " " + qhostRuleRef + " " + xchangeRuleRef );
			try {
				Statement statement = connection.createStatement();
				// set timeout to 10 sec.
				statement.setQueryTimeout(10);
				statement.execute("insert into AutoScaling_Cfg  values(null, '" + 
						cfgName + "',"+
						(hostRuleRef==0?null:hostRuleRef) + "," + 
						(xchangeRuleRef==0?null:xchangeRuleRef) +  ", " + 
						(qhostRuleRef==0?null:qhostRuleRef)  +
						")");
				statement.close();
			} catch (Exception e) {
				logger.error("Error while inserting new configuration", e);
			}
		} finally {
			this.lock.unlock();
		}
	}
	
	/**
	 * Return the specified configuration or null if it does not exist.
	 * TODO implement & test  the method && return config object.
	 * @param name the configuration name
	 * @throws SQLException  in case od SQL issue
	 */
	public AutoScalingConfig getAutoScalingCfg(String name) throws SQLException{
		Statement statement = connection.createStatement();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select Name, HostRuleID, XchangeRuleID, QueueRuleID" + " from AutoScaling_Cfg "
				+ "where Name='"+ name+"';" );
		if (!rs.next()) {
			logger.warn("The auto scaling configuration named "+ name +" does not exist in DB.");
			return null;
		} else {
			//1. Extract the Reference 
			int hostRID = rs.getInt("HostRuleID");
			int xchangeRID =   rs.getInt("XchangeRuleID");
			int qRID =   rs.getInt("QueueRuleID");
			logger.debug("Getting Autoscaling FK ID " + rs.getString("name") + ": " + 
					hostRID+", "+
					xchangeRID+", "+
					qRID);
			statement.close();
			
			//2. Look for each FK the corresponding rule
			AutoScalingConfig autoScalingConfig =new AutoScalingConfig();
			autoScalingConfig.setName(name);
			if(hostRID!=0){
				autoScalingConfig.setHostRule(this.ruleManager.getHostScalingRule( connection.createStatement(),hostRID));
			}
			if(xchangeRID!=0){
				autoScalingConfig.setXgRule(this.ruleManager.getExchangeScalingRule( connection.createStatement(),xchangeRID));
			}
			if(qRID!=0){
				autoScalingConfig.setqRule(this.ruleManager.getQScalingRule( connection.createStatement(),qRID));
			}
			return autoScalingConfig;
				
		}
	
	}
	
	/**
	 * remove the specified configuration or null if it does not exist.
	 * TODO implement & test  the method
	 * @param name the configuration name
	 */
	public void removeAutoScalingScg(String name){};
	

	/**
	 * Read all queues definition in DB
	 * 
	 * @return the list of Queue states
	 * @throws SQLException
	 */
	public ArrayList<QueueManagementState> getQueues() throws SQLException {
		logger.debug("Get all queues");
		ArrayList<QueueManagementState> result = new ArrayList<QueueManagementState>();
		Statement statement = connection.createStatement();
		// set timeout to 10 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select name, State, IP_Address, autoscalingCfg" + " from Queues, Hosts "
				+ "where Queues.MainhostRef=Hosts.IP_Address;");
		while (rs.next()) {
			QueueManagementState state = new QueueManagementState(rs.getString("name"), rs.getString("IP_Address"),
					rs.getBoolean("State"), rs.getString("autoscalingCfg"));
			logger.debug("name = " + state.getName() + ", State = " + state.isRunning() + " IP = " + state.getHost() +" AS config: "+rs.getInt("autoscalingCfg"));
			result.add(state);
		}
		statement.close();
		return result;
	}

	/**
	 * @param newConfig
	 *            the updated configuration received each minute
	 * @param hosts  the list of host manager deployed
	 * @throws SQLException
	 */
	public void updateConfiguration(HashMap<String, String> newConfig, List<String> hosts) throws SQLException {
		try {
			this.lock.lock();
			logger.debug("Updating configuration tables ...");
			// This will define the set of new queues that are not known yet by
			// the management
			ArrayList<String> newQueues = new ArrayList<String>();
			// 1. Select name from Queues
			ArrayList<QueueManagementState> queueStates = this.getQueues();
			//Get all defined hosts
			ArrayList<String > hostStates = getHosts();

			// 2. Check whether an existing Q is now running
			for (String qName : newConfig.keySet()) {
				boolean qFound = false;
				Iterator<QueueManagementState> iter = queueStates.iterator();
				while (iter.hasNext() && !qFound) {
					QueueManagementState state_i = iter.next();
					if (state_i.getName().equals(qName)) {
						qFound = true;
						// This means that the was known, let's check the host

						if (!state_i.getHost().equals(newConfig.get(qName))) {
							// The queue was known but the host changed-> update
							// the host in management DB
							String ip = this.getHost(newConfig.get(qName));
							if (ip == null) {
								// The host on the new configuration is unknown
								// Add the host
								ip = addRoQHost(newConfig.get(qName));
							}
							logger.debug("Update DB: update Queue " + state_i.getName() + " with host to " + ip);
							Statement statement = connection.createStatement();
							statement.setQueryTimeout(10);
							statement.executeUpdate("UPDATE Queues SET MainhostRef=" + ip + " where name='"
									+ state_i.getName() + "' ;");
							statement.close();
						}

						if (!state_i.isRunning()) {
							// The Q is known but was seen as off
							logger.debug("Update DB: update Queue " + state_i.getName() + " with running TRUE");
							Statement statement = connection.createStatement();
							statement.setQueryTimeout(10);
							statement.executeUpdate("UPDATE Queues SET State=1 where name='" + state_i.getName()
									+ "' ;");
							statement.close();
						}
					}
				}// end of inner loop

				if (!qFound) {
					newQueues.add(qName);
				}
			}
			// 3. Set to running false the queue that are not present anymore
			for (QueueManagementState state_i : queueStates) {
				if (!newConfig.containsKey(state_i.getName()) && state_i.isRunning()) {
					// Set the queue to running false
					logger.debug("Update DB: update Queue " + state_i.getName() + " with running FALSE");
					Statement statement = connection.createStatement();
					statement.setQueryTimeout(10);
					statement.executeUpdate("UPDATE Queues SET State=0 where name='" + state_i.getName() + "' ;");
					statement.close();
				}
			}

			// 4. Check whether there is a new Q (created by code)
			for (String qName : newQueues) {
				// Check whether the host is known
				String IP = this.getHost(newConfig.get(qName));
				if (IP == null) {
					// The host on the new configuration is unknown
					// Add the host
					IP = addRoQHost(newConfig.get(qName));
				}
				// Add the queue with default configuration
				this.addQueueConfiguration(qName, IP, 1, true, null);
			}
			
			//5. Check wether a new host has been added
			for (Iterator<String> iterator = hosts.iterator(); iterator.hasNext();) {
				String hosts_i = (String) iterator.next();
				if(!hostStates.contains(hosts_i)){
					this.addRoQHost(hosts_i);
				}
			}
			
			for (Iterator<String> iterator = hostStates.iterator(); iterator.hasNext();) {
				String hostState_i = (String) iterator.next();
				if(!hosts.contains(hostState_i)){
					this.removeHost(hostState_i);
				}
			}
		} catch (Exception e) {
			logger.error("Error while updating configuration", e);
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * @param name
	 *            the logical queue name
	 * @return the state of the queue if exists , otherwise null
	 * @throws IllegalStateException
	 *             whether there is 2 entry for the same name
	 * @throws SQLException
	 */
	public QueueManagementState getQueue(String name) throws IllegalStateException, SQLException {
		Statement statement = connection.createStatement();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select name, State, IP_Address, autoscalingCfg" + " from Queues, Hosts "
				+ "where Queues.MainhostRef=Hosts.IP_Address AND Queues.name='" + name + "';");
		if (!rs.next()) {
			return null;
		} else {
			logger.debug("Getting Q " + rs.getString("name") + ": " + rs.getString("IP_Address")
					+ " "+(rs.getInt("State") == 0 ? false : true) +" AS config  "+rs.getInt("autoscalingCfg"));
			QueueManagementState result = new QueueManagementState(rs.getString("name"), rs.getString("IP_Address"),
					rs.getInt("State") == 0 ? false : true, rs.getString("autoscalingCfg"));
			statement.close();
			return result;
		}
	}

	/**
	 * Just check whether the IP address is present in DB. Now the IP address is used as FK in the queue table.
	 * @param ipAddress
	 *            the IP address of the host
	 * @return the row id of the host or -1 if unknown
	 * @throws SQLException
	 */
	public String getHost(String ipAddress) throws SQLException {
		Statement statement = connection.createStatement();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select idHosts from Hosts " + "WHERE IP_Address='" + ipAddress + "';");
		if (!rs.next())
			return null;
		else {
			statement.close();
			return ipAddress;
		}
	}

	/**
	 * @return the list of host that RoQ knows
	 * @throws SQLException  if the connection to sql lite fails
	 */
	public ArrayList<String> getHosts() throws SQLException {
		ArrayList<String> hosts = new ArrayList<String>();
		logger.debug("Getting all hosts ...");
		Statement statement = connection.createStatement();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select IP_Address from Hosts;");
		while (rs.next()) {
			hosts.add(rs.getString("IP_Address"));
			logger.debug(rs.getString("IP_Address"));
		}
		statement.close();
		return hosts;
	}

	/**
	 * Removes the queue defined by the qName
	 * @param qName the queue to remove
	 * @throws SQLException if an exception occurred when removing the record
	 */
	public void removeQueue(String qName) throws SQLException {
		logger.debug("Delete the queue "+ qName);
		Statement statement = connection.createStatement();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("Select idQueues from Queues  where name='" + qName + "' ;");
		while (rs.next()) {
			statement.executeUpdate("DELETE  from Queues where idQueues="+rs.getInt("idQueues")+";");
		}
		statement.close();
	}
	
	/**
	 * Store the auto scaling rule.
	 * @param rule the auto scaling rule to add.
	 * @throws SQLException if an exception occurred when removing the record
	 */
	public int addAutoScalingRule(IAutoScalingRule rule) throws SQLException {
		try {
			this.lock.lock();
			if (rule instanceof XchangeScalingRule) {
				return ruleManager.addExchangeRule(connection.createStatement(), (XchangeScalingRule) rule);
			}
			if (rule instanceof LogicalQScalingRule) {
				return ruleManager.addQueueRule(connection.createStatement(), (LogicalQScalingRule) rule);
			}
			if (rule instanceof HostScalingRule) {
				return ruleManager.addHostRule(connection.createStatement(), (HostScalingRule) rule);
			}
		} catch (Exception e) {
			logger.error("Error while creating autoscaling rule in db", e);
		} finally {
			this.lock.unlock();
		}
		return -1;
	}
	
	/**
	 * Remove the auto scaling rule.
	 * @param rule the auto scaling rule to add.
	 * @throws SQLException if an exception occurred when removing the record
	 */
	public void removeAutoScalingRule(IAutoScalingRule rule) throws SQLException{
		if (rule instanceof XchangeScalingRule) {
			ruleManager.removeXChangeRule(connection.createStatement(), rule.getID());
		}
		if (rule instanceof LogicalQScalingRule) {
			ruleManager.removeQRule(connection.createStatement(), rule.getID());
		}
		if (rule instanceof HostScalingRule) {
			ruleManager.removeHostRule(connection.createStatement(), rule.getID());
		}
	}
	
	/**
	 * @return the aggregated list of auto scaling rules.
	 * @throws SQLException in case of SQL errors during reading.
	 */
	public List<IAutoScalingRule> getAllAutoScalingRules() throws SQLException{
		List<IAutoScalingRule> rules = new ArrayList<IAutoScalingRule>();
		rules.addAll(ruleManager.getAllLogicalQScalingRule(connection.createStatement()));
		rules.addAll(ruleManager.getAllExchangeScalingRule(connection.createStatement()));
		rules.addAll(ruleManager.getAllHostScalingRule(connection.createStatement()));
		return rules;
	}

	/**
	 * Remove all hosts from configuration
	 * @throws SQLException  in case of SQL error
	 */
	public void removeHosts() throws SQLException {
		try {
			this.lock.lock();
			logger.debug("Removing all hosts from table");
			Statement statement = connection.createStatement();
			// set timeout to 5 sec.
			statement.setQueryTimeout(5);
			statement.executeUpdate("DELETE  from Hosts;");
			statement.close();
		} finally {
			this.lock.unlock();
		}
	}
	
	/**
	 * Remove the specified host address from the host table
	 * @param host the host to remove
	 */
	private void removeHost(String host) {
		try {
			this.lock.lock();
			logger.debug("Removing host " + host + " from table");
			Statement statement = connection.createStatement();
			// set timeout to 5 sec.
			statement.setQueryTimeout(5);
			statement.executeUpdate("DELETE  from Hosts where IP_Address='" + host + "' ;");
			statement.close();
			statement = connection.createStatement();
		} catch (SQLException e) {
			logger.error("ERROR when deleting host " + host + " from hosts table", e);
		} finally {
			this.lock.unlock();
		}
	}
	
}
