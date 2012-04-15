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
package org.roqmessaging.management.launcher;

import org.roqmessaging.management.HostConfigManager;

/**
 * Class HostConfigManagerLauncher 
 * <p> Description: Launcher for {@linkplain HostConfigManager}
 * Launched by  java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.HostConfigManagerLauncher
 * 
 * @author sskhiri
 */
public class HostConfigManagerLauncher {
	
	/**
	 * @param args no argument shall be provided,  it starts on the port 5100
	 */
	public static void main(String[] args) {
		System.out.println("Starting the local host configuration manager");
		HostConfigManager hostManager = new HostConfigManager();
		ShutDownHook hook = new ShutDownHook(hostManager);
		Runtime.getRuntime().addShutdownHook(hook);
		Thread configThread = new Thread(hostManager);
		configThread.start();
		try {
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
		private HostConfigManager hostManager = null;

		/**
		 * Set the launcher as argument
		 * 
		 * @param launcher
		 *            the RaQall in 1 local launcher
		 */
		public ShutDownHook(HostConfigManager manager) {
			this.hostManager = manager;
		}

		public void run() {
			System.out.println("Running Clean Up...");
			try {
				this.hostManager.getShutDownMonitor().shutDown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	}
}

