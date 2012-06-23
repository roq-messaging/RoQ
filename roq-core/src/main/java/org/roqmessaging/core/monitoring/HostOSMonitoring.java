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
package org.roqmessaging.core.monitoring;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import org.apache.log4j.Logger;

/**
 * Class HostOSMonitoring
 * <p> Description: Facility class that monitor the memory and CPU usage of the host OS.
 * 
 * @author sskhiri
 */
public class HostOSMonitoring {
	Logger logger = Logger.getLogger(HostOSMonitoring.class);
	
	/**
	 * @return the current memory usage
	 */
	public double getMemoryUsage(){
		double total = (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024);
		double currentMemory =total- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
		logger.debug("Current memory " +currentMemory +" Mo  on " +total +" Mo" );
		return currentMemory;
	}
	
	/**
	 * @return Returns the system load average for the last minute.
	 *  The system load average is the sum of the number of runnable entities queued to the available processors and the number of runnable entities running on the available processors averaged over a period of time. The way in which the load average is calculated is
	 *  operating system specific but is typically a damped time-dependent average.
	 */
	public double getCPUUsage() {
		// Using the JMX bean
		OperatingSystemMXBean threadBean = ManagementFactory.getOperatingSystemMXBean();
		double load =  threadBean.getSystemLoadAverage();
		logger.info("CPU usage this last minute:  " +load +" %");
		return  load;
	}

}
