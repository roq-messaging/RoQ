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
package org.roqmessaging.management.config.scaling;

/**
 * Class AutoScalingConfig
 * <p> Description: Defines the autoscaling configuration with which a logical queue can be linked.
 * It is composed by several autoscaling rule. This object is mainly a DAO.
 * 
 * @author sskhiri
 */
public class AutoScalingConfig {
	//The 3 rules composing the configuration
	public HostScalingRule hostRule;
	public LogicalQScalingRule qRule;
	public XchangeScalingRule xgRule;
	//the configuration name
	private String name;
	
	/**
	 * @param hostRule the rule at the host level
	 * @param qRule the rule at the queue level
	 * @param xgRule the rule at the exchange level
	 */
	public AutoScalingConfig(HostScalingRule hostRule, LogicalQScalingRule qRule, XchangeScalingRule xgRule) {
		this.hostRule = hostRule;
		this.qRule = qRule;
		this.xgRule = xgRule;
	}

	/**
	 * Default constructor.
	 */
	public AutoScalingConfig() {
	}

	/**
	 * @return the hostRule
	 */
	public HostScalingRule getHostRule() {
		return hostRule;
	}

	/**
	 * @param hostRule the hostRule to set
	 */
	public void setHostRule(HostScalingRule hostRule) {
		this.hostRule = hostRule;
	}

	/**
	 * @return the qRule
	 */
	public LogicalQScalingRule getqRule() {
		return qRule;
	}

	/**
	 * @param qRule the qRule to set
	 */
	public void setqRule(LogicalQScalingRule qRule) {
		this.qRule = qRule;
	}

	/**
	 * @return the xgRule
	 */
	public XchangeScalingRule getXgRule() {
		return xgRule;
	}

	/**
	 * @param xgRule the xgRule to set
	 */
	public void setXgRule(XchangeScalingRule xgRule) {
		this.xgRule = xgRule;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AutoScalingConfig other = (AutoScalingConfig) obj;
		if (hostRule == null) {
			if (other.hostRule != null)
				return false;
		} else if (!hostRule.equals(other.hostRule))
			return false;
		if (qRule == null) {
			if (other.qRule != null)
				return false;
		} else if (!qRule.equals(other.qRule))
			return false;
		if (xgRule == null) {
			if (other.xgRule != null)
				return false;
		} else if (!xgRule.equals(other.xgRule))
			return false;
		return true;
	}
	
	
	

}
