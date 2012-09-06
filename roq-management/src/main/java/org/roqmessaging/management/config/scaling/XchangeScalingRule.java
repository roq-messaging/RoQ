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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstantInternal;

/**
 * Class XchangeScalingRule
 * <p> Description: autoscaling rule based on the Exchange load.
 * 
 * @author sskhiri
 */
public class XchangeScalingRule implements IAutoScalingRule {
	//Logger
	private Logger logger = Logger.getLogger(HostScalingRule.class);
	//KPI on the number of message throughput the last minute
	private int Throughput_Limit = 0;
	//KPI on Time_Spend, can be used for rampup of xchange nodes
	private float Time_Limit = 0;
	
	/**
	 * @see org.roqmessaging.management.config.scaling.IAutoScalingRule#isOverLoaded(java.util.HashMap)
	 */
	public boolean isOverLoaded(HashMap<String, Double> context) {
		Double eventLimit = context.get(RoQConstantInternal.CONTEXT_KPI_XCHANGE_EVENTS);
		if (this.getEvent_Limit() != 0) {
			if (eventLimit.floatValue() > this.getEvent_Limit()) {
				logger.info("Host Scaling rule reached [cpu: " + eventLimit.floatValue() + "]");
				return true;

			}
		}
		return false;
	}
	

	/**
	 * Constructor
	 * @param throughput_Limit the max event limit at an exchange
	 * @param time_Limit the time limit
	 */
	public XchangeScalingRule(int throughput_Limit, float time_Limit) {
		super();
		Throughput_Limit = throughput_Limit;
		Time_Limit = time_Limit;
	}



	/**
	 * @return the event_Limit
	 */
	public int getEvent_Limit() {
		return Throughput_Limit;
	}

	/**
	 * @param event_Limit the event_Limit to set
	 */
	public void setEvent_Limit(int event_Limit) {
		Throughput_Limit = event_Limit;
	}

	/**
	 * @return the time_Limit
	 */
	public float getTime_Limit() {
		return Time_Limit;
	}

	/**
	 * @param time_Limit the time_Limit to set
	 */
	public void setTime_Limit(float time_Limit) {
		Time_Limit = time_Limit;
	}

}
