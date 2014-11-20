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
	
	//Define whether we format the DB
	private boolean formatDB = false;
	//Define whether RoQ should use a cloud or not
	private boolean useCloud= false;
	//The user  for the cloud server
	private String cloudUser = "roq";
	//PASSWD for the cloud server
	private String cloudPasswd = "roq";
	//The gateway for identifying the subnetwork gateway
	private String cloudGateWay = "??";
	//API end point for the cloud server
	private String cloudEndPoint = "??";
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

	/**
	 * @return the useCloud
	 */
	public boolean isUseCloud() {
		return useCloud;
	}

	/**
	 * @param useCloud the useCloud to set
	 */
	public void setUseCloud(boolean useCloud) {
		this.useCloud = useCloud;
	}

	/**
	 * @return the cloudUser
	 */
	public String getCloudUser() {
		return cloudUser;
	}

	/**
	 * @param cloudUser the cloudUser to set
	 */
	public void setCloudUser(String cloudUser) {
		this.cloudUser = cloudUser;
	}

	/**
	 * @return the cloudPasswd
	 */
	public String getCloudPasswd() {
		return cloudPasswd;
	}

	/**
	 * @param cloudPasswd the cloudPasswd to set
	 */
	public void setCloudPasswd(String cloudPasswd) {
		this.cloudPasswd = cloudPasswd;
	}

	/**
	 * @return the cloudGateWay
	 */
	public String getCloudGateWay() {
		return cloudGateWay;
	}

	/**
	 * @param cloudGateWay the cloudGateWay to set
	 */
	public void setCloudGateWay(String cloudGateWay) {
		this.cloudGateWay = cloudGateWay;
	}

	/**
	 * @return the cloudEndPoint
	 */
	public String getCloudEndPoint() {
		return cloudEndPoint;
	}

	/**
	 * @param cloudEndPoint the cloudEndPoint to set
	 */
	public void setCloudEndPoint(String cloudEndPoint) {
		this.cloudEndPoint = cloudEndPoint;
	}
}
