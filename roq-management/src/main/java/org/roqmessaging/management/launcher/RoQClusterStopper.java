/**
 * Copyright 2013 EURANOVA
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

import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.launcher.hook.ShutDownSender;

/**
 * Class GlobalConfigurationStopper
 * <p> Description: Stop the GCM by sending a stop signal to its shutdown monitor onthe port 5001.
 * 
 * @author sskhiri
 */
public class RoQClusterStopper {

	/**
	 * @param args defines whether we want to stop the GCM or the HCM: [HCM|| GCM]
	 */
	public static void main(String[] args) {
		System.out.println("Starting the RoQ Shutdown sender");
		String target = RoQUtils.getInstance().getLocalIP();;
		if (args.length!=1){
			System.out.println("The usage is ./shutdown [GCM|| HCM]]");
		}else{
			if( args[0].equalsIgnoreCase("GCM"))	{
				System.out.println("Shutting down GCM @"+target);
				ShutDownSender sender = new ShutDownSender("tcp://"+target+":5001");
				sender.shutdown();
				System.out.println("Shutting down Signal sent");
			}else{
				if( args[0].equalsIgnoreCase("HCM"))	{
					System.out.println("Shutting down HCM @"+target);
					ShutDownSender sender = new ShutDownSender("tcp://"+target+":5101");
					sender.shutdown();
					System.out.println("Shutting down Signal sent");
				}else{
					System.out.println("The usage is ./shutdown [GCM|| HCM]]");
				}
			}
		}
	}
}
