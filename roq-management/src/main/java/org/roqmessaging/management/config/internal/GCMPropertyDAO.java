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
package org.roqmessaging.management.config.internal;

import org.roqmessaging.management.zookeeper.RoQZooKeeperConfig;

/**
 * Class GCMPropertyDAO
 * <p> Description: data object representing the file configuration of the GCM.
 * 
 * @author sskhiri
 */
public class GCMPropertyDAO {
	
	//The frequency period for the update to the configuration server
	private int period = 60000;
	// Time for the GCM to respond
	private int hcmTIMEOUT = 5000;
	//Define whether we format the DB
	private boolean formatDB = false;
	// Indicates if the master must register the cloud configuration
	private boolean hasCloudConfiguration = false;
	// Class which contains the ports used for the various components of the GCM.
	public GCMPorts ports = new GCMPorts();
	// ZooKeeper configuration
	public RoQZooKeeperConfig zkConfig = new RoQZooKeeperConfig();
	
	/**
	 * @return the period
	 */
	public int getPeriod() {
		return period;
	}

	/**
	 * @param period the period to set
	 */
	public void setPeriod(int period) {
		this.period = period;
	}
	
	/**
	 * @return the period
	 */
	public int gethcmTIMEOUT() {
		return hcmTIMEOUT;
	}

	/**
	 * @param period the period to set
	 */
	public void sethcmTIMEOUT(int hcmTIMEOUT) {
		this.hcmTIMEOUT = hcmTIMEOUT;
	}
	
	/**
	 * @return hasCloudConfiguration
	 */
	public boolean hasCloudConfiguration() {
		return hasCloudConfiguration;
	}
	
	/**
	 * @param hasCloudConfiguration
	 */
	public void hasCloudConfiguration(boolean hasCloudConfiguration) {
		this.hasCloudConfiguration = hasCloudConfiguration;
	}
	
	/**
	 * @return the formatDB
	 */
	public boolean isFormatDB() {
		return formatDB;
	}

	/**
	 * @param formatDB the formatDB to set
	 */
	public void setFormatDB(boolean formatDB) {
		this.formatDB = formatDB;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GCM "+ this.getPeriod()+" formatDB "+ this.isFormatDB();
	}
}
