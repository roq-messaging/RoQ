package org.roqmessaging.management.monitor;

import java.util.HashMap;
import java.util.Set;

public class HcmState {
	// [qName, the monitor]
	private HashMap<String, String> qMonitorMap = null;
	// [qName, monitor stat server address]
	private HashMap<String, String> qMonitorStatMap = null;
	// [qName, list of Xchanges]
	private HashMap<String, HashMap<String, String>> qExchangeMap = null;
	//[qName, Scaling process shutdown port (on the same machine as host)] 
	//TODO starting the process, register it and deleting it when stoping
	private HashMap<String, Integer> qScalingProcessAddr = null;
	
	/**
	 * Class constructor to init the map
	 */
	public HcmState() {
		// Init the map
		this.qExchangeMap = new HashMap<String, HashMap<String, String>>();
		this.qMonitorMap = new HashMap<String, String>();
		this.qMonitorStatMap = new HashMap<String, String>();
		this.qScalingProcessAddr = new HashMap<String, Integer>();
	}
	
	public void removeExchange(String qName) {
		this.qExchangeMap.remove(qName);
	}
	
	public void removeMonitor(String qName) {
		this.qMonitorMap.remove(qName);
	}
	
	public void removeStat(String qName) {
		this.qMonitorStatMap.remove(qName);
	}
	
	public void removeScalingProcess(String qName) {
		this.qScalingProcessAddr.remove(qName);
	}
	
	public void putExchange(String qName, String id, String exchangeAddress) {
		if (qExchangeMap.containsKey(qName))
			this.qExchangeMap.get(qName).put(id, exchangeAddress);
		else {
			HashMap<String, String> exchange = new HashMap<String, String>();
			exchange.put(id, exchangeAddress);
			this.qExchangeMap.put(qName, exchange);
		}
	}
	
	public void putMonitor(String qName, String monitorAddress) {
		this.qMonitorMap.put(qName, monitorAddress);
	}
	
	public void putStat(String qName, String StateMonitorAddress) {
		this.qMonitorStatMap.put(qName, StateMonitorAddress);
	}
	
	public void putScalingProcess(String qName, int scalingProcessAddress) {
		this.qScalingProcessAddr.put(qName, scalingProcessAddress);
	}
	
	public HashMap<String, String> getExchanges(String qName) {
		return this.qExchangeMap.get(qName);
	}
	
	public String getMonitor(String qName) {
		return this.qMonitorMap.get(qName);
	}
	
	public String getStat(String qName) {
		return this.qMonitorStatMap.get(qName);
	}
	
	public Integer getScalingProcess(String qName) {
		return this.qScalingProcessAddr.get(qName);
	}	
	
	public Set<String> getAllMonitors() {
		return this.qMonitorMap.keySet();
	}
	
	public Set<String> getAllExchanges() {
		return this.qExchangeMap.keySet();
	}

	public boolean scalingProcessExists(String qName) {
		return qScalingProcessAddr.containsKey(qName);
	}
	
	public boolean MonitorExists(String qName) {
		return qMonitorMap.containsKey(qName);
	}
	
	public boolean statExists(String qName) {
		return qMonitorStatMap.containsKey(qName);
	}
	
	public boolean ExchangeExists(String qName, String id) {
		if (qExchangeMap.containsKey(qName))
			return qExchangeMap.get(qName).containsKey(id);
		else return false;
	}
	
	public boolean ExchangeExists(String qName) {
		return qExchangeMap.containsKey(qName);
	}
}
