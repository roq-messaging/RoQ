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
package org.roqmessaging.core;

/**
 * Interface RoQConstantInternal
 * <p> Description: Same role as ROQConstant but only for internal usage.
 * 
 * @author Sabri Skhiri
 */
public interface RoQConstantInternal {
	/**
	 * Used in the autoscaling policy configuration, defines the CPU KPI
	 */
	public static String CONTEXT_KPI_HOST_CPU="context.kpi.host.cpu";
	
	/**
	 * Used in the autoscaling configuration, defines the memory occupation KPI
	 */
	public static String CONTEXT_KPI_HOST_RAM="context.kpi.host.ram";
	
	/**
	 * Used in the autoscaling configuration, defines the number of messages the passing through the exchange
	 * the last minute. 
	 */
	public static String CONTEXT_KPI_XCHANGE_EVENTS="context.kpi.xchange.events";

	/**
	 * Used in the autoscaling configuration, defines the time spent afterwards you need to start a new exchange
	 */
	public static String CONTEXT_KPI_XCHANGE_TIME="context.kpi.xchange.time";
	
	/**
	 * Used in the autoscaling configuration, defines the number of exchanges per logical queue.
	 */
	public static String CONTEXT_KPI_Q_XCHANGE_NUMBER="context.kpi.queue.exchange.number";

	/**
	 * Used in the autoscaling configuration, defines the number of producers per logical queue.
	 */
	public static String CONTEXT_KPI_Q_PRODUCER_NUMBER = "context.kpi.queue.producer.number";

	/**
	 * Used in the autoscaling configuration, defines the throuput  per logical queue.
	 */
	public static String CONTEXT_KPI_Q_THROUGPUT = "context.kpi.queue.througput";
	
	/**
	 * Used to idenify Monitor process
	 */
	public static int PROCESS_MONITOR = 0;
	
	/**
	 * Used to idenify Monitor process
	 */
	public static int PROCESS_EXCHANGE = 2;
	
	/**
	 * Used to idenify Monitor process
	 */
	public static int PROCESS_SCALING = 3;
	
}
