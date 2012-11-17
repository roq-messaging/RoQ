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
package org.roqmessaging.scaling;

import org.bson.BSONObject;
import org.roqmessaging.management.stat.KPISubscriber;

/**
 * Class ScalingProcess
 * <p> Description: Subscriber to the Statistic channel and process specific stat in order 
 * to evaluate auto scaling rules.
 * 
 * @author sskhiri
 */
public class ScalingProcess extends KPISubscriber {

	/**
	 * @param globalConfiguration the GCM IP address
	 * @param qName the name of the queue we want to connect.
	 */
	public ScalingProcess(String globalConfiguration, String qName) {
		super(globalConfiguration, qName);
	}

	/**
	 * @see org.roqmessaging.management.stat.KPISubscriber#processStat(java.lang.Integer, org.bson.BSONObject)
	 */
	@Override
	public void processStat(Integer CMD, BSONObject statObj) {
		this.logger.info(" Processing in AUTO SCALING process Stat "+ CMD);
		// TODO Auto-generated method stub
		
	}

}
