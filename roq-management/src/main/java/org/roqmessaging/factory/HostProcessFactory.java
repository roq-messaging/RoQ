package org.roqmessaging.factory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstantInternal;
import org.roqmessaging.core.launcher.ExchangeLauncher;
import org.roqmessaging.core.launcher.MonitorLauncher;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.config.internal.HostConfigDAO;
import org.roqmessaging.management.launcher.hook.ShutDownSender;
import org.roqmessaging.management.monitor.ProcessMonitor;
import org.roqmessaging.management.server.state.HcmState;
import org.roqmessaging.scaling.launcher.ScalingProcessLauncher;
	
public class HostProcessFactory {
	// Logger
	private Logger logger = Logger.getLogger(HostProcessFactory.class);
	// The host configuration manager properties
	private HostConfigDAO properties = null;
	//The lock to avoid any race condition
	private Lock lockRemoveQ = new ReentrantLock();
	
	private HcmState serverState;
	
	private ProcessMonitor processMonitor;
	
	public HostProcessFactory(HcmState serverState, HostConfigDAO properties) 
			throws IOException {
		this.serverState = serverState;
		this.properties = properties;
	}
	
	public void setProcessMonitor(ProcessMonitor processMonitor) {
		this.processMonitor = processMonitor;
	}
	
	/**
	 * Remove a complete queue: 1. Sends a shut down request to the
	 * corresponding monitor 2. The monitor will send a shut down request to all
	 * exchanges that it knows
	 * 
	 * @param qName
	 *            the logical Q name to remove
	 */
	public void removingQueue(String qName) {
		try {
			this.lockRemoveQ.lock();
			logger.debug("Removing Q  " + qName);
			String monitorAddress = serverState.getMonitor(qName);
			// The address is the address of the base monitor, we need to
			// extract
			// the port and make +5
			// to get the shutdown monitor thread
			int basePort = RoQSerializationUtils.extractBasePort(monitorAddress);
			String portOff = monitorAddress.substring(0, monitorAddress.length() - "xxxx".length());
			logger.info("Sending Remove Q request to " + portOff + (basePort + 5));
			// 2. Send the remove message to the monitor
			// The monitor will stop all the exchanges during its shut down
			ShutDownSender shutDownSender = new ShutDownSender(portOff + (basePort + 5));
			shutDownSender.shutdown();
			//3. Stopping the scaling process
			if(serverState.scalingProcessExists(qName)){
				shutDownSender.setAddress(portOff + serverState.getScalingProcess(qName).toString());
				shutDownSender.shutdown();
			}
			//The caller must remove the queue.
		} finally {
			this.lockRemoveQ.unlock();
		}
	}


	/**
	 * Start a new exchange process
	 * 1. Check the number of local xChange present in the host 2. Start a new
	 * xChange with port config + nchange
	 * 
	 * @param qName
	 *            the name of the queue to create
	 * @return true if the creation process worked well
	 */
	public boolean startNewExchangeProcess(String qName, String monitorAddress, String monitorStatAddress, String transID) {		
		if (monitorAddress == null || monitorStatAddress == null) {
			logger.error("The monitor or the monitor stat server is null", new IllegalStateException());
			return false;
		}
		// Check if the Exchange already exists (idempotent exchange creation process)
		if (serverState.ExchangeExists(qName, transID)) {
				return true;
		}
		
		// 1. Get the number of installed queues on this host
		int number = 0;
		for (String q_i : serverState.getAllExchanges()) {
			number += serverState.getExchanges(q_i).size();
		}
		// 2. Assigns a front port and a back port
		logger.debug(" This host contains already " + number + " Exchanges");
		//x4 = Front, back, Shutdown, prod request
		int frontPort = this.properties.getExchangeFrontEndPort() + number * 4;
		// 3 because there is the front, back and the shut down
		int backPort = frontPort + 1;
		String ip = RoQUtils.getInstance().getLocalIP();

		// We start the exchange in its own process
		// Launch script
		try {
			ProcessBuilder pb = new ProcessBuilder("java", "-Djava.library.path="
					+ System.getProperty("java.library.path"), "-cp", System.getProperty("java.class.path"), "-Xmx"+this.properties.getExchangeHeap()+"m","-XX:+UseConcMarkSweepGC",
					ExchangeLauncher.class.getCanonicalName(), new Integer(frontPort).toString(), new Integer(
							backPort).toString(), monitorAddress, monitorStatAddress);
			logger.info("Starting: " + pb.command());
			final Process process = pb.start();
			// Start process monitoring
			processMonitor.addprocess(Integer.toString(frontPort), process, RoQConstantInternal.PROCESS_EXCHANGE, transID);
			pipe(process.getErrorStream(), System.err);
			pipe(process.getInputStream(), System.out);
		} catch (IOException e) {
			logger.error("Error while executing script", e);
			return false;
		}
		
		logger.debug("Storing Xchange id: " + transID + " info: " + "tcp://" + ip + ":" + frontPort);
		serverState.putExchange(qName, transID, "tcp://" + ip + ":" + frontPort);
		return true;
	}

	/**
	 * @return the monitor port
	 */
	private int getMonitorPort() {
		return (this.properties.getMonitorBasePort() + serverState.getAllMonitors().size() * 6);
	}

	/**
	 * @return the monitor stat port
	 */
	private int getStatMonitorPort() {
		//By for because the stat monitor starts on port, its shutdown on port+1, the scaling process on 
		//port+2 and its shuto down process on port +3.
		return (this.properties.getStatMonitorBasePort() + serverState.getAllMonitors().size()*4);
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
	public String startNewMonitorProcess(String qName) {
		
		int statPort, frontPort;
		String monitorAddress, statAddress;
		if (serverState.MonitorExists(qName)) {
			monitorAddress = serverState.getMonitor(qName);
			statAddress = serverState.getStat(qName);
			String[] splitAddress = monitorAddress.split(":");
			frontPort = new Integer((splitAddress[splitAddress.length - 1]));
			splitAddress = statAddress.split(":");
			statPort = new Integer((splitAddress[splitAddress.length - 1]));
		}
		else {
			// 1. Get the number of installed queues on this host
			frontPort = getMonitorPort();
			statPort = getStatMonitorPort();
			logger.debug(" This host contains already " + serverState.getAllMonitors().size() + " Monitor");
			String argument = frontPort + " " + statPort;
			logger.debug("Starting monitor process by script launch on " + argument);
			//Monitor configuration
			monitorAddress = "tcp://" + RoQUtils.getInstance().getLocalIP() + ":" + frontPort;
			statAddress = "tcp://" + RoQUtils.getInstance().getLocalIP() + ":" + statPort;
		}
		// We start the monitor in its own process
		// ProcessBuilder pb = new ProcessBuilder(this.monitorScript,
		// argument);
		ProcessBuilder pb = new ProcessBuilder("java", "-Djava.library.path="
				+ System.getProperty("java.library.path"), "-cp", System.getProperty("java.class.path"),	MonitorLauncher.class.getCanonicalName(), new Integer(frontPort).toString(),
				new Integer(statPort).toString(), qName, new Integer(this.properties.getStatPeriod()).toString(), this.properties.getLocalPath(), new Long( this.properties.getMonitorHbPeriod()).toString());
		try {
			logger.debug("Starting: " + pb.command());
			final Process process = pb.start();
			// Start process monitoring
			processMonitor.addprocess(Integer.toString(frontPort), process, RoQConstantInternal.PROCESS_MONITOR, qName);
			pipe(process.getErrorStream(), System.err);
			pipe(process.getInputStream(), System.out);
		} catch (IOException e) {
			logger.error("Error while executing script", e);
			return null;
		}
		
		//add the monitor configuration
		serverState.putMonitor(qName, monitorAddress);
		serverState.putStat(qName, statAddress);
		return monitorAddress + "," + statAddress;
	}
	
	/**
	 * @param qName the name of queue for which we need to create the scaling process
	 * @param port the listener port on wich the sclaing process will scubscribe to configuration update
	 * @return true if the creation was OK
	 */
	public boolean startNewScalingProcess(String qName) {
		if(serverState.statExists(qName)){
			//1. Compute the stat monitor port+2
			int basePort = RoQSerializationUtils.extractBasePort(serverState.getMonitor(qName));
			basePort+=3;
			
			// Get the address and the ports used by the GCM
			String gcm_address = this.properties.getGcmAddress();
			int gcm_interfacePort = this.properties.ports.get("GlobalConfigurationManager.interface");
			int gcm_adminPort    = this.properties.ports.get("MngtController.interface");
			
			
			// Start scalingProcess in its own process
			// 2. Launch script
			try {
				ProcessBuilder pb = new ProcessBuilder("java", "-Djava.library.path="
						+ System.getProperty("java.library.path"), "-cp", System.getProperty("java.class.path"),
						ScalingProcessLauncher.class.getCanonicalName(),
						gcm_address, Integer.toString(gcm_interfacePort), Integer.toString(gcm_adminPort),
						qName, Integer.toString(basePort));
				logger.debug("Starting: " + pb.command());
				final Process process = pb.start();
				// Start process monitoring
				processMonitor.addprocess(Integer.toString(basePort), process, RoQConstantInternal.PROCESS_SCALING, qName);
				pipe(process.getErrorStream(), System.err);
				pipe(process.getInputStream(), System.out);
			} catch (IOException e) {
				logger.error("Error while executing script", e);
				return false;
			}	

			//4. Add the configuration information
			logger.debug("Storing scaling process information");
			serverState.putScalingProcess(qName, basePort+1);
		}else{
			return false;
		}
		return true;
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

	public void stopProcess(String pid) {
				
	}

}
