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
package org.roqmessaging.loaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.LogicalQFactory;

/**
 * Class TestLoadController
 * <p> Description: This object is responsible for managing a test load. It will configure the test conditions 
 * according to the {@linkplain TestLoaderDecription}.
 * 
 * @author sskhiri
 */
public class TestLoadController implements IStoppable {
	//The load description
	private TestLoaderDecription testDesc = null;
	//The connection factory
	private IRoQConnectionFactory conFactory = null;
	//The Qname
	private String qName = "performance-test";
	//The GCM address
	private String gcmAddress = null;
	//handles on subscribers we created
	private List<IRoQSubscriberConnection> subscriberConnections = null;
	//handles on publishers we created
	private List<IStoppable> publisherConnections = null;
	//handles on the timers that launch the publisher processes
	private List<Timer> timerHandles = null;
	//Define how many message we must rcv befor logging them
	private int logMsg = 300000;
	//define whether the process has been stopped
	private boolean stopped = false;
	//The logger
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
	
	/**
	 * @param description the test load description
	 * @param gcmAddress the GCM address to connect
	 * @param queueName the queue name under test
	 */
	public  TestLoadController(TestLoaderDecription description, String gcmAddress, String queueName){
		this.testDesc = description;
		this.gcmAddress = gcmAddress;
		this.subscriberConnections = new ArrayList<IRoQSubscriberConnection>();
		this.publisherConnections = new ArrayList<IStoppable>();
		this.timerHandles = new ArrayList<Timer>();
		this.qName = queueName;
		//Init 0: create the connection factory
		this.conFactory = new RoQConnectionFactory(this.gcmAddress);
	}
	
	/**
	 * initializes the test load with the test description
	 * @param removeQonFinish defines whether we need to remore the queue on finish. 
	 */
	public void start(boolean removeQonFinish) {
		// 0. Wait for the delay before starting (delay in second)
		try {
			Thread.sleep(this.testDesc.getDelay() * 1000);
			// 1. Creates the y subscriber
			createAndAttachSubscriber(this.testDesc.getMaxSub());
			// 2. According to the spawn rate (pub/second), it creates the x
			// senders
			spawnPublisher(this.testDesc.getSpawnRate(), this.testDesc.getMaxProd());
			// 3. Wait for the test duration (min)
			logger.info("Waiting for end of the test in "+ this.testDesc.getDuration() +" min...");
			Thread.sleep((long) this.testDesc.getDuration() * 1000 * 60);
			// 4. Clean all
			stopTest();
			Thread.sleep(1000);
			if(removeQonFinish){
				IRoQLogicalQueueFactory factory = new LogicalQFactory(this.gcmAddress);
				factory.removeQueue(qName);
			}
		} catch (InterruptedException e) {
			logger.error("Error when starting the load test", e);
			stopTest();
		}
	}

	/**
	 * Stop the test and clean all
	 */
	private void stopTest() {
		logger.info("Stopping the test and clean operations.");
		if(!stopped){
			this.stopped =true;
			//Clean timers
			for (Timer timer_i : this.timerHandles) {
				timer_i.cancel();
				timer_i.purge();
			}
			this.timerHandles.clear();
			//Clear subscriber
			for (IRoQSubscriberConnection subscriber_i : this.subscriberConnections) {
				subscriber_i.close();
			}
			this.subscriberConnections.clear();
			//Clear the publisher
			for (IStoppable publisher_i: this.publisherConnections	) {
				publisher_i.shutDown();
			}
			this.publisherConnections.clear();
		}else{
			logger.info("Already Stopped");
		}
		
	}

	/**
	 * Create the publisher. It creates [spawn rate] publisher every second.
	 * @param spawnRate the rate on which we create the publisher every second
	 * @param publisherNumber the number of publisher to create
	 */
	private void spawnPublisher(int spawnRate, int publisherNumber) {
		try {
			// Number of publisher to create
			int toCreate = publisherNumber;
			//Define the last position in the array of publisher process
			int lastPosition = 0;
			//  While we need to create publishers - launch the timer tasks
			while (toCreate > 0) {
				for (int i = 0; (i < spawnRate) || (lastPosition<this.publisherConnections.size()); i++, lastPosition++) {
					//Create a sender loader process
					SenderLoader loader = new SenderLoader(this.testDesc.getRate(), this.testDesc.getPayload(), this.gcmAddress, this.qName);
					//Create a publisher process
					Timer timerLoad = new Timer("Loader "+i+" -"+System.currentTimeMillis());
					//Schedule it, the run will be called soon
					timerLoad.schedule(loader, 50,1000);
					//Keep an handle on the timer
					this.timerHandles.add(timerLoad);
					logger.info("Starting "+ timerLoad.toString());
				}
				toCreate -= spawnRate;
				//Sleep 1 second
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			logger.error("Error while spawning publishers...", e);
			stopTest();
		}
	}

	/**
	 * Create a certain number of subscribers and maintains a handle on the connection.
	 * @param maxSub the number of subscriber to create
	 */
	private void createAndAttachSubscriber(int maxSub) {
		for (int i = 0; i < maxSub; i++) {
			IRoQSubscriberConnection subscriberConnection = conFactory.createRoQSubscriberConnection(qName, "test");
			// Open the connection to the logical queue
			subscriberConnection.open();
			// Register a message listener
			IRoQSubscriber subs = new IRoQSubscriber() {
				private int count = 0;
				private int sizeMD = (Long.toString(System.currentTimeMillis()) + " ").getBytes().length + "test".getBytes().length;
				public void onEvent(byte[] msg) {
					count++;
					if(count>logMsg){
						logger.info("Got "+logMsg+" message of "+(msg.length+sizeMD)  +" byte" );
						count =0;
					}
				}
			};
			//Se the subscriber logic for this connection
			subscriberConnection.setMessageSubscriber(subs);
			//Maintains an handle to all subscriber connection
			this.subscriberConnections.add(subscriberConnection);
		}
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		System.out.println("Stopping the Test and cleaning connections.");
		Thread.currentThread().interrupt();
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "Test Load Controller on " + RoQUtils.getInstance().getLocalIP();
	}

}
