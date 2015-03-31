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
package org.roq.simulation;

import java.io.File;

import org.apache.curator.test.TestingServer;
import org.apache.log4j.Logger;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.HostConfigManager;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.internal.FileConfigurationReader;
import org.roqmessaging.management.config.internal.GCMPropertyDAO;
import org.roqmessaging.management.server.MngtController;

/**
 * Class RoQAllLocalLauncher
 * <p>
 * Description: launch a all local instance of RoQ elements. Can be launched by <br>
 * java -Djava.library.path=/usr/local/lib -cp roq-simulation-1.0-SNAPSHOT-jar-with-dependencies.jar org.roq.simulation.RoQAllLocalLauncher
 * 
 * @author sskhiri
 */
public class RoQAllLocalLauncher {
	private static final Logger logger = Logger.getLogger(RoQAllLocalLauncher.class);
	private GlobalConfigurationManager configurationManager = null;
	private HostConfigManager hostConfigManager = null;
	String configurationServer = "?";
	int    configurationServerInterfacePort;
	private TestingServer zkServer;
	private String configFile = "GCM.properties";

	/**
	 * Helper method to create an instance of {@link GlobalConfigurationManager}
	 * based on the provided configuration file and zookeeper connection string.
	 * The function takes care to overwrite the "zkConfig.servers" property
	 * with the one provided to ensure that the GCM connects to the correct
	 * zookeeper server.
	 * 
	 * @param configFile       the gcm configuration file
	 * @param zkConnectString  the comma-separated string of ip:port zookeeper addresses
	 * @return                 a new instance of {@link GlobalConfigurationManager}
	 * @throws Exception
	 */
	private GlobalConfigurationManager createGCM(String configFile, String zkConnectString) throws Exception {
		logger.info("Creating GCM");
		// Ignore the "zk.servers" property defined in the configuration file
		// and overwrite it with the connection string provided by the TestingServer class.
		GCMPropertyDAO gcmConfig = new FileConfigurationReader().loadGCMConfiguration(configFile);
		gcmConfig.zkConfig.servers = zkConnectString;
		logger.info("zk: " + zkConnectString);
		
		CloudConfig cloudConfig = new FileConfigurationReader().loadCloudConfiguration(configFile);
		return new GlobalConfigurationManager(gcmConfig, cloudConfig);
	}
	
	/**
	 * Starts:<br>
	 * 1. The global configuration manager<br>
	 * 2. The local host configuration manager for this host <br>
	 * 3. Adding the local host to global host configuration manager
	 * @param formatDB defined whether the DB must be cleaned.
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
		// 1. Start the configuration
		this.configurationServer =RoQUtils.getInstance().getLocalIP().toString();
		logger.info("Initial setup Start global config thread");
		
		logger.info("Start global config...");
		zkServer = new TestingServer();
		configurationManager = createGCM(configFile, zkServer.getConnectString());
		logger.info("GCM created");
		
		configurationServerInterfacePort = configurationManager.getInterfacePort();
		Thread configThread = new Thread(configurationManager);
		configThread.start();
		// 2. Start the host configuration manager locally
		logger.info("Start host config....");
		hostConfigManager = new HostConfigManager("testHCM.properties");
		Thread hostThread = new Thread(hostConfigManager);
		hostThread.start();
		logger.info("Start factory config...");
	}

	/**
	 * Stops all the involved elements
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
		this.hostConfigManager.getShutDownMonitor().shutDown();
		Thread.sleep(3000);
		this.configurationManager.getShutDownMonitor().shutDown();
		this.zkServer.close();
		Thread.sleep(6000);
	}

	/**
	 * @param args
	 *            must contain 2 argument the queue name that we want to create and true or false
	 */
	public static void main(String[] args) {
		RoQAllLocalLauncher launcher = null;
		if(args.length ==0) {
			launcher = new RoQAllLocalLauncher();
		}
		if(args.length ==1) {
			File file = new File(args[0]);
			if(file.exists()){
				launcher = new RoQAllLocalLauncher();
				launcher.setConfigFile(args[0]);
			}
			else{
				System.out.println(" File does not exist...");
				System.exit(0);
			}
		}
		ShutDownHook hook = new ShutDownHook(launcher);
		Runtime.getRuntime().addShutdownHook(hook);
		try {
			launcher.setUp();
			while (true) {
				Thread.sleep(500);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Class ShutDownHook
	 * <p>
	 * Description: provides a hook called when we stop the launcher
	 * 
	 * @author sskhiri
	 */
	private static class ShutDownHook extends Thread {
		private RoQAllLocalLauncher launcher = null;

		/**
		 * Set the launcher as argument
		 * 
		 * @param launcher
		 *            the RaQall in 1 local launcher
		 */
		public ShutDownHook(RoQAllLocalLauncher launcher) {
			this.launcher = launcher;
		}

		public void run() {
			System.out.println("Running Clean Up...");
			try {
				this.launcher.tearDown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return the GCM address
	 */
	public String getConfigurationServer() {
		return configurationServer;
	}
	
	/**
	 * @return the ZK address
	 */
	public String getZkServerAddress() {
		return zkServer.getConnectString();
	}
	
	/**
	 * @return the GCM topology port (req port for getting the topology)
	 */
	public int getConfigurationServerInterfacePort() {
		return configurationServerInterfacePort;
	}

	
	/**
	 * @return the mangement controller handle
	 */
	public MngtController getMngtController(){
		return this.configurationManager.getMngtController();
	}

	/**
	 * @return the configFile
	 */
	public String getConfigFile() {
		return configFile;
	}

	/**
	 * @param configFile the configFile to set
	 */
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}
	
	/**
	 * @return HCM instance
	 */
	public HostConfigManager getHCMInstance() {
		return this.hostConfigManager;
	}
}
