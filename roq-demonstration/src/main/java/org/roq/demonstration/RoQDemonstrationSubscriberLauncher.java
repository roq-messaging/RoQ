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

import org.roqmessaging.client.IRoQQueueManagement;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.factory.RoQQueueManager;

/**
 * Class RoQDemonstrationSubscriberLauncher
 * <p>
 * Description: lauch a subscriber demo that prints all the messages recieved. Can be launched by <br>
 * java -Djava.library.path=/usr/local/lib -cp roq-demonstration-1.0-SNAPSHOT-jar-with-dependencies.jar org.roq.demonstration.RoQDemonstrationSubscriberLauncher <GCM IP address, Qname>
 * 
 * @author BVanMelle
 */
public class RoQDemonstrationSubscriberLauncher {
	/**
	 * Run the interactive publisher
	 * @param args the argument should be [The GCM IP address, The QName defined by the user]
	 */
	public static void main(String[] args) {
		System.out.println("Starting the Subscriber's daemon with  " + args.length + " arguments");
		if(args.length!=2){
			System.out.println("The argument must be [The GCM IP address, The queue name of your choice]");
			System.exit(0);
		}
		System.out.println("Starting the Subscriber's daemon with queue: " + args[0]);
		// The Qname used for demonstration
		String qName = args[1];
		try {
			// Create the queue
			IRoQQueueManagement qFactory = new RoQQueueManager(args[0]);
			qFactory.createQueue(qName);
			Thread.sleep(5000);
			//Create the subscriber
			IRoQConnectionFactory conFactory = new RoQConnectionFactory(args[0]);
			IRoQSubscriberConnection subscriberConnection = conFactory.createRoQSubscriberConnection(qName, "test");
			subscriberConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				public void onEvent(byte[] msg) {
					String message = new String(msg, 0, msg.length);
					System.out.println("Got message: " + message);
				}
			};
			// Set the subscriber logic for this connection
			subscriberConnection.setMessageSubscriber(subs);
			// Maintain the main process openned
			System.out.println("Subscriber's daemon started with queue: " + args[0]);
			while (true) {
				Thread.sleep(500);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
