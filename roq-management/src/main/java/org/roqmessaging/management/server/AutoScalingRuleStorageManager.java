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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.IAutoScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;

/**
 * Class AutoScalingRuleStorageManager
 * <p> Description: Responsible for storing/reading the autoscaling configurations. The logic is 
 * encapsulate in this manager.
 * 
 * @author Sabri Skhiri
 */
public class AutoScalingRuleStorageManager {
	private Logger logger = Logger.getLogger(AutoScalingRuleStorageManager.class);
	
	/**
	 * @return all the exchange auto scaling rules.
	 * @throws SQLException  in case of SQL error
	 */
	public List<IAutoScalingRule> getAllExchangeScalingRule(Statement statement) throws SQLException{
		List<IAutoScalingRule> result = new ArrayList<IAutoScalingRule>();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select rule_id, Throughput, Time_Spend" +
		" from AS_Xchange_Rules;");
		while (rs.next()) {
			IAutoScalingRule rule = new XchangeScalingRule(rs.getInt("Throughput"), rs.getFloat("Time_Spend"));
			rule.setID(rs.getInt("rule_id"));
			logger.debug("Reading rule: "+ rule.toString());
			result.add(rule);
		}
		statement.close();
		return result;
	}
	
	/**
	 * Get all the scaling rule related to logical queue KPI.
	 * @return all the queue auto scaling rules.
	 * @throws SQLException  in case of SQL error
	 */
	public List<IAutoScalingRule> getAllLogicalQScalingRule(Statement statement) throws SQLException{
		List<IAutoScalingRule> result = new ArrayList<IAutoScalingRule>();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select rule_id, Producer_per_exchange_limit, Throughput_per_exchange_limit" +
		" from AS_LogicalQueue_Rules;");
		while (rs.next()) {
			IAutoScalingRule rule = new LogicalQScalingRule(rs.getInt("Producer_per_exchange_limit"), rs.getInt("Throughput_per_exchange_limit"));
			rule.setID(rs.getInt("rule_id"));
			logger.debug("Reading rule: "+ rule.toString());
			result.add(rule);
		}
		statement.close();
		return result;
	}
	
	/**
	 * Get all the scaling rule related to host KPI.
	 * @return all the host auto scaling rules.
	 * @throws SQLException  in case of SQL error
	 */
	public List<IAutoScalingRule> getAllHostScalingRule(Statement statement) throws SQLException{
		List<IAutoScalingRule> result = new ArrayList<IAutoScalingRule>();
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		ResultSet rs = statement.executeQuery("select rule_id, CPU_Limit, RAM_Limit" +
		" from AS_Host_Rules;");
		while (rs.next()) {
			IAutoScalingRule rule = new HostScalingRule(rs.getInt("CPU_Limit"), rs.getInt("RAM_Limit"));
			rule.setID(rs.getInt("rule_id"));
			logger.debug("Reading rule: "+ rule.toString());
			result.add(rule);
		}
		statement.close();
		return result;
	}	
	
	/**
	 * Add an autoscaling rule in the management DB.
	 * @param statement the SQL statement
	 * @param rule the auto scaling rule
	 */
	public void addExchangeRule(Statement statement, XchangeScalingRule rule){
		logger.info("Inserting 1 new Exchange Auto scaling  configuration: "+ rule.toString());
		try {
			// set timeout to 10 sec.
			statement.setQueryTimeout(10);
			statement.execute("insert into AS_Xchange_Rules  values(null, '" + rule.getEvent_Limit() + "'," + rule.getTime_Limit() + ")");
			statement.close();
		} catch (Exception e) {
			logger.error("Error whil inserting new configuration", e);
		}
	}
	
	/**
	 * Remove the specified auto scaling rule.
	 * @param statement the SQL statement from a SQL connection.
	 * @param ruleID the rule identifying the rule to remove.
	 * @throws SQLException  in case of SQL error during the removal
	 */
	public void removeXChangeRule(Statement statement, long ruleID) throws SQLException{
		logger.debug("Deleting the rule "+ ruleID);
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		statement.executeUpdate("DELETE  from AS_Xchange_Rules where rule_id="+ruleID+";");
		statement.close();
	}
	
	/**
	 * Add an autoscaling rule in the management DB. The auto scaling rule is define at the logical Q level.
	 * @param statement the SQL statement
	 * @param rule the auto scaling rule
	 */
	public void addQueueRule(Statement statement, LogicalQScalingRule rule){
		logger.info("Inserting 1 new Queue  Auto scaling  configuration: "+ rule.toString());
		try {
			// set timeout to 10 sec.
			statement.setQueryTimeout(10);
			statement.execute("insert into AS_LogicalQueue_Rules  values(null, '" + rule.getProducerNumber() + "'," + rule.getThrougputNumber() + ")");
			statement.close();
		} catch (Exception e) {
			logger.error("Error whil inserting new configuration", e);
		}
	}
	
	/**
	 * Remove the specified auto scaling rule.
	 * @param statement the SQL statement from a SQL connection.
	 * @param ruleID the rule identifying the rule to remove.
	 * @throws SQLException  in case of SQL error during the removal
	 */
	public void removeQRule(Statement statement, long ruleID) throws SQLException{
		logger.debug("Deleting the rule "+ ruleID);
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		statement.executeUpdate("DELETE  from AS_LogicalQueue_Rules where rule_id="+ruleID+";");
		statement.close();
	}
	
	/**
	 * Add an autoscaling rule in the management DB. The auto scaling rule is define at the Physical host level.
	 * @param statement the SQL statement
	 * @param rule the auto scaling rule
	 */
	public void addHostRule(Statement statement, HostScalingRule rule){
		logger.info("Inserting 1 new Physical host Auto scaling  configuration: "+ rule.toString());
		try {
			// set timeout to 10 sec.
			statement.setQueryTimeout(10);
			statement.execute("insert into AS_Host_Rules  values(null, '" + rule.getCPU_Limit() + "'," + rule.getRAM_Limit() + ")");
			statement.close();
		} catch (Exception e) {
			logger.error("Error whil inserting new configuration", e);
		}
	}
	
	/**
	 * Remove the specified auto scaling rule.
	 * @param statement the SQL statement from a SQL connection.
	 * @param ruleID the rule identifying the rule to remove.
	 * @throws SQLException  in case of SQL error during the removal
	 */
	public void removeHostRule(Statement statement, long ruleID) throws SQLException{
		logger.debug("Deleting the rule "+ ruleID);
		// set timeout to 5 sec.
		statement.setQueryTimeout(5);
		statement.executeUpdate("DELETE  from AS_Host_Rules where rule_id="+ruleID+";");
		statement.close();
	}

}
