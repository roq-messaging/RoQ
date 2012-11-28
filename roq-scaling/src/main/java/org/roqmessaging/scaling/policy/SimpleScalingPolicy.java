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
package org.roqmessaging.scaling.policy;

import java.util.HashMap;

/**
 * Class SimpleScalingPolicy
 * <p> Description: a simple scaling policy that first looks whether we need to create an exchange 
 * or spawning a new machine.
 * 
 * @author sskhiri
 */
public class SimpleScalingPolicy implements IScalingPolicy {

	/**
	 * @see org.roqmessaging.scaling.policy.IScalingPolicy#scaleOut(java.util.HashMap)
	 */
	public boolean scaleOut(HashMap<String, Double> context) {
		//1. Get the list of host from the GCM
		//2. for each host ask the host the number of exchanges
		//3. If one has a few number we can create a new exchanges otherwise we need to spawn a new host with a brand new exchange.
		return false;
	}

}
