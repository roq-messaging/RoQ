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
 * Class LogicalQScalingRule
 * <p> Description: Auto-scaling rule based on the logical queue KPI. 
 * 
 * @author sskhiri
 */
public class LogicalQScalingRule implements IAutoScalingRule {
	private Logger log = Logger.getLogger(LogicalQScalingRule.class);
	//The key ID
	private long ID = 0;
	//Defines the max number of producer per exchange limit
	private int producerPerXchangeLimit=100;
	//Defines the max throughput per exchange limit
	private int througputPerXchangeLimit=100;
	
	/**
	 * @param exchangeNumberLimit the exchange limit
	 * @param producerPerXchangeLimit the producer number limit per exchange limit
	 * @param througputPerXchangeLimit the max througput per exchange limit
	 */
	public LogicalQScalingRule(int producerPerXchangeLimit, int througputPerXchangeLimit) {
		super();
		this.producerPerXchangeLimit = producerPerXchangeLimit;
		this.througputPerXchangeLimit = througputPerXchangeLimit;
	}


	/**
	 * @see org.roqmessaging.management.config.scaling.IAutoScalingRule#isOverLoaded(java.util.HashMap)
	 */
	public boolean isOverLoaded(HashMap<String, Double> context) {
			Double xchanges = context.get(RoQConstantInternal.CONTEXT_KPI_Q_XCHANGE_NUMBER);
			if (this.producerPerXchangeLimit != 0) {
				Double producer = context.get(RoQConstantInternal.CONTEXT_KPI_Q_PRODUCER_NUMBER);
				Double prodPerExchange = producer/xchanges;
				if(prodPerExchange>(this.producerPerXchangeLimit)){
					log.info("Trigger the autoscaling rule because the producer per exchange ="+ prodPerExchange);
					log.info("For the rule " +this.toString());
					return true;
				}
			}
			if (this.througputPerXchangeLimit != 0) {
				Double througput = context.get(RoQConstantInternal.CONTEXT_KPI_Q_THROUGPUT);
				Double througPerExchange = througput/xchanges;
				if(througPerExchange>(this.througputPerXchangeLimit)){
					log.info("Trigger the autoscaling rule because the througput per exchange ="+througPerExchange);
					log.info("For the rule " +this.toString());
					return true;
				}
			}


		return false;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Logical Q String [ Producer limit per Exchange: "+ this.producerPerXchangeLimit+", Throughput limit per exchange:"+ this.througputPerXchangeLimit+"]";
	}


		/**
	 * @return the producerNumber
	 */
	public int getProducerNumber() {
		return producerPerXchangeLimit;
	}


	/**
	 * @param producerNumber the producerNumber to set
	 */
	public void setProducerNumber(int producerNumber) {
		this.producerPerXchangeLimit = producerNumber;
	}


	/**
	 * @return the througputNumber
	 */
	public int getThrougputNumber() {
		return througputPerXchangeLimit;
	}


	/**
	 * @param througputNumber the througputNumber to set
	 */
	public void setThrougputNumber(int througputNumber) {
		this.througputPerXchangeLimit = througputNumber;
	}


	/**
	 * @see org.roqmessaging.management.config.scaling.IAutoScalingRule#getID()
	 */
	public long getID() {
		return this.ID;
	}


	/**
	 * @param iD the iD to set
	 */
	public void setID(long iD) {
		ID = iD;
	}

}
