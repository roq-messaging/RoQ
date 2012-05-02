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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.HostConfigManager;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Class MngtServerController
 * <p> Description: The controller is responsible for offering the Storage and control features of the 
 * management API. In opposite to the {@linkplain GlobalConfigurationManager} which only 
 * manages the runtime queues, this controller is able to manage non executing queues.
 * 
 * @author sskhiri
 */
public class MngtServerStorage {
	private Logger logger = Logger.getLogger(MngtServerStorage.class);
	//The SQL connection
	private  Connection connection = null;
	
	/**
	 * 
	 */
	public MngtServerStorage(String dbName) {
		init(dbName);
	}
	
	public void init(String dbName){
		 // load the sqlite-JDBC driver using the current class loader
	    try {
			Class.forName("org.sqlite.JDBC");
		      // create a database connection
		      connection = DriverManager.getConnection("jdbc:sqlite:"+dbName);
		} catch (ClassNotFoundException e1) {
			logger.error("The SQL lite has not been installed in the class path",e1);
		} catch (SQLException e) {
			logger.error(e);
		}
	}
	
	/**
	 * @param serverAddress the address of the host on which the {@linkplain HostConfigManager}
	 * is running.
	 */
	public void addRoQHost(String serverAddress){
		logger.info("Inserting 1 new host in configuration " +serverAddress);
		
		    try {
		      Statement statement = connection.createStatement();
		      // set timeout to 10 sec.
		      statement.setQueryTimeout(10); 
		      statement.execute("insert into Hosts  values(null, '"+serverAddress+"')");
		    }catch (Exception e) {
				logger.error("Error while inserting new host",e);
			}
	}
	
	/**
	 * The configuration defines the maximum load accepted by an exchange
	 * @param name the configuration name
	 * @param maxEvent the maximum event throughput per second and per exchange
	 * @param maxPub the maximum number of publisher that an exchange can accept
	 */
	public void addConfiguration(String name, int maxEvent, int maxPub) {
		logger.info("Inserting 1 new Exchange configuration");
		logger.info("Inserting "+ name+" "+ maxEvent+" "+ maxPub);
	    try {
	      Statement statement = connection.createStatement();
	      // set timeout to 10 sec.
	      statement.setQueryTimeout(10); 
	      statement.execute("insert into Configuration  values(null, '"+ name+"',"+maxEvent+", "+maxPub+")");
	    }catch (Exception e) {
			logger.error("Error whil inserting new configuration", e);
		}
	}
	
	/**
	 * Add a queue configuration.
	 * @param name the logical queue name
	 * @param hostRef the host reference (FK)
	 * @param configRef the configuration reference (FK)
	 * @param state define whether the queue is running or not
	 */
	public void addQueueConfiguration(String name, int hostRef, int configRef, boolean state) {
		logger.info("Inserting 1 new logical Q configuration");
		logger.info("Inserting "+ name+" "+ hostRef+" "+ configRef +" " +state);
	    try {
	      Statement statement = connection.createStatement();
	      // set timeout to 10 sec.
	      statement.setQueryTimeout(10); 
	      statement.execute("insert into Queues  values(null, '"+ name+"',"+hostRef+", "+configRef+", "+(state?1:0)+")");
	    }catch (Exception e) {
			logger.error("Error while inserting new configuration", e);
		}
	}
	
	/**
	 * Read all queues definition in DB
	 * @return the list of Queue states
	 * @throws SQLException 
	 */
	public ArrayList<QueueManagementState> getQueues() throws SQLException{
		logger.debug("Get all queues");
		ArrayList<QueueManagementState> result = new ArrayList<QueueManagementState>();
		Statement statement = connection.createStatement();
		// set timeout to 10 sec.
		statement.setQueryTimeout(5);
		ResultSet  rs = statement.executeQuery("select name, State, IP_Address" + " from Queues, Hosts "
				+ "where Queues.MainhostRef=Hosts.idHosts;");
		 while(rs.next())  {
			 QueueManagementState state = new QueueManagementState(  rs.getString("name"), rs.getString("IP_Address"),  rs.getBoolean("State"));
	    	 logger.debug("name = " + state.getName() + ", State = " +state.isRunning() + " IP = " + state.getHost());
	    	 result.add(state);
	      }
		 return result;
	}
	
	/**
	 * @param newConfig the updated configuration recieved each minute
	 */
	public void updateConfiguration(HashMap<String, String> newConfig) {
		// TODO update configuration & tests
		try {
			//This will define the set of new queues that are not known yet by the management
			ArrayList<String> newQueues = new ArrayList<String>();
					
			// 1. Select name from Queues
			ArrayList<QueueManagementState> queues = this.getQueues();
			
			// 2. Check whether an existing Q is now running
			for (String qName : newConfig.keySet()) {
				boolean qFound = false;
				Iterator<QueueManagementState> iter = queues.iterator();
				while (iter.hasNext() && !qFound ) {
					QueueManagementState state_i = iter.next();
					if (state_i.getName().equals(qName)) {
						qFound = true;
						// This means that the was known, let's check the host
						
						if (!state_i.getHost().equals(newConfig.get(qName))) {
							// The queue was known but the host changed-> update
							// the host in management DB
							// TODO update host
						}
						
						if(!state_i.isRunning() ){
							//The Q is known but was seen as off
							// TODO update running to true
						}
					}
				}//end of inner loop
				
				if(!qFound){
					newQueues.add(qName);
				}
			}
			// 3. Check whether there is a new Q (created by code)
		} catch (Exception e) {
			logger.error("Error while updating configuration", e);
		}
	}

	/**
	 * @param name the logical queue name
	 * @return the state of the queue if exists , otherwise null
	 * @throws IllegalStateException whether there is 2 entry for the same name
	 * @throws SQLException 
	 */
	public QueueManagementState getQueue(String name) throws IllegalStateException, SQLException {
		Statement statement = connection.createStatement();
		// set timeout to 10 sec.
		statement.setQueryTimeout(5);
		ResultSet  rs = statement.executeQuery("select name, State, IP_Address" + " from Queues, Hosts "
				+ "where Queues.MainhostRef=Hosts.idHosts AND name='"+name+"';");
		if(rs.getString("name")!=null) 	return  new QueueManagementState(  rs.getString("name"), rs.getString("IP_Address"),  rs.getBoolean("State"));
		else return null;
	}

}
