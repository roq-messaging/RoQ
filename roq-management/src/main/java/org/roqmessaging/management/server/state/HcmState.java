package org.roqmessaging.management.server.state;

import java.util.HashMap;
import java.util.Set;

public class HcmState {
	// Allow to calculate the port for the next monitor
	// this variable is only incremented, never decremented
	// in order to avoid port collisions
	private int portMultiplicator = 0;
	private int exchangesPortMultiplicator = 0;
	// [qName, the monitor]
	private HashMap<String, String> qMonitorMap = null;
	// [qName, monitor stat server address]
	private HashMap<String, String> qMonitorStatMap = null;
	// [qName, the monitor]
	private HashMap<String, String> qSTBYMonitorMap = null;
	// [qName, monitor stat server address]
	private HashMap<String, String> qSTBYMonitorStatMap = null;
	// [qName, [XchangesID, Address]]
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
		this.qSTBYMonitorMap = new HashMap<String, String>();
		this.qSTBYMonitorStatMap = new HashMap<String, String>();
		this.qScalingProcessAddr = new HashMap<String, Integer>();
	}
	
	public int getPortMultiplicator() {
		return this.portMultiplicator;
	}
	
	public int getExchangesPortMultiplicator() {
		return this.exchangesPortMultiplicator;
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
	
	public void removeSTBYMonitor(String qName) {
		this.qSTBYMonitorMap.remove(qName);
	}
	
	public void removeSTBYStat(String qName) {
		this.qSTBYMonitorStatMap.remove(qName);
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
		this.exchangesPortMultiplicator++;
	}
	
	public void putMonitor(String qName, String monitorAddress) {
		this.qMonitorMap.put(qName, monitorAddress);
		this.portMultiplicator++;
	}
	
	public void putStat(String qName, String StateMonitorAddress) {
		this.qMonitorStatMap.put(qName, StateMonitorAddress);
	}
	
	public void putSTBYMonitor(String qName, String monitorAddress) {
		this.qSTBYMonitorMap.put(qName, monitorAddress);
		this.portMultiplicator++;
	}
	
	public void putSTBYStat(String qName, String StateMonitorAddress) {
		this.qSTBYMonitorStatMap.put(qName, StateMonitorAddress);
	}
	
	public void putScalingProcess(String qName, int scalingProcessAddress) {
		this.qScalingProcessAddr.put(qName, scalingProcessAddress);
	}
	
	public HashMap<String, String> getExchanges(String qName) {
		return this.qExchangeMap.get(qName);
	}
	
	public String getExchange(String qName, String transID) {
		if (!ExchangeExists(qName))
			return null;
		else
			return this.qExchangeMap.get(qName).get(transID);
	}
	
	public String getMonitor(String qName) {
		return this.qMonitorMap.get(qName);
	}
	
	public String getStat(String qName) {
		return this.qMonitorStatMap.get(qName);
	}
	
	public String getSTBYMonitor(String qName) {
		return this.qSTBYMonitorMap.get(qName);
	}
	
	public String getSTBYStat(String qName) {
		return this.qSTBYMonitorStatMap.get(qName);
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
	
	public boolean MonitorSTBYExists(String qName) {
		return qSTBYMonitorMap.containsKey(qName);
	}
	
	public boolean statSTBYExists(String qName) {
		return qSTBYMonitorStatMap.containsKey(qName);
	}
	
	public boolean ExchangeExists(String qName, String id) {
		if (qExchangeMap.containsKey(qName))
			return qExchangeMap.get(qName).containsKey(id);
		else return false;
	}
	
	public boolean ExchangeExists(String qName) {
		return qExchangeMap.containsKey(qName);
	}
	
	/**
	 * This function transfer the monitor from standbylist to
	 * master list without modifying the portMultiplicator
	 * @param qName
	 */
	public void switchToMaster(String qName) {
		String monitorAddress = this.qSTBYMonitorMap.get(qName);
		this.qSTBYMonitorMap.remove(qName);
		this.qMonitorMap.put(qName, monitorAddress);
		String monitorStatAddress = this.qSTBYMonitorStatMap.get(qName);
		this.qSTBYMonitorStatMap.remove(qName);
		this.qMonitorStatMap.put(qName, monitorStatAddress);
	}
}
