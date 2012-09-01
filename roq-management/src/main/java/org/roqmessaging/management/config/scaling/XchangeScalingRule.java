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

/**
 * Class XchangeScalingRule
 * <p> Description: utoscaling rule based on physical on the Exchange load.
 * 
 * @author sskhiri
 */
public class XchangeScalingRule implements IAutoScalingRule {
	//Logger
	private Logger logger = Logger.getLogger(HostScalingRule.class);
	//KPI on the number of message throughput the last minute
	private int Event_Limit = 100;
	//KPI on Time_Spend, can be used for rampup of xchange nodes
	private int Time_Limit = 100;
	
	/* (non-Javadoc)
	 * @see org.roqmessaging.management.config.scaling.IAutoScalingRule#isOverLoaded(java.util.HashMap)
	 */
	public boolean isOverLoaded(HashMap<String, Double> context) {
		// TODO Auto-generated method stub
		return false;
	}

}
