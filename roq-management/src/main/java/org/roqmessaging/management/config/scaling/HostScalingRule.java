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

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstantInternal;

/**
 * Class HostScalingRule
 * <p> Description: autoscaling rule based on physical host KPI. In the RoQ case we consider the KPI 
 * from the {@linkplain HostConfigManager}
 * 
 * @author sskhiri
 */
public class HostScalingRule implements IAutoScalingRule, Serializable {
	//Logger
	private static final Logger logger = Logger.getLogger(HostScalingRule.class);
	//KPI on RAM memory
	public int RAM_Limit = 100;
	//KPI on CPU
	public int CPU_Limit = 100;
	//The key ID
	private long ID = 0;
	
	/**
	 * @param rAM_Limit the ram memory to not over load. if 0 it will  not be considered. The value 
	 * must be in Mo
	 * @param cPU_Limit the cpu limit to not overload. If 0 it will  not be considered. The value must in %
	 */
	public HostScalingRule(int rAM_Limit, int cPU_Limit) {
		super();
		RAM_Limit = rAM_Limit;
		CPU_Limit = cPU_Limit;
	}

	/**
	 * @return the rAM_Limit
	 */
	public int getRAM_Limit() {
		return RAM_Limit;
	}

	/**
	 * @param rAM_Limit the rAM_Limit to set
	 */
	public void setRAM_Limit(int rAM_Limit) {
		RAM_Limit = rAM_Limit;
	}

	/**
	 * @return the cPU_Limit
	 */
	public int getCPU_Limit() {
		return CPU_Limit;
	}

	/**
	 * @param cPU_Limit the cPU_Limit to set
	 */
	public void setCPU_Limit(int cPU_Limit) {
		CPU_Limit = cPU_Limit;
	}

	/**
	 * Evaluate the limit
	 * @see org.roqmessaging.management.config.scaling.IAutoScalingRule#isOverLoaded(java.util.HashMap)
	 */
	public boolean isOverLoaded(HashMap<String, Double> context) {
		Double cpu = context.get(RoQConstantInternal.CONTEXT_KPI_HOST_CPU);
		Double ram = context.get(RoQConstantInternal.CONTEXT_KPI_HOST_RAM);
		if (this.getCPU_Limit() != 0 && cpu!=null) {
			if (cpu.floatValue() > this.getCPU_Limit()) {
				logger.info("Host Scaling rule reached [cpu: " + cpu.floatValue() + "]");
				return true;

			}
		}
		if (this.getRAM_Limit() != 0 && ram!=null) {
			if (ram.floatValue() > this.getRAM_Limit()) {
				logger.info("Host Scaling rule reached [ram:" + ram.floatValue() + "]");
				return true;
			}
		}
		return false;
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
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Host scaling rule id = "+getID()+" [cpu:"+this.getCPU_Limit() +", ram:"+ this.getRAM_Limit()+"]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HostScalingRule other = (HostScalingRule) obj;
		if (CPU_Limit != other.CPU_Limit)
			return false;
		if (RAM_Limit != other.RAM_Limit)
			return false;
		return true;
	}
	

}
