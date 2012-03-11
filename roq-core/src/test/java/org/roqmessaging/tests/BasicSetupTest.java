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
package org.roqmessaging.tests;

import static org.junit.Assert.assertNotNull;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roqmessaging.client.RoQPublisher;
import org.roqmessaging.core.Exchange;
import org.roqmessaging.core.Monitor;
import org.roqmessaging.core.PublisherClient;
import org.roqmessaging.core.SubClientLib;

/**
 * Class BasicSetupTests
 * <p>
 * Description: This Test case test the basic setup of the Exchange with few
 * listeners and providers.
 * 
 * @author Sabri Skhiri
 */
public class BasicSetupTest {
	private Exchange xChange = null;
	private Monitor monitor = null;
	private Thread threadSub = null;
	private RoQPublisher publisher = null;
	private Logger logger = Logger.getLogger(BasicSetupTest.class);

	/**
	 * Create the Exchange.
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.log(Level.INFO, "Basic Setup before test");
		startExchange();
		startMonitor();
		startPublisherClient();
		startSubscriberClient(); 
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.xChange.cleanShutDown();
		// TODO implementing a clean shutdown methd
		this.threadSub.stop();
		this.monitor.cleanShutDown();
	}

	@Test
	public void test() {
		int attemp = 0;
		logger.log(Level.INFO, "Start Test");
		assertNotNull(this.xChange);
		assertNotNull(this.monitor);
		try {
			//Need to wait at least 10 second to make the echange and listener configure porperly.
			while(	!this.publisher.sendMessage("sabri".getBytes(), "hello".getBytes()) && attemp<6){
				Thread.sleep(3000);
				logger.info("Re -try sending message");
				attemp++;
			}
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Start an Exchange with Hardcoded value. Monitor host = "localhost" A
	 * potential evolution would be a configuration file from which the
	 * parameter are loaded.
	 */
	private void startExchange() {
		final String monitorHost = "localhost";
		this.xChange = new Exchange("5559", "5560", monitorHost);
		Thread t = new Thread(this.xChange);
		t.start();
		
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initiate a thread publisher
	 */
	private void startPublisherClient() {
		// Launching the pub client
	   this.publisher = new PublisherClient();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initiate a thread subscriber
	 */
	private void startSubscriberClient() {
		String monitor = "localhost";
		String subKey = "manche";
		int ID = 0;
		boolean tstmp = true;
		SubClientLib SubClient = new SubClientLib(monitor, subKey, ID, tstmp);
		this.threadSub= new Thread(SubClient);
		this.threadSub.start();
	}
	
	/**
	 * Start the monitor thread. We need one monitor per logical queue.
	 */
	private void startMonitor() {
		this.monitor = new Monitor();
		Thread t = new Thread(this.monitor);
		t.start();
		
	}

}
