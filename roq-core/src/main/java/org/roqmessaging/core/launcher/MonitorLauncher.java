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
	 * Example: 5571 5800 testQ period
	 * @param args must be the base monitor port and the stat port, default value is 
	 * 5571 5800 qname period
	 *
	 */
	public static void main(String[] args) {
		System.out.println("Launching Monitor process with arg "+displayArg(args));
		if(args.length !=4){
			System.out.println("The argument should be <int: base port> <int: stat port> <qname>");
			return;
		}
		//TODO for future evolution: replacing the 3rd argument by the property file location directly
		System.out.println("Starting Monitor on base port "+ args[0] + ", "+args[1] +", "+ args[2] +" "+ args[3]) ;
		try {
			int basePort = Integer.parseInt(args[0]);
			int statPort = Integer.parseInt(args[1]);
			final Monitor monitor = new Monitor(basePort, statPort,  args[2], args[3]);
			Thread t = new Thread(monitor);
			t.start();
			
		} catch (NumberFormatException e) {
			System.out.println(" The arguments are not valid, must: <int: base port> <int: stat port>");
		}
		
	}

	/**
	 * @param args the argument we recieved at the start up
	 * @return the concatenated string of argument
	 */
	private static String displayArg(String[] args) {
		String result="";
		for (int i = 0; i < args.length; i++) {
			result+=args[i] +", ";
		}
		return result;
	}

}
