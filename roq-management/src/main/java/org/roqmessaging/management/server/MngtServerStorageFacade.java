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

import org.apache.log4j.Logger;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.HostConfigManager;

/**
 * Class MngtServerController
 * <p> Description: The controller is responsible for offering the Storage and control features of the 
 * management API. In opposite to the {@linkplain GlobalConfigurationManager} which only 
 * manages the runtime queues, this controller is able to manage non executing queues.
 * 
 * @author sskhiri
 */
public class MngtServerStorageFacade {
	private Logger logger = Logger.getLogger(MngtServerStorageFacade.class);
	//The SQL connection
	private  Connection connection = null;
	
	/**
	 * 
	 */
	public MngtServerStorageFacade() {
		init();
	}
	
	public void init(){
		 // load the sqlite-JDBC driver using the current class loader
	    try {
			Class.forName("org.sqlite.JDBC");
		      // create a database connection
		      connection = DriverManager.getConnection("jdbc:sqlite:sampleMngt.db");
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
			logger.error("Error whil inserting new configuration", e);
		}
	}

}
