package org.roqmessaging.management.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstantInternal;
import org.roqmessaging.factory.HostProcessFactory;
import org.roqmessaging.utils.LocalState;
import org.roqmessaging.utils.Time;

public class ProcessMonitor implements Runnable{
	Logger logger = Logger.getLogger(ProcessMonitor.class);
	
	// The timeout after which process is considered a dead
	private int processTimeOut;
	private int maxTimeToStart;
	
	/**
	 * The localState Database
	 * contains HB from processes 
	 * that run on the Host
	 */
	LocalState localState;
	
	// used to shutdown thread
	public volatile boolean isRunning = true;
	
	/**
	 * The key is the port (because port is unique and allows to identify
	 * The value is the PID
	 */
	private HashMap<String, String> processesPID = 	  new HashMap<String, String>();
	private HashMap<String, Integer> processesType =  new HashMap<String, Integer>();
	private HashMap<String, Integer> processesToAdd = new HashMap<String, Integer>();
	private ArrayList<String> processesFailed =		  new ArrayList<String>();
	private ArrayList<String> processesRunning = 	  new ArrayList<String>();
	
	// Lock to avoid race condition when modifying processesToAdd Map
	private ReentrantLock processLock = new ReentrantLock();
	
	// Allows the thread to start/stop processes
	private HostProcessFactory processFactory;
	
	public ProcessMonitor(String LocalStatePath, int processTimeout, int maxTimeToStart, HostProcessFactory processFactory) 
				throws IOException {
		super();
		this.processTimeOut = processTimeout;
		this.localState = new LocalState(LocalStatePath);
		this.maxTimeToStart = maxTimeToStart;
		this.processFactory = processFactory;
	}
	
	public void addprocess(String id, String pid, int type) {
		processLock.lock();
		processesPID.put(id, pid);
		processesType.put(id, type);
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
		HashMap<String, Long> processHbs = new HashMap<String, Long>();
		for (String process : processesRunning) {
			processHbs.put(process ,(Long) localState.get(process));
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
		while (isRunning) {
			try {
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
				Thread.sleep(processTimeOut / 2);
			} catch (IOException e) {
				logger.error("Failed to read locState DB");
				e.printStackTrace();
			} catch (InterruptedException e) {
				logger.error("Thread interrupted");
				e.printStackTrace();
			}
		}
	}
	
	/***
	 * Add the just started processes in the running list
	 * when the max time to start had elapsed
	 */
	private void startProcessMonitoring() {
		processLock.lock();
		for (String processID: processesToAdd.keySet()) {
			if (processesToAdd.get(processID) > maxTimeToStart) {
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
		for (String processID : processesHB.keySet()) {
			if (current - processesHB.get(processID) > processTimeOut) {
				processesFailed.add(processID);
				processesRunning.remove(processID);
			}
		}
	}
	
	/**
	 * Restart the processes which failed
	 * @param processesHB
	 */
	private void restartFailedProcesses() {
		for (String processID : processesFailed) {
			stopProcess(processID);
			restartProcess(processID);
		}
	}
	
	/**
	 * kill a process
	 * @param processesHB
	 */
	private boolean stopProcess(String processID) {
		int processType = processesType.get(processID);
		switch (processType) {
		case RoQConstantInternal.PROCESS_MONITOR:
			
			break;
		case RoQConstantInternal.PROCESS_SCALING:
			
			break;
		case RoQConstantInternal.PROCESS_STAT:
			
			break;
		case RoQConstantInternal.PROCESS_EXCHANGE:
			
			break;
		}
		return false;
	}
	
	/**
	 * Restart a process
	 * @param processesHB
	 */
	private boolean restartProcess(String processID) {
		int processType = processesType.get(processID);
		switch (processType) {
		case RoQConstantInternal.PROCESS_MONITOR:
			
			break;
		case RoQConstantInternal.PROCESS_SCALING:
			
			break;
		case RoQConstantInternal.PROCESS_STAT:
			
			break;
		case RoQConstantInternal.PROCESS_EXCHANGE:
			
			break;
		}
		return false;
	}
	
}
