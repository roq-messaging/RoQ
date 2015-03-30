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

/**
 * Class HostConfigDAO
 * <p> Description: data object handling properties for the host configuration manager
 * 
 * @author sskhiri
 */
public class HostConfigDAO {
	private String networkInterface = null;
	private String gcmAddress = "localhost";
	private int statMonitorBasePort = 5800;
	private int monitorBasePort = 5500;
	private int exchangeFrontEndPort = 6000;
	private int statPeriod =60000;
	private int maxNumberEchanges =3;
	private boolean queueInHcmVm = true;
	private boolean exchangeInHcmVm = true;
	private int exchangeHeap = 256;
	private String statePath = "";
	private int monitorTimeOut = 10000;
	private int monitorMaxTimeToStart = 20000;
	private long monitorHbPeriod = 1000;
	
	// Class which contains the ports used for the various components of the GCM.
	public GCMPorts ports = new GCMPorts();
	
	/**
	 * @return the networkInterface
	 */
	public String getNetworkInterface() {
		return networkInterface;
	}
	/**
	 * @param networkInterface the networkInterface to set
	 */
	public void setNetworkInterface(String networkInterface) {
		this.networkInterface = networkInterface;
	}
	public void setMonitorTimeOut(int timeout) {
		monitorTimeOut = timeout;
	}
	public void setMonitorMaxTimeToStart(int timeToStart) {
		monitorMaxTimeToStart = timeToStart;
	}
	public void setExchangeTimeOut(int timeout) {
		monitorTimeOut = timeout;
	}
	public void setExchangeMaxTimeToStart(int timeToStart) {
		monitorMaxTimeToStart = timeToStart;
	}
	public void setScalingProcessTimeOut(int timeout) {
		monitorTimeOut = timeout;
	}
	public void setScalingProcessMaxTimeToStart(int timeToStart) {
		monitorMaxTimeToStart = timeToStart;
	}
	/**
	 * @return the gcmAddress
	 */
	public String getGcmAddress() {
		return gcmAddress;
	}
	/**
	 * @param gcmAddress the gcmAddress to set
	 */
	public void setGcmAddress(String gcmAddress) {
		this.gcmAddress = gcmAddress;
	}
	/**
	 * @return the statMonitorBasePort
	 */
	public int getStatMonitorBasePort() {
		return statMonitorBasePort;
	}
	/**
	 * @param statMonitorBasePort the statMonitorBasePort to set
	 */
	public void setStatMonitorBasePort(int statMonitorBasePort) {
		this.statMonitorBasePort = statMonitorBasePort;
	}
	/**
	 * @return the monitorBasePort
	 */
	public int getMonitorBasePort() {
		return monitorBasePort;
	}
	/**
	 * @param monitorBasePort the monitorBasePort to set
	 */
	public void setMonitorBasePort(int monitorBasePort) {
		this.monitorBasePort = monitorBasePort;
	}
	/**
	 * @return the exchangeFrontEndPort
	 */
	public int getExchangeFrontEndPort() {
		return exchangeFrontEndPort;
	}
	/**
	 * @param exchangeFrontEndPort the exchangeFrontEndPort to set
	 */
	public void setExchangeFrontEndPort(int exchangeFrontEndPort) {
		this.exchangeFrontEndPort = exchangeFrontEndPort;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Host configuration manager [GCM :"+gcmAddress +"] [Exhange FE :" +exchangeFrontEndPort+"] [Monitor base port: "+ monitorBasePort+"]\n" +
				" [Create Queue in the same HCM VM: "+ queueInHcmVm+"] [Create Exchange in same HCM VM: "+ exchangeInHcmVm+"] [ Exchange Heap: "+ exchangeHeap+"]";
	}
	/**
	 * @return the statPeriod
	 */
	public int getStatPeriod() {
		return statPeriod;
	}
	/**
	 * @return the localstatePath
	 */
	public String getLocalPath() {
		return statePath;
	}
	/**
	 * @param statPeriod the statPeriod to set
	 */
	public void setStatPeriod(int statPeriod) {
		this.statPeriod = statPeriod;
	}
	/**
	 * @return the maxNumberEchanges
	 */
	public int getMaxNumberEchanges() {
		return maxNumberEchanges;
	}
	/**
	 * @param maxNumberEchanges the maxNumberEchanges to set
	 */
	public void setMaxNumberEchanges(int maxNumberEchanges) {
		this.maxNumberEchanges = maxNumberEchanges;
	}
	/**
	 * @return the queueInHcmVm
	 */
	public boolean isQueueInHcmVm() {
		return queueInHcmVm;
	}
	/**
	 * @param queueInHcmVm the queueInHcmVm to set
	 */
	public void setQueueInHcmVm(boolean queueInHcmVm) {
		this.queueInHcmVm = queueInHcmVm;
	}
	/**
	 * @return the exchangeInHcmVm
	 */
	public boolean isExchangeInHcmVm() {
		return exchangeInHcmVm;
	}
	/**
	 * @param exchangeInHcmVm the exchangeInHcmVm to set
	 */
	public void setExchangeInHcmVm(boolean exchangeInHcmVm) {
		this.exchangeInHcmVm = exchangeInHcmVm;
	}
	/**
	 * @return the exchangeHeap
	 */
	public int getExchangeHeap() {
		return exchangeHeap;
	}
	/**
	 * @param exchangeHeap the exchangeHeap to set
	 */
	public void setExchangeHeap(int exchangeHeap) {
		this.exchangeHeap = exchangeHeap;
	}
	
	/**
	 * @param the path for localstateDB
	 */
	public void setLocalStatePath(String localPath) {
		this.statePath = localPath;
	}
	public int getMonitorTimeOut() {
		return monitorTimeOut;
	}
	public int getMonitorMaxTimeToStart() {
		return monitorMaxTimeToStart;
	}
	public long getMonitorHbPeriod() {
		return monitorHbPeriod;
	}
	public void setMonitorHbPeriod(long period) {
		monitorHbPeriod = period;
	}
	public int getExchangeTimeOut() {
		return monitorTimeOut;
	}
	public int getExchangeMaxTimeToStart() {
		return monitorMaxTimeToStart;
	}
	public long getExchangeHbPeriod() {
		return monitorHbPeriod;
	}
	public void setExchangeHbPeriod(long period) {
		monitorHbPeriod = period;
	}public int getScalingProcessTimeOut() {
		return monitorTimeOut;
	}
	public int getScalingProcessMaxTimeToStart() {
		return monitorMaxTimeToStart;
	}
	public long getScalingProcessHbPeriod() {
		return monitorHbPeriod;
	}
	public void setScalingProcessHbPeriod(long period) {
		monitorHbPeriod = period;
	}

}
