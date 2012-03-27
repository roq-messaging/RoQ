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
import org.roqmessaging.core.Monitor;
import org.roqmessaging.core.utils.RoQUtils;

/**
 * Class MonitorLauncher
 * <p>
 * Description: Launches the monitor thread. We need 1 monitor instance per
 * logical queue.
 * 
 * @author sskhiri
 */
public class MonitorLauncher {

	/**
	 * @param args must be the base monitor port and the stat port, default value is 
	 * 5570 5800
	 */
	public static void main(String[] args) {
		Logger logger = Logger.getLogger(MonitorLauncher.class);
		System.out.println(RoQUtils.getInstance().getFileStamp());
		int basePort = Integer.parseInt(args[0]);
		int statPort = Integer.parseInt(args[1]);
		logger.info("Starting the monitor on base port "+basePort+ ", stat port" +statPort);
		Monitor monitor = new Monitor(basePort, statPort);
		Thread t = new Thread(monitor);
		t.start();
	}

}
