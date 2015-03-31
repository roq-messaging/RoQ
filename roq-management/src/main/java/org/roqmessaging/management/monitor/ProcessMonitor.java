package org.roqmessaging.management.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstantInternal;
import org.roqmessaging.factory.HostProcessFactory;
import org.roqmessaging.management.config.internal.HostConfigDAO;
import org.roqmessaging.utils.LocalState;
import org.roqmessaging.utils.Time;

import com.google.common.collect.ImmutableList;

public class ProcessMonitor implements Runnable {
	Logger logger = Logger.getLogger(ProcessMonitor.class);
	
	private HostConfigDAO properties = null;
	// used to shutdown thread
	public volatile boolean isRunning = true;
	
	private String LocalStatePath;
	
	/**
	 * The maps/lists to manage the processes
	 * the keys are often the port of the process because
	 * it is unique for a given host
	 */
	private HashMap<String, MonitoredProcess> processes = 	  new HashMap<String, MonitoredProcess>();
	private HashMap<String, Integer> processesToAdd = new HashMap<String, Integer>();
	private ArrayList<String> processesFailed =		  new ArrayList<String>();
	private ArrayList<String> processesRunning = 	  new ArrayList<String>();
	
	// Lock to avoid race condition when modifying processesToAdd Map
	private ReentrantLock processLock = new ReentrantLock();
	
	// Allows the thread to start/stop processes
	private HostProcessFactory processFactory;
	
	/**
	 * A class to represent a monitored
	 * process
	 */
	private class MonitoredProcess {
		private final Process process;
		private final HashMap<String, String> keys;
		private final int type;
		
		public MonitoredProcess(Process process, HashMap<String, String> keys, int type) {
			this.process = process;
			this.keys = keys;
			this.type = type;
		}
		
		public Process getProcess() {
			return process;
		}
		
		public HashMap<String, String> getKeys() {
			return keys;
		}
		
		public int getType() {
			return type;
		}
		
	}
	
	public ProcessMonitor(String LocalStatePath, HostConfigDAO properties, HostProcessFactory processFactory) 
				throws IOException {
		super();
		this.LocalStatePath = LocalStatePath;
		this.properties = properties;
		this.processFactory = processFactory;
	}
	
	public void addprocess(String id, Process process, int type, HashMap<String, String> keys) {
		logger.info("adding process: " + id + " in process monitoring system");
		processLock.lock();
		processes.put(id, new MonitoredProcess(process, keys, type));
		processesToAdd.put(id, Time.currentTimeSecs());
		processLock.unlock();
	}
	
	/**
	 * Get the worker heartbeats in a map
	 * @throws IOException 
	 * 
	 *  
	 */
	public HashMap<String, Long> getprocessesHB() 
			throws IOException {
		LocalState localState;
		HashMap<String, Long> processHbs = new HashMap<String, Long>();
		for (String process : processesRunning) {
			localState = new LocalState(LocalStatePath + "/" + process);
			processHbs.put(process ,(Long) localState.get("HB"));
		}
		return processHbs;
	}

	@Override
	/**
	 * Thread loop can be stopped
	 * by setting isRunning to false
	 */
	public void run() {
		HashMap<String, Long> processesHB;
		logger.info("Starting heartbeat monitor");
		while (isRunning) {
			try {
				logger.info("check up loop");
				// Add the just spawned process
				// to the running list when
				// max start time has elapsed
				startProcessMonitoring();
				// Get heartbeat of each running process
				processesHB = getprocessesHB();
				// reclassify process according to
				// timeout
				classifyProcesses(processesHB);
				// Restart timed out processes
				restartFailedProcesses();
				// Dont waste resources
				Thread.sleep((properties.getMonitorTimeOut() * 1000) / 2);
			} catch (IOException e) {
				logger.error("Failed to read locState DB");
				e.printStackTrace();
			} catch (InterruptedException e) {
				logger.error("Thread interrupted");
				e.printStackTrace();
			}
		}
		logger.info("heartbeat monitor stopped");
	}
	
	/***
	 * Add the just started processes in the running list
	 * when the max time to start had elapsed
	 */
	private void startProcessMonitoring() {
		processLock.lock();
		ImmutableList<String> processesList = ImmutableList.copyOf(processesToAdd.keySet());
		for (String processID: processesList) {
			if (((processes.get(processID).getType() == RoQConstantInternal.PROCESS_MONITOR)
						&& (Time.currentTimeSecs() - processesToAdd.get(processID)) > properties.getMonitorMaxTimeToStart())
				|| ((processes.get(processID).getType() == RoQConstantInternal.PROCESS_EXCHANGE)
						&& (Time.currentTimeSecs() - processesToAdd.get(processID)) > properties.getExchangeMaxTimeToStart())
				|| ((processes.get(processID).getType() == RoQConstantInternal.PROCESS_SCALING)
						&& (Time.currentTimeSecs() - processesToAdd.get(processID)) > properties.getScalingProcessMaxTimeToStart()))
			{
				logger.info("begin to monitor: " + processID);
				processesToAdd.remove(processID);
				processesRunning.add(processID);
			}
		}
		processLock.unlock();
	}
	
	/**
	 * Move the processes which has time out in
	 * the failed list
	 * @param processesHB
	 */
	private void classifyProcesses(HashMap<String, Long> processesHB) {
		int current = Time.currentTimeSecs();
		ImmutableList<String> processesList = ImmutableList.copyOf(processesRunning);
		for (String processID : processesList) {
			logger.info("check: " + processID);
			if ((processesHB.get(processID) == null)
				|| ((processes.get(processID).getType() == RoQConstantInternal.PROCESS_MONITOR)
							&& (current - processesHB.get(processID)) > properties.getMonitorTimeOut())
				|| ((processes.get(processID).getType() == RoQConstantInternal.PROCESS_EXCHANGE)
							&& (current - processesHB.get(processID)) > properties.getExchangeTimeOut())
				|| ((processes.get(processID).getType() == RoQConstantInternal.PROCESS_SCALING)
							&& (current - processesHB.get(processID)) > properties.getScalingProcessTimeOut()))
			{
				logger.info("process: " + processID + " has timed out");
				processesFailed.add(processID);
				processesRunning.remove(processID);
			} else {
				logger.info("process: " + processID + " has sent hb");
			}
		}
	}
	
	/**
	 * Restart the processes which failed
	 * @param processesHB
	 */
	private void restartFailedProcesses() {
		ImmutableList<String> processesList = ImmutableList.copyOf(processesFailed);
		HashMap<String, String> keys;
		int type;
		for (String processID : processesList) {
			logger.info("restarting process: " + processID);
			keys = processes.get(processID).getKeys();
			type = processes.get(processID).getType();
			stopAndRemoveProcess(processID);
			restartProcess(type, keys);
		}
	}
	
	/**
	 * Stop the process and removes it from the list
	 */
	private void stopAndRemoveProcess(String processID) {
		MonitoredProcess process;
		process = processes.get(processID);
		// kill the process
		process.getProcess().destroy();
		processesFailed.remove(processID);
		processes.remove(processID);
	}
	
	/**
	 * Restart a process
	 * @param processesHB
	 */
	private void restartProcess(int type,HashMap<String, String> keys) {
		switch (type) {
		case RoQConstantInternal.PROCESS_MONITOR:
			processFactory.startNewMonitorProcess(keys.get("qName"));
			break;
		case RoQConstantInternal.PROCESS_SCALING:
			processFactory.startNewScalingProcess(keys.get("qName"));
			break;
		case RoQConstantInternal.PROCESS_EXCHANGE:
			processFactory.startNewExchangeProcess(keys.get("qName"), keys.get("transID"), true);
			break;
		}
	}

	/**
	 * This method kill all the processes for a specific type
	 * 
	 * @WARNING
	 * This method is only used for test
	 * purpose, don't use it if you don't
	 * know what you do, the process monitor
	 * normally runs autonomously.
	 * @param type
	 * @return true if one or more processes killed
	 */
	public boolean killProcess(int type) {
		boolean processDestroyed = false;
		for (MonitoredProcess process : processes.values()) {
			if (process.getType() == type) {
				process.getProcess().destroy();
				processDestroyed = true;
			}
		}
		return processDestroyed;
	}
}
