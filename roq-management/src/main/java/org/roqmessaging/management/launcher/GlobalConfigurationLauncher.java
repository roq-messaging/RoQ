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

import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.launcher.hook.ShutDownHook;

/**
 * Class GlobalConfigurationLauncher
 * <p> Description: Launcher for the global configuration.
 * Launched by:
 * java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.GlobalConfigurationLauncher
 * 
 * @author sskhiri
 */
public class GlobalConfigurationLauncher {

	/**
	 * @param args no args,  it starts on the port 5100
	 */
	public static void main(String[] args) {
		System.out.println("Starting the  global configuration manager");
		GlobalConfigurationManager configurationManager = new GlobalConfigurationManager(60000);
		ShutDownHook hook = new ShutDownHook(configurationManager.getShutDownMonitor());
		Runtime.getRuntime().addShutdownHook(hook);
		Thread configThread = new Thread(configurationManager);
		configThread.start();
		try {
			while (true) {
				Thread.sleep(500);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
