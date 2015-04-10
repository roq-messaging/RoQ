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
package org.roq.demonstration;

import java.util.Scanner;

import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;

/**
 * Class RoQDemonstrationPublisherLauncher
 * <p>
 * Description: lauch an interactive publisher <br>
 * java -Djava.library.path=/usr/local/lib -cp roq-demonstration-1.0-SNAPSHOT-jar-with-dependencies.jar org.roq.demonstration.RoQDemonstrationPublisherLauncher <GCM IP address, Qname>
 * 
 * @author BVanMelle
 */
public class RoQDemonstrationPublisherLauncher {
	/**
	 * @param args the argument should be [GCM IP address, The QName that you subscribed]
	 */
	public static void main(String[] args) {
		System.out.println("Starting the interactive publisher with " + args.length + " arguments");
		if(args.length!=2){
			System.out.println("The argument must be [container IP address, queue name]");
			System.exit(0);
		}
		// The Qname used for demonstration
		try {
			//Will read the user inputs
			Scanner scan = new Scanner(System.in);
			try {
				//1. Creating the connection
				IRoQConnectionFactory factory = new RoQConnectionFactory(args[0]);
				IRoQConnection connection = factory.createRoQConnection(args[1]);
				connection.open();
				//2. Creating the publisher and sending message
				IRoQPublisher publisher = connection.createPublisher();
				// Maintain the main process openned
				System.out.println("Started the interactive publisher with queue: " + args[1]);
				while (true) {
					String msg = scan.nextLine();
					publisher.sendMessage("test".getBytes(), msg.getBytes());
				}
			} finally {
				scan.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
