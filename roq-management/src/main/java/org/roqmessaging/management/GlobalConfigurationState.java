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
package org.roqmessaging.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.zeromq.ZMQ;

/**
 * Class GlobalConfigurationState
 * <p> Description: Define the state of the global configuration. Then it provides an encapsulation mechanism for
 *  all objects that want to get the global configuration. This cntralized the information and the access to the information.
 * 
 * @author sskhiri
 */
public class GlobalConfigurationState {
	// [Host manager address, socket>
	protected HashMap<String, ZMQ.Socket> hostManagerMap;
	// QName, monitor location
	protected HashMap<String, String> queueMonitorMap = null;
	//QName, target host location
	protected HashMap<String, String> queueHostLocation = null;
	//QName, stat monitor address server (ready to connect!)
	protected HashMap<String, String> queueMonitorStatMap = null;
	//Configuration data: list of host manager (1 per RoQ Host)
	protected ArrayList<String> hostManagerAddresses = null;
	//QName, stat monitor address server (ready to connect!)
	protected HashMap<String, List<String>> queueBUMonitorMap = null;
	protected HashMap<String, List<String>> queueBUMonitorHostMap = null;
	//utils
	protected RoQSerializationUtils serializationUtils=null;

	/**
	 * @param configurationServer the global configuration server address
	 */
	public GlobalConfigurationState() {
		this.serializationUtils = new RoQSerializationUtils();
		
		//init map
		this.hostManagerMap = new HashMap<String, ZMQ.Socket>();
		this.queueHostLocation = new HashMap<String, String>();
		this.queueMonitorMap = new HashMap<String, String>();
		this.queueBUMonitorMap = new HashMap<String, List<String>>();
		this.queueBUMonitorHostMap = new HashMap<String, List<String>>();
		this.queueMonitorStatMap = new HashMap<String, String>();
		this.hostManagerAddresses = new ArrayList<String>();
	}
	
	/**
	 * @return the hostManagerMap
	 */
	public HashMap<String, ZMQ.Socket> getHostManagerMap() {
		return hostManagerMap;
	}

	/**
	 * @param hostManagerMap the hostManagerMap to set
	 */
	public void setHostManagerMap(HashMap<String, ZMQ.Socket> hostManagerMap) {
		this.hostManagerMap = hostManagerMap;
	}

	/**
	 * @return the queueMonitorMap
	 */
	public HashMap<String, String> getQueueMonitorMap() {
		return queueMonitorMap;
	}

	/**
	 * @param queueMonitorMap the queueMonitorMap to set
	 */
	public void setQueueMonitorMap(HashMap<String, String> queueMonitorMap) {
		this.queueMonitorMap = queueMonitorMap;
	}

	/**
	 * @return the queueHostLocation
	 */
	public HashMap<String, String> getQueueHostLocation() {
		return queueHostLocation;
	}

	/**
	 * @param queueHostLocation the queueHostLocation to set
	 */
	public void setQueueHostLocation(HashMap<String, String> queueHostLocation) {
		this.queueHostLocation = queueHostLocation;
	}

	/**
	 * @return the queueMonitorStatMap
	 */
	public HashMap<String, String> getQueueMonitorStatMap() {
		return queueMonitorStatMap;
	}

	/**
	 * @param queueMonitorStatMap the queueMonitorStatMap to set
	 */
	public void setQueueMonitorStatMap(HashMap<String, String> queueMonitorStatMap) {
		this.queueMonitorStatMap = queueMonitorStatMap;
	}
	
	/**
	 * @return the queueMonitorStatMap
	 */
	public HashMap<String, List<String>> getQueueBUMonitorMap() {
		return queueBUMonitorMap;
	}

	/**
	 * @param queueMonitorStatMap the queueMonitorStatMap to set
	 */
	public void setQueueBUMonitorMap(HashMap<String, List<String>> queueBUMonitorMap) {
		this.queueBUMonitorMap = queueBUMonitorMap;
	}
	
	/**
	 * @return the queueMonitorStatMap
	 */
	public HashMap<String, List<String>> getQueueBUMonitorHostMap() {
		return queueBUMonitorHostMap;
	}

	/**
	 * @param queueMonitorStatMap the queueMonitorStatMap to set
	 */
	public void setQueueBUMonitorHostMap(HashMap<String, List<String>> queueBUMonitorHostMap) {
		this.queueBUMonitorHostMap = queueBUMonitorHostMap;
	}

	/**
	 * @return the hostManagerAddresses
	 */
	public ArrayList<String> getHostManagerAddresses() {
		return hostManagerAddresses;
	}

	/**
	 * @param hostManagerAddresses the hostManagerAddresses to set
	 */
	public void setHostManagerAddresses(ArrayList<String> hostManagerAddresses) {
		this.hostManagerAddresses = hostManagerAddresses;
	}

}
