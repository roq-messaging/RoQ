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
package org.roqmessaging.core.launcher;

import org.apache.log4j.Logger;
import org.roqmessaging.core.Monitor;

/**
 * Class MonitorLauncher
 * <p>
 * Description: Launches a monitor thread. We need 1 monitor instance per
 * logical queue.
 * 
 * @author sskhiri
 */
public class MonitorLauncher {

	/**
	 * Example: 5571 5800
	 * @param args must be the base monitor port and the stat port, default value is 
	 * 5571 5800
	 */
	public static void main(String[] args) {
		final Logger logger = Logger.getLogger(MonitorLauncher.class);
		System.out.println("Starting Monitor on base port "+ args[1] + ", "+args[2]);
		try {
			int basePort = Integer.parseInt(args[1]);
			int statPort = Integer.parseInt(args[2]);
			final Monitor monitor = new Monitor(basePort, statPort);
			Thread t = new Thread(monitor);
			t.start();
			
			//TODO Implementing a JMX interface to clean the shut down
			//http://stackoverflow.com/questions/191215/how-to-stop-java-process-gracefully
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					logger.info("Shutting down Monitor");
					monitor.cleanShutDown();
				}
			});
		} catch (NumberFormatException e) {
			System.out.println(" The arguments are not valid, must: <int: base port> <int: stat port>");
		}
		
	}

}
