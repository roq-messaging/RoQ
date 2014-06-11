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

import static org.junit.Assert.assertNotNull;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.roqmessaging.client.IRoQConnection;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.client.IRoQSubscriber;
import org.roqmessaging.client.IRoQSubscriberConnection;
import org.roqmessaging.clientlib.factory.IRoQConnectionFactory;
import org.roqmessaging.core.Exchange;
import org.roqmessaging.core.Monitor;
import org.roqmessaging.core.factory.RoQConnectionFactory;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.HostConfigManager;
import org.roqmessaging.management.LogicalQFactory;

/**
 * Class BasicSetupTests
 * <p>
 * Description: This Test case test the basic setup of the Exchange with few
 * listeners and providers.
 * @deprecated the test uses shortcut that makes following test invalid.
 * 
 * @author Sabri Skhiri
 */
@Ignore
public class BasicSetupTest {
	private Exchange xChange = null;
	private Monitor monitor = null;
	private IRoQPublisher publisher = null;
	private IRoQConnection connection = null;
	private IRoQConnectionFactory factory = null;
	private IRoQSubscriberConnection subConnection = null;
	private GlobalConfigurationManager configManager =null;
	private Logger logger = Logger.getLogger(BasicSetupTest.class);
	int basePort = 5500;
	int stat = 5900;
	int frontPort = 5561;
	private HostConfigManager hostConfigManager=null;

	/**
	 * Create the Exchange.
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.log(Level.INFO, "Basic Setup before test");
		startGlobalConfig();
		startMonitor(basePort, stat);
		startExchange("tcp://localhost:"+basePort, "tcp://localhost:"+stat);
		startPublisherClient();
		startSubscriberClient(); 
	}


	/**
	 * Start the global configuration thread
	 */
	private void startGlobalConfig() {
		this.configManager = new GlobalConfigurationManager("testCGM.properties");
		//1. start a host config manager
		this.logger.info("Start host config...");
		if(hostConfigManager==null){
			hostConfigManager = new HostConfigManager("testHCM.properties");
			// add a fake queue in the host config manager
			this.hostConfigManager.getqMonitorMap().put("queue1", "tcp://localhost:"+basePort);
			Thread hostThread = new Thread(hostConfigManager);
			hostThread.start();
		}
		//1. Create a fake logical queue
		// if we use the logical q Factory API we would not need to cheat
		configManager.addQueue("queue1", "tcp://localhost:"+basePort, "tcp://localhost:"+stat, "localhost");
		Thread thread = new Thread(configManager);
		thread.start();
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.connection.close();
		this.subConnection.close();
		
		LogicalQFactory loQFactory = new LogicalQFactory("localhost");
		loQFactory.refreshTopology();
		loQFactory.removeQueue("queue1");
	
		this.hostConfigManager.getShutDownMonitor().shutDown();
		Thread.sleep(2000);
		this.configManager. getShutDownMonitor().shutDown();
		Thread.sleep(2000);
	}

	@Test
	public void test() {
		int attemp = 0;
		logger.log(Level.INFO, "Start main Test");
		assertNotNull(this.xChange);
		assertNotNull(this.monitor);
		assertNotNull(this.configManager);
		
		//1. Check wether the connection is ready
		try {
			logger.info("Before sending");
			Thread.sleep(2000);
			//Need to wait at least 10 second to make the echange and listener configure porperly.
			while(	!this.connection.isReady()&& attemp<6){
				Thread.sleep(3000);
				logger.info("Waiting for connection ready..." + attemp*3 +" sec");
				attemp++;
			}
			logger.info("Sending message to subscriber ...");
			if(this.connection.isReady()){
				this.publisher.sendMessage("sabri".getBytes(), "hello".getBytes());
				this.publisher.sendMessage("sabri".getBytes(), "hello".getBytes());
			}
			else throw new IllegalStateException("Connection is not ready after 15 sec");
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
	private void startExchange(String monitorHost, String statHost) {
		this.xChange = new Exchange(frontPort, (frontPort+1), monitorHost,statHost );
		Thread t = new Thread(this.xChange);
		t.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initiate a thread publisher
	 */
	private void startPublisherClient() {
		//1. Creating the connection
		this.factory = new RoQConnectionFactory("localhost");
		this.connection = this.factory.createRoQConnection("queue1");
		this.connection.open();
		this.publisher = this.connection.createPublisher();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initiate a thread subscriber
	 */
	private void startSubscriberClient() {
		this.subConnection = this.factory.createRoQSubscriberConnection("queue1", "sabri");
		this.subConnection.open();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.subConnection.setMessageSubscriber(new IRoQSubscriber() {
			public void onEvent(byte[] msg) {
				String content= new String(msg,0,msg.length) ;
				assert content.equals("hello");
				logger.info("In message listener recieveing :"+ content);
			}
		});
	}
	
	/**
	 * Start the monitor thread. We need one monitor per logical queue.
	 * @param basePort the base port on which the monitor starts
	 */
	private void startMonitor(int basePort, int statPort) {
		this.monitor = new Monitor(basePort, statPort, "queue1", "5000");
		Thread t = new Thread(this.monitor);
		t.start();
		
	}

}
