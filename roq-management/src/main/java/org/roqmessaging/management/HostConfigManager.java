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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class HostConfigManager
 * <p>
 * Description: Responsible for the local management of the queue elements. For
 * each host it will track the list of monitors and exchanges. Notice that the
 * host config manager implement a server pattern that exposes services to the
 * monitor management.
 * 
 * @author sskhiri
 */
public class HostConfigManager implements Runnable, IStoppable {

	// ZMQ config
	private ZMQ.Socket clientReqSocket = null;
	private ZMQ.Context context;
	// Logger
	private Logger logger = Logger.getLogger(HostConfigManager.class);
	// Host manager config
	private volatile boolean running = false;
	// the base port for front port
	private int baseMonitortPort = 5500;
	private int baseStatPort = 5800;

	private int baseFrontPort = 6000;
	private int baseBackPort = 7000;
	// Local configuration maintained by the host manager
	// [qName, the monitor]
	private HashMap<String, String> qMonitorMap = null;
	// [qName, list of Xchanges]
	private HashMap<String, List<String>> qExchangeMap = null;
	private ShutDownMonitor shutDownMonitor = null;
	
	//The scripts to starts TODO defining a multi platform approach for script
	private String monitorScript = "/usr/bin/roq/startMonitor.sh";
	private String exchangeScript = "/usr/bin/roq/startXchange.sh";

	/**
	 * Constructor
	 */
	public HostConfigManager() {
		// ZMQ Init
		this.context = ZMQ.context(1);
		this.clientReqSocket = context.socket(ZMQ.REP);
		this.clientReqSocket.bind("tcp://*:5100");
		this.qExchangeMap = new HashMap<String, List<String>>();
		this.qMonitorMap = new HashMap<String, String>();
		this.shutDownMonitor = new ShutDownMonitor(5101, this);
		new Thread(this.shutDownMonitor).start();
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		this.running = true;
		// ZMQ init
		ZMQ.Poller items = context.poller(1);
		items.register(this.clientReqSocket);

		// 2. Start the main run of the monitor
		while (this.running) {
			items.poll(2000);
			if (items.pollin(0)) { // Comes from a client
				logger.debug("Receiving Incoming request @host...");
				String[] info = new String(clientReqSocket.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);
				logger.debug("Start analysing info code = " + infoCode);
				switch (infoCode) {

				// Receive a create queue request on the local host manager
				// (likely from the LogicalQFactory
				case RoQConstant.CONFIG_CREATE_QUEUE:
					logger.debug("Recieveing create Q request from a client ");
					if (info.length == 2) {
						String qName = info[1];
						logger.debug("The request format is valid with 2 parts, Q to create:  " + qName);
						if(!checkScriptInstall()){
							logger.error("The create queue request has failed: the scripts for launching Xchange & monitor have not been found ");
							this.clientReqSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL)+", ").getBytes(), 0);
						}
						String monitorAddress = startNewMonitorProcess(qName);
						boolean xChangeOK = startNewExchangeProcess(qName);
						// if OK send OK
						if (monitorAddress!=null & xChangeOK){
							logger.info("Successfully created new Q for " + qName + "@"+monitorAddress);
							this.clientReqSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_OK)+","+monitorAddress).getBytes(),	0);
						}else{
							logger.error("The create queue request has failed at the monitor host,check log (when starting launching scripts");
							this.clientReqSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL)+","+monitorAddress).getBytes(), 0);
						}
					} else {
						logger.error("The create queue request sent does not contain 3 part: ID, quName, Monitor host");
						this.clientReqSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL)+", ").getBytes(), 0);
					}
					break;
					
				case RoQConstant.CONFIG_REMOVE_QUEUE:
					logger.debug("Recieveing create Q request from a client ");
					if (info.length == 2) {
						String qName = info[1];
						removingQueue(qName);
					} else {
						logger.error("The create queue request sent does not contain 2 part: ID, quName");
						this.clientReqSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL)+", ").getBytes(), 0);
					}
					break;
				}
			}
		}
		this.clientReqSocket.close();
	}

	/**
	 * Remove a complete queue:
	 * 1. Sends a shut down request to the corresponding monitor
	 * 2. The monitor will send a shut down request to all exchanges that it knows
	 * @param qName the logical Q name to remove
	 */
	private void removingQueue(String qName) {
		String monitorAddress = this.qMonitorMap.get(qName);
		//The address is the address of the base monitor, we need to extract the port and make +5
		// to get the shutdown monitor thread
		int basePort = RoQUtils.getInstance().extractBasePort(monitorAddress);
		String portOff = monitorAddress.substring(0, monitorAddress.length()-"xxxx".length());
		logger.info("Sending Remove Q request to " + portOff+(basePort+5));
		// 2. Send the remove message to the monitor
		//The monitor will stop all the exchanges during its shut down
		ZMQ.Socket shutDownMonitor = ZMQ.context(1).socket(ZMQ.REQ);
		shutDownMonitor.setSendTimeOut(0);
		shutDownMonitor.connect(portOff+(basePort+5));
		shutDownMonitor.send((Integer.toString(RoQConstant.SHUTDOWN_REQUEST)).getBytes(), 0);
		shutDownMonitor.close();
	}

	/**
	 * @return true if the scripts are found in the expected location
	 */
	private boolean checkScriptInstall() {
		File script1 = new File(this.monitorScript);
		if(!script1.exists()) return false;
		File script2 = new File(this.exchangeScript);
		if(!script2.exists()) return false;
		return true;
	}

	/**
	 * Start a new exchange process
	 * <p>
	 * 1. Check the number of local xChange present in the host 2. Start a new
	 * xChange with port config + nchange
	 * 
	 * @param qName
	 *            the name of the queue to create
	 * @return true if the creation process worked well
	 */
	private boolean startNewExchangeProcess(String qName) {
		// 1. Get the number of installed queues on this host
		int number = 0;
		for (String q_i : this.qExchangeMap.keySet()) {
			List<String> xChanges = this.qExchangeMap.get(q_i);
			number += xChanges.size();
		}
		logger.debug(" This host contains already )" + number + " Exchanges");
		int frontPort = this.baseFrontPort + number*2;
		int backPort = this.baseBackPort + number;
		String ip = RoQUtils.getInstance().getLocalIP();
		String argument = frontPort + " " + backPort + " tcp://" + ip + ":"
				+ getMonitorPort() + " tcp://" + ip + ":"
				+ getStatMonitorPort();
		logger.info(" Starting Xchange script with " + argument);
		// 2. Launch script
		ProcessBuilder pb = new ProcessBuilder(this.exchangeScript, argument);
		try {
			pb.start();
			if(this.qExchangeMap.containsKey(qName)){
				this.qExchangeMap.get(qName).add( "tcp://" + ip + ":"+frontPort);
			}else {
				List<String> xChange = new ArrayList<String>();
				xChange.add("tcp://" + ip + ":"+frontPort);
				this.qExchangeMap.put(qName, xChange);
			}
		} catch (IOException e) {
			logger.error("Error while executing script", e);
			return false;
		}
		return true;
	}

	/**
	 * @return the monitor port
	 */
	private int getMonitorPort() {
		return (this.baseMonitortPort + this.qMonitorMap.size()*5);
	}
	
	/**
	 * @return the monitor stat  port
	 */
	private int getStatMonitorPort() {
		return (this.baseStatPort + this.qMonitorMap.size());
	}

	/**
	 * Start a new Monitor process
	 * <p>
	 * 1. Check the number of local monitor present in the host 2. Start a new
	 * monitor with port config + nMonitor*4 because the monitor needs to book 4
	 * ports + stat
	 * 
	 * @param qName
	 *            the name of the queue to create
	 * @return the monitor address as tcp://IP:port of the newly created monitor +"," tcp://IP: statport
	 */
	private String startNewMonitorProcess(String qName) {
		// 1. Get the number of installed queues on this host
		int frontPort =getMonitorPort();
		int statPort =  getStatMonitorPort();
		logger.debug(" This host contains already )" + this.qMonitorMap.size() + " Monitor");
		String argument = frontPort + " " + statPort;
		logger.debug("Starting monitor process by script launch on " + argument);
		
		// 2. Launch script
		ProcessBuilder pb = new ProcessBuilder(this.monitorScript, argument);
		try {
			pb.start();
			String monitorAddress = "tcp://"+ RoQUtils.getInstance().getLocalIP()+":" + frontPort;
			String statAddress= "tcp://"+ RoQUtils.getInstance().getLocalIP()+":" + statPort;
			this.qMonitorMap.put(qName, (monitorAddress));
			return monitorAddress+","+statAddress;
		} catch (IOException e) {
			logger.error("Error while executing script", e);
			return null;
		}
	}

	/**
	 * 
	 */
	public void shutDown() {
		this.running = false;
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "Host config manager "+RoQUtils.getInstance().getLocalIP();
	}

	/**
	 * @return the qMonitorMap
	 */
	public HashMap<String, String> getqMonitorMap() {
		return qMonitorMap;
	}

	/**
	 * @param qMonitorMap the qMonitorMap to set
	 */
	public void setqMonitorMap(HashMap<String, String> qMonitorMap) {
		this.qMonitorMap = qMonitorMap;
	}

}
