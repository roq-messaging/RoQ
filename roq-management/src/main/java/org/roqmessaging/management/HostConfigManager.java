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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
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
	// Local configuration maintained by the host manager
	// [qName, the monitor]
	private HashMap<String, String> qMonitorMap = null;
	// [qName, monitor stat server address]
	private HashMap<String, String> qMonitorStatMap = null;
	// [qName, list of Xchanges]
	private HashMap<String, List<String>> qExchangeMap = null;
	private ShutDownMonitor shutDownMonitor = null;
	private Lock lockRemoveQ = new ReentrantLock();

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
		this.qMonitorStatMap = new HashMap<String, String>();
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
						// 1. Start the monitor
						String monitorAddress = startNewMonitorProcess(qName);

						// 2. Start the exchange
						// 2.1. Getting the monitor stat address
						// 2.2. Start the exchange
						boolean xChangeOK = startNewExchangeProcess(qName, this.qMonitorMap.get(qName),
								this.qMonitorStatMap.get(qName));
						
						// if OK send OK
						if (monitorAddress != null & xChangeOK) {
							logger.info("Successfully created new Q for " + qName + "@" + monitorAddress);
							this.clientReqSocket.send(
									(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_OK) + "," + monitorAddress)
											.getBytes(), 0);
						} else {
							logger.error("The create queue request has failed at the monitor host,check log (when starting launching scripts");
							this.clientReqSocket.send(
									(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) + "," + monitorAddress)
											.getBytes(), 0);
						}
					} else {
						logger.error("The create queue request sent does not contain 3 part: ID, quName, Monitor host");
						this.clientReqSocket.send(
								(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) + ", ").getBytes(), 0);
					}
					break;

				case RoQConstant.CONFIG_REMOVE_QUEUE:
					logger.debug("Recieveing remove Q request from a client ");
					if (info.length == 2) {
						String qName = info[1];
						removingQueue(qName);
						this.clientReqSocket.send((Integer.toString(RoQConstant.OK) + ", ").getBytes(), 0);
					} else {
						logger.error("The remove queue request sent does not contain 2 part: ID, quName");
						this.clientReqSocket.send(
								(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) + ", ").getBytes(), 0);
					}
					break;

				case RoQConstant.CONFIG_CREATE_EXCHANGE:
					logger.debug("Recieveing create XChange request from a client ");
					if (info.length == 4) {
						String qName = info[1];
						// Qname, monitorhost, monitorstat host
						if (startNewExchangeProcess(qName, info[2], info[3])) {
							this.clientReqSocket.send((Integer.toString(RoQConstant.OK) ).getBytes(), 0);
						} else {
							this.clientReqSocket.send((Integer.toString(RoQConstant.FAIL) ).getBytes(), 0);
						}
					} else {
						logger.error("The create new exchange does not contain 4 parts: ID, Qname, monitor, monitor host");
						this.clientReqSocket.send(
								(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE_FAIL) ).getBytes(), 0);
					}
					break;
				}
			}
		}
		this.clientReqSocket.close();
	}

	/**
	 * Remove a complete queue: 1. Sends a shut down request to the
	 * corresponding monitor 2. The monitor will send a shut down request to all
	 * exchanges that it knows
	 * 
	 * @param qName
	 *            the logical Q name to remove
	 */
	private void removingQueue(String qName) {
		try {
			this.lockRemoveQ.lock();
			logger.debug("Removing Q  " + qName);
			String monitorAddress = this.qMonitorMap.get(qName);
			// The address is the address of the base monitor, we need to
			// extract
			// the port and make +5
			// to get the shutdown monitor thread
			int basePort = RoQUtils.getInstance().extractBasePort(monitorAddress);
			String portOff = monitorAddress.substring(0, monitorAddress.length() - "xxxx".length());
			logger.info("Sending Remove Q request to " + portOff + (basePort + 5));
			// 2. Send the remove message to the monitor
			// The monitor will stop all the exchanges during its shut down
			ZMQ.Socket shutDownMonitor = ZMQ.context(1).socket(ZMQ.REQ);
			shutDownMonitor.setSendTimeOut(0);
			shutDownMonitor.connect(portOff + (basePort + 5));
			shutDownMonitor.send((Integer.toString(RoQConstant.SHUTDOWN_REQUEST)).getBytes(), 0);
			shutDownMonitor.close();
		} finally {
			this.lockRemoveQ.unlock();
		}
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
	private boolean startNewExchangeProcess(String qName, String monitorAddress, String monitorStatAddress) {
		if(monitorAddress == null || monitorStatAddress==null){
			logger.error("The monitor or the monitor stat server is null", new IllegalStateException());
			return false;
		}
		// 1. Get the number of installed queues on this host
		int number = 0;
		for (String q_i : this.qExchangeMap.keySet()) {
			List<String> xChanges = this.qExchangeMap.get(q_i);
			number += xChanges.size();
		}
		// 2. Assigns a front port and a back port
		logger.debug(" This host contains already " + number + " Exchanges");
		int frontPort = this.baseFrontPort + number * 3; // 3 because there is
															// the front, back
															// and the shut down
		int backPort = frontPort + 1;
		String ip = RoQUtils.getInstance().getLocalIP();

		// 2. Launch script
		try {
			ProcessBuilder pb = new ProcessBuilder("java", "-Djava.library.path="
					+ System.getProperty("java.library.path"), "-cp", System.getProperty("java.class.path"),
					ExchangeLauncher.class.getCanonicalName(), new Integer(frontPort).toString(),
					new Integer(backPort).toString(), monitorAddress, monitorStatAddress);
			logger.debug("Starting: " + pb.command());
			final Process process = pb.start();
			pipe(process.getErrorStream(), System.err);
			pipe(process.getInputStream(), System.out);
			if (this.qExchangeMap.containsKey(qName)) {
				this.qExchangeMap.get(qName).add("tcp://" + ip + ":" + frontPort);
				logger.debug("Storing Xchange info: " + "tcp://" + ip + ":" + frontPort);
			} else {
				List<String> xChange = new ArrayList<String>();
				xChange.add("tcp://" + ip + ":" + frontPort);
				this.qExchangeMap.put(qName, xChange);
				logger.debug("Storing Xchange info: " + "tcp://" + ip + ":" + frontPort);
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
		return (this.baseMonitortPort + this.qMonitorMap.size() * 6);
	}

	/**
	 * @return the monitor stat port
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
	 * @return the monitor address as tcp://IP:port of the newly created monitor
	 *         +"," tcp://IP: statport
	 */
	private String startNewMonitorProcess(String qName) {
		// 1. Get the number of installed queues on this host
		int frontPort = getMonitorPort();
		int statPort = getStatMonitorPort();
		logger.debug(" This host contains already " + this.qMonitorMap.size() + " Monitor");
		String argument = frontPort + " " + statPort;
		logger.debug("Starting monitor process by script launch on " + argument);

		// 2. Launch script
		// ProcessBuilder pb = new ProcessBuilder(this.monitorScript, argument);
		ProcessBuilder pb = new ProcessBuilder("java",
				"-Djava.library.path=" + System.getProperty("java.library.path"), "-cp",
				System.getProperty("java.class.path"), MonitorLauncher.class.getCanonicalName(),
				new Integer(frontPort).toString(), new Integer(statPort).toString());

		logger.debug("Starting: " + pb.command());
		String monitorAddress = "tcp://" + RoQUtils.getInstance().getLocalIP() + ":" + frontPort;
		String statAddress = "tcp://" + RoQUtils.getInstance().getLocalIP() + ":" + statPort;

		try {
			final Process process = pb.start();
			pipe(process.getErrorStream(), System.err);
			pipe(process.getInputStream(), System.out);

			this.qMonitorMap.put(qName, (monitorAddress));
			this.qMonitorStatMap.put(qName, statAddress);
			return monitorAddress + "," + statAddress;
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

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "Host config manager " + RoQUtils.getInstance().getLocalIP();
	}

	/**
	 * @return the qMonitorMap
	 */
	public HashMap<String, String> getqMonitorMap() {
		return qMonitorMap;
	}

	/**
	 * @param qMonitorMap
	 *            the qMonitorMap to set
	 */
	public void setqMonitorMap(HashMap<String, String> qMonitorMap) {
		this.qMonitorMap = qMonitorMap;
	}

	/**
	 * @return the shutDownMonitor
	 */
	public ShutDownMonitor getShutDownMonitor() {
		return shutDownMonitor;
	}

}
