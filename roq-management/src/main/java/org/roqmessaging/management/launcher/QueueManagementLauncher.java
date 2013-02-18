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

import java.util.Scanner;

import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.LogicalQFactory;

/**
 * Class QueueManagementLauncher
 * <p> Description: process to launch to create or remove logical queue.
 * 
 * @author sskhiri
 */
public class QueueManagementLauncher {

	/**
	 * This process 
	 * @param args as [GCM address, add|del| help, queueName]
	 */
	public static void main(String[] args) {
		System.out.println("Starting Queue management process - this process will create a queue locally");
		if(args.length!=3){
			System.out.println("The right usage is :  <GCM address, add|| del || help, queue name]");
			System.exit(0);
		}
		LogicalQFactory factory = new LogicalQFactory(args[0]);
		//1. Parameter check
		System.out.println("Connecting to GCM@"+ args[0] +" to "+ args[1]+" Queue " + args[2] );
		if( (!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("del") && !args[1].equalsIgnoreCase("help"))){
			System.out.println(" The second argument must be add or del");
			System.out.println(" <GCM address, add|| del || help, queue name]");
			factory.clean();
			System.exit(0);
		}
		System.out.println("queue to create "+ args[2]);
		//2. Create the scanner that will scan the user input
		Scanner scan = new Scanner(System.in);
		if(args[1].equalsIgnoreCase("add")){
			System.out.println("Do you confirm to create the queue "+ args[2] +" (GCM @"+ args[0]+ ") [Y,N] ?");
			if(checkYes(scan))
				factory.createQueue(args[2], RoQUtils.getInstance().getLocalIP());
		}else{
			if(args[1].equalsIgnoreCase("del")){
				System.out.println("Do you confirm to remove the queue "+ args[2] +" (GCM @"+ args[0]+ ")[Y,N] ?");
				if(checkYes(scan))
					factory.removeQueue(args[2]);
			} else {
				if (args[1].equalsIgnoreCase("help")) {
					System.out.println("[<GCM address>,< add|| del || help>, <queue name>]");
					if (checkYes(scan))
						factory.removeQueue(args[2]);

				}
			}
		}
		factory.clean();
		

	}

	/**
	 * Evaluate the user input [Y,N]
	 * @param scan the scanner that will scan the I/O
	 * @return true if the user enter yes
	 */
	private static boolean checkYes(Scanner scan) {
		String choice = scan.nextLine();
		if (choice.equalsIgnoreCase("Y"))
			return true;
		else
			return false;
	}

}
