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
package org.roqmessaging.management.usage;

import java.util.HashMap;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roqmessaging.core.RoQConstantInternal;
import org.roqmessaging.core.monitoring.HostOSMonitoring;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;

/**
 * Class TestJVMStatus
 * <p> Description: Test the method to get the CPU and memory usage
 * 
 * @author sskhiri
 */
public class TestJVMStatus {
	Logger logger = Logger.getLogger(TestJVMStatus.class);

	@Test
	public void test() {
		HostOSMonitoring monitor = new HostOSMonitoring();
		monitor.getMemoryUsage();
		monitor.getCPUUsage();
	}
	
	@Test
	public void testScalingCPURule() throws Exception {
		HostScalingRule rule = new HostScalingRule(35, 2);
		//Sumulating context
		HashMap<String, Double> ctx = new HashMap<String, Double>();
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_CPU, new Double(4));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_RAM, new Double(32));
		Assert.assertEquals(true, rule.isOverLoaded(ctx));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_CPU, new Double(1.5));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_RAM, new Double(37));
		Assert.assertEquals(true, rule.isOverLoaded(ctx));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_CPU, new Double(1.5));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_RAM, new Double(34.5));
		Assert.assertEquals(false, rule.isOverLoaded(ctx));
		//Testing 0 value
		rule = new HostScalingRule(0, 2);
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_CPU, new Double(1.5));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_HOST_RAM, new Double(37));
		Assert.assertEquals(false, rule.isOverLoaded(ctx));
	}
	
	@Test
	public void testLogicalQScalingRule() throws Exception {
		LogicalQScalingRule rule = new LogicalQScalingRule(1000, 1000);
		HashMap<String, Double> ctx = new HashMap<String, Double>();
		ctx.put(RoQConstantInternal.CONTEXT_KPI_Q_XCHANGE_NUMBER, new Double(10));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_Q_PRODUCER_NUMBER, new Double(20000));
		ctx.put(RoQConstantInternal.CONTEXT_KPI_Q_THROUGPUT, new Double(1000000));
		Assert.assertEquals(true, rule.isOverLoaded(ctx));
		
		ctx.put(RoQConstantInternal.CONTEXT_KPI_Q_XCHANGE_NUMBER, new Double(100));
		Assert.assertEquals(true, rule.isOverLoaded(ctx));
		
		ctx.put(RoQConstantInternal.CONTEXT_KPI_Q_XCHANGE_NUMBER, new Double(1000));
		Assert.assertEquals(false, rule.isOverLoaded(ctx));
	}

}
