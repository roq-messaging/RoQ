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
package org.roq.simulation.bak;

import java.util.ArrayList;

import org.roqmessaging.core.SubscriberConnectionManager;

/**
 * Class SubScriberLauncher
 * <p>
 * Description: Launches subscriber thread according to cmd lines args.
 * 
 * @author sskhiri
 */
public class SubScriberLauncher {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<String> monitors = new ArrayList<String>();
		monitors.add("tcp://localhost:5571");
		ArrayList<String> statMonitors = new ArrayList<String>();
		statMonitors.add("tcp://localhost:5800");
		SubscriberConnectionManager SubClient = new SubscriberConnectionManager(1, monitors, statMonitors, "manche", Boolean.parseBoolean(args[1]));
		Thread t = new Thread(SubClient);
		t.start();
	}

}
