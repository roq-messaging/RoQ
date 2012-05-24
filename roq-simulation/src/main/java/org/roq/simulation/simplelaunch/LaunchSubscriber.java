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
package org.roq.simulation.simplelaunch;

import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.management.LogicalQFactory;
import org.roqmessaging.management.launcher.hook.ShutDownHook;

/**
 * Class LaunchSubscriber
 * <p> Description: Clean test class to validate a simple Roq installation. 
 * it creates a Queue  and register a subscriber with the key "key".
 * It will stop either on kill signal or after 20 seconds
 * 
 * @author sskhiri
 */
public class LaunchSubscriber {

	/**
	 * @param args [0] the global configuration manager address, [1] the queue name to create
	 */
	public static void main(String[] args) {
		if(args.length!=2){
			System.out.println("The argument must contain <GCM address> <QNAME>");
			System.exit(0);
		}
		System.out.println("Starting simulation subscriber launcher on "+args[0] + "," + args[1]);
		
		// Create Q
		IRoQLogicalQueueFactory factory = new LogicalQFactory(args[0]);
		factory.createQueue(args[1], args[0]);
		
		//Register a hook
		PublisherInit init = new PublisherInit(args[1], args[0]);
		ShutDownHook hook = new ShutDownHook(new ShutDownMonitor(1000, init));
		Runtime.getRuntime().addShutdownHook(hook);
		
		int count =0;
		try {
			while (count<10) {
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
	

