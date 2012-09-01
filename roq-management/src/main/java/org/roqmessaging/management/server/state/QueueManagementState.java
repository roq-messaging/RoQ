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
package org.roqmessaging.management.server.state;

/**
 * Class QueueManagementState
 * <p> Description: define the state of a logical queue seen by the management
 * 
 * @author sskhiri
 */
public class QueueManagementState {
	//The queue name
	private String name = null;
	//The host address on which the monitor runs
	private String host = null;
	//Is it running
	private boolean running = false;
	//The auto-scaling configuration
	private int autoScalingCfgRef =0;
	
	
	/**
	 * @param name the Q name
	 * @param host the physical host on which the monitor runs
	 * @param running define whether the Q runs
	 */
	public QueueManagementState(String name, String host, boolean running, int autoScalingCfg) {
		super();
		this.name = name;
		this.host = host;
		this.running = running;
		this.autoScalingCfgRef= autoScalingCfg;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}
	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return running;
	}
	/**
	 * @param running the running to set
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Queue: " + this.name +" running on "+ this.host + " is "+ (isRunning()?"ON":"OFF");
	}
	/**
	 * @return the autoScalingCfgRef
	 */
	public int getAutoScalingCfgRef() {
		return autoScalingCfgRef;
	}
	/**
	 * @param autoScalingCfgRef the autoScalingCfgRef to set
	 */
	public void setAutoScalingCfgRef(int autoScalingCfgRef) {
		this.autoScalingCfgRef = autoScalingCfgRef;
	}
	
	

}
