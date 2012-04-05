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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.launcher.ExchangeLauncher;
import org.roqmessaging.core.launcher.MonitorLauncher;
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
public class HostConfigManager implements Runnable {

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
				}
			}
		}
		this.clientReqSocket.close();
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
		int frontPort = this.baseFrontPort + number;
		int backPort = this.baseBackPort + number;
		String ip = RoQUtils.getInstance().getLocalIP();
		String argument = frontPort + " " + backPort + " tcp://" + ip + ":"
				+ (this.baseMonitortPort + this.qMonitorMap.size()) + " tcp://" + ip + ":"
				+ (this.baseStatPort + this.qMonitorMap.size());
		logger.info(" Starting Xchange script with " + argument);
		// 2. Launch script
		//ProcessBuilder pb = new ProcessBuilder(this.exchangeScript, argument);
		
		ProcessBuilder pb = new ProcessBuilder("java"
				," -Djava.library.path="+System.getProperty("java.library.path")
				," -cp "+System.getProperty("java.class.path")
				,ExchangeLauncher.class.getCanonicalName()
				,argument);
		
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
		int number = this.qMonitorMap.size();
		int frontPort = this.baseMonitortPort + (number * 4);
		int statPort = this.baseStatPort + number;
		logger.debug(" This host contains already )" + number + " Monitor");
		String argument = frontPort + " " + statPort;
		logger.debug("Starting monitor process by script launch on " + argument);
		
		// 2. Launch script
		ProcessBuilder pb = new ProcessBuilder("java"
				," -Djava.library.path="+System.getProperty("java.library.path")
				," -cp "+System.getProperty("java.class.path")
				,MonitorLauncher.class.getCanonicalName()
				,argument);
		
//		logger.debug("env:"+pb.environment());
		logger.debug("Starting: "+pb.command());
		
		try {
			final Process process = pb.start();
		    pipe(process.getErrorStream(), System.err);
		    pipe(process.getInputStream(), System.out);
			
			
			String monitorAddress = "tcp://"+ RoQUtils.getInstance().getLocalIP()+":" + frontPort;
			String statAddress= "tcp://"+ RoQUtils.getInstance().getLocalIP()+":" + statPort;
			this.qMonitorMap.put(qName, (monitorAddress));
			return monitorAddress+","+statAddress;
		} catch (IOException e) {
			logger.error("Error while executing script", e);
			return null;
		}
	}

	private static void pipe(final InputStream src, final PrintStream dest) {
	    new Thread(new Runnable() {
	        public void run() {
	            try {
	                byte[] buffer = new byte[1024];
	                for (int n = 0; n != -1; n = src.read(buffer)) {
	                    dest.write(buffer, 0, n);
	                }
	            } catch (IOException e) { // just exit
	            }
	        }
	    }).start();
	}

	
	
	/**
	 * 
	 */
	public void shutDown() {
		this.running = false;
	}

}
