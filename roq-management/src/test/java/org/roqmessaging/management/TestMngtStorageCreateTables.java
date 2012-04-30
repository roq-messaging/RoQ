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

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roqmessaging.management.server.MngtServerStorage;

/**
 * Class TestMngtStorageCreateTables
 * <p> Description: Test the SQL lite storage feature and schemas creation
 * 
 * @author sskhiri
 */
public class TestMngtStorageCreateTables {

	private Logger logger = Logger.getLogger(TestMngtStorageCreateTables.class);
	
	@Test
	public void test() {
		 // load the sqlite-JDBC driver using the current class loader
	    try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			logger.error(e1);
		}
	    
	    Connection connection = null;
	    try
	    {
	      // create a database connection
	      connection = DriverManager.getConnection("jdbc:sqlite:sampleMngt.db");
	      Statement statement = connection.createStatement();
	      statement.setQueryTimeout(30);  // set timeout to 30 sec.
	      //Drop table if exist - clean the file
	      statement.executeUpdate("drop table if exists Hosts");
	      statement.executeUpdate("drop table if exists Configuration");
	      statement.executeUpdate("drop table if exists Queues");
	      //Create scripts
	      statement.execute("CREATE  TABLE IF NOT EXISTS `Hosts` (  `idHosts` INTEGER PRIMARY KEY AUTOINCREMENT ," +
	      		"  `IP_Address` VARCHAR(45) NULL " +
	      		" )");
	      statement.execute("CREATE  TABLE IF NOT EXISTS `Configuration` (  `idConfiguration` INTEGER PRIMARY KEY AUTOINCREMENT ,	" +
	      		"  `Name` VARCHAR(45) NULL ,	  " +
	      		"`MAX_EVENT_EXCHANGE` MEDIUMTEXT NULL ," +
	      		" `MAX_PUB_EXCHANGE` MEDIUMTEXT NULL " +
	      		"  );");
	      statement.execute("CREATE  TABLE IF NOT EXISTS `Queues` ( `idQueues`INTEGER PRIMARY KEY AUTOINCREMENT ," +
	      		" `Name` VARCHAR(45) NULL ,  " +
	      		"`MainhostRef`  INT NOT NULL, " +
	      		"`ConfigRef`  INT NOT NULL,  " +
	      		"`State` INT NOT NULL," +
	      		"  FOREIGN KEY(`MainhostRef`) REFERENCES `Hosts` (idHosts)," +
	      		" FOREIGN KEY(`ConfigRef`) REFERENCES `Configuration` (idConfiguration)" +
	      		")");
	      //insert tuples
	      MngtServerStorage facade = new MngtServerStorage();
	      String serverAddress = "127.0.0.1";
	      facade.addRoQHost(serverAddress);
	      facade.addRoQHost("1270.1.2");
	      facade.addConfiguration("Configuration1", 100000, 2000);
	      facade.addConfiguration("Configuration2", 5000, 2000);
	      facade.addQueueConfiguration("Queue1", 1, 2, true);
      
	      //Query example
	      ResultSet rs = statement.executeQuery("select * from Queues");
	      while(rs.next())
	      {
	        // read the result set
	        System.out.println("name = " + rs.getString("name") + ", id = " + rs.getInt("idQueues")+", Config = " + rs.getInt("ConfigRef")+ ", State = "+rs.getString("State"));
	      }
	      rs = statement.executeQuery("select * from Hosts");
	      while(rs.next())
	      {
	        // read the result set
	        System.out.println("address = " + rs.getString("IP_Address") + ", id = " + rs.getInt("idHosts"));
	      }
	      rs = statement.executeQuery("select * from Configuration");
	      while(rs.next())
	      {
	        // read the result set
	    	  System.out.println("name = " + rs.getString("name") + ", id = " + rs.getInt("idConfiguration")+", MAX event  = " + rs.getInt("MAX_EVENT_EXCHANGE")  );
	      }
	    }
	    catch(SQLException e)
	    {
	      // if the error message is "out of memory", 
	      // it probably means no database file is found
	      System.err.println(e.getMessage());
	    }
	    finally
	    {
	      try
	      {
	        if(connection != null)
	          connection.close();
	      }
	      catch(SQLException e)
	      {
	        // connection close failed.
	        System.err.println(e);
	      }
	    }
	}

}
