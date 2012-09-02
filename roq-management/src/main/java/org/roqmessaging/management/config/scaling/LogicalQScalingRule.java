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
 * Class LogicalQScalingRule
 * <p> Description: Auto-scaling rule based on the logical queue KPI. 
 * 
 * @author sskhiri
 */
public class LogicalQScalingRule implements IAutoScalingRule {
	//Defines the max number of exchange in the logical
	private int exchangeNumberLimit =100; 
	//Defines the max number of producer per exchange limit
	private int producerNumber=100;
	//Defines the max throughput per exchange limit
	private int througputNumber=100;
	
	/**
	 * @param exchangeNumberLimit the exchange limit
	 * @param producerNumber the producer number limit per exchange limit
	 * @param througputNumber the max througput per exchange limit
	 */
	public LogicalQScalingRule(int exchangeNumberLimit, int producerNumber, int througputNumber) {
		super();
		this.exchangeNumberLimit = exchangeNumberLimit;
		this.producerNumber = producerNumber;
		this.througputNumber = througputNumber;
	}


	/**
	 * @see org.roqmessaging.management.config.scaling.IAutoScalingRule#isOverLoaded(java.util.HashMap)
	 */
	public boolean isOverLoaded(HashMap<String, Double> context) {
		// TODO Auto-generated method stub
		return false;
	}


	/**
	 * @return the exchangeNumberLimit
	 */
	public int getExchangeNumberLimit() {
		return exchangeNumberLimit;
	}


	/**
	 * @param exchangeNumberLimit the exchangeNumberLimit to set
	 */
	public void setExchangeNumberLimit(int exchangeNumberLimit) {
		this.exchangeNumberLimit = exchangeNumberLimit;
	}


	/**
	 * @return the producerNumber
	 */
	public int getProducerNumber() {
		return producerNumber;
	}


	/**
	 * @param producerNumber the producerNumber to set
	 */
	public void setProducerNumber(int producerNumber) {
		this.producerNumber = producerNumber;
	}


	/**
	 * @return the througputNumber
	 */
	public int getThrougputNumber() {
		return througputNumber;
	}


	/**
	 * @param througputNumber the througputNumber to set
	 */
	public void setThrougputNumber(int througputNumber) {
		this.througputNumber = througputNumber;
	}

}
