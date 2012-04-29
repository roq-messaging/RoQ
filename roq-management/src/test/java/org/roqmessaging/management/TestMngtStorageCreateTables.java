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

/**
 * Class TestMngtStorageCreateTables
 * <p> Description: TODO
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
	      statement.executeUpdate("drop table if exists Hosts");
	      statement.executeUpdate("drop table if exists Configuration");
	      statement.executeUpdate("drop table if exists Queues");
	      statement.execute("CREATE  TABLE IF NOT EXISTS `Hosts` (  `idHosts` INT NOT NULL ,  `IP Address` VARCHAR(45) NULL ,  PRIMARY KEY (`idHosts`))");
	      statement.execute("CREATE  TABLE IF NOT EXISTS `Configuration` (  `idConfiguration` INT NOT NULL ,	  `Name` VARCHAR(45) NULL ,	  " +
	      		"`MAX_EVENT_EXCHANGE` MEDIUMTEXT NULL , `MAX_PUB_EXCHANGE` MEDIUMTEXT NULL ,  PRIMARY KEY (`idConfiguration`));");
	      statement.execute("CREATE  TABLE IF NOT EXISTS `Queues` ( `idQueues` INT NOT NULL , `Name` VARCHAR(45) NULL ,  " +
	      		"`MainhostRef`  INT NOT NULL, `ConfigRef`  INT NOT NULL,  `State` INT NOT NULL, PRIMARY KEY (`idQueues`)," +
	      		"  FOREIGN KEY(`MainhostRef`) REFERENCES `Hosts` (idHosts)," +
	      		" FOREIGN KEY(`ConfigRef`) REFERENCES `Configuration` (idConfiguration)" +
	      		")");
	      //insert
	      statement.executeUpdate("insert into Hosts  values(1, '127.0.1.1')");
	      statement.executeUpdate("insert into Hosts  values(2, '127.0.1.2')");
	      statement.executeUpdate("insert into Configuration  values(1, 'Config1',10000, 200)");
	      statement.executeUpdate("insert into Configuration  values(2, 'Config2',10000, 200)");
	      statement.executeUpdate("insert into Queues  values(1, 'Queue1', 1,2,0)");
	      statement.executeUpdate("insert into Queues  values(2,'Queue1',2,2,1)");
      
//	      statement.executeUpdate("drop table if exists person");
//	      statement.executeUpdate("create table person (id integer, name string)");
//	      statement.executeUpdate("insert into person values(1, 'leo')");
//	      statement.executeUpdate("insert into person values(2, 'yui')");
	      ResultSet rs = statement.executeQuery("select * from Queues");
	      while(rs.next())
	      {
	        // read the result set
	        System.out.println("name = " + rs.getString("name"));
	        System.out.println("id = " + rs.getInt("idQueues"));
	        System.out.println("Config = " + rs.getInt("ConfigRef"));
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
