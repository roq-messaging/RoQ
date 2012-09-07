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

/**
 * Interface AutoScalingRule
 * <p> Description: Describe the basic and common behavior for autoscaling rules.
 * We encapsulate the logic of the rule evaluation in each rule.
 * 
 * @author sskhiri
 */
public interface IAutoScalingRule {
	
	
	/**
	 * @param context the current queue context
	 * @return true if the rule is triggered. 
	 */
	public boolean isOverLoaded(HashMap<String, Double> context);
	
	/**
	 * @return the rule ID
	 */
	public long getID();
	
	/**
	 * @param ID the rule ID
	 */
	public void setID(long ID);
	
}
