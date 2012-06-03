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

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.HostConfigManager;
import org.roqmessaging.management.server.MngtController;

/**
 * Class RoQAllLocalLauncher
 * <p>
 * Description: launch a all local instance of RoQ elements. Can be launched by <br>
 * java -Djava.library.path=/usr/local/lib -cp roq-simulation-1.0-SNAPSHOT-jar-with-dependencies.jar org.roq.simulation.RoQAllLocalLauncher
 * 
 * @author sskhiri
 */
@Ignore
public class RoQAllLocalLauncher {
	private Logger logger = Logger.getLogger(RoQAllLocalLauncher.class);
	private GlobalConfigurationManager configurationManager = null;
	private HostConfigManager hostConfigManager = null;
	private String configurationServer = "?";
	private String configFile = "GCM.properties";

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
		this.logger.info("Initial setup Start global config thread");
		this.logger.info("Start global config...");
		configurationManager = new GlobalConfigurationManager(this.configFile);
		Thread configThread = new Thread(configurationManager);
		configThread.start();
		// 2. Start the host configuration manager locally
		this.logger.info("Start host config....");
		hostConfigManager = new HostConfigManager("testHCM.properties");
		Thread hostThread = new Thread(hostConfigManager);
		hostThread.start();
		this.logger.info("Start factory config...");
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
		this.configurationManager.getShutDownMonitor().shutDown();
		this.hostConfigManager.getShutDownMonitor().shutDown();
		Thread.sleep(4000);
	}

	/**
	 * @param args
	 *            must contain 2 argument the queue name that we want to create and true or false
	 */
	public static void main(String[] args) {
		if(args.length!=2){
			System.out.println("The args must be <qname> <true||false>");
			System.exit(0);
		}
		RoQAllLocalLauncher launcher = new RoQAllLocalLauncher();
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
	 * @return the configurationServer
	 */
	public String getConfigurationServer() {
		return configurationServer;
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
}
