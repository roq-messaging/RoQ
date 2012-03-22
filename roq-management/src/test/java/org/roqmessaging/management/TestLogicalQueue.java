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
package org.roqmessaging.management;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roqmessaging.core.utils.RoQUtils;

/**
 * Class TestLogicalQueue
 * <p>
 * Description: TODO
 * 
 * @author sskhiri
 */
public class TestLogicalQueue {
	private Logger logger = Logger.getLogger(TestLogicalQueue.class);
	private GlobalConfigurationManager configurationManager = null;
	private LogicalQueueFactory factory = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// 1. Start the configuration
		this.logger.info("Initial setup Start global config thread");
		configurationManager = new GlobalConfigurationManager();
		Thread configThread = new Thread(configurationManager);
		configThread.start();
		factory = new LogicalQueueFactory(RoQUtils.getInstance().getLocalIP().toString());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.configurationManager.shutDown();
	}

	@Test
	public void testQueueTopologyRequest() {
		logger.info("Start the main test");
		// Let 1 sec to init the thread
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error("Error while waiting", e);
		}
		factory.refreshTopology();
	}

	@Test
	public void testCreateQueueRequest() {
		logger.info("Start create queue test");
		// Let 1 sec to init the thread
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error("Error while waiting", e);
		}
		// 1. Add new host
		String host = "10.20.5.10";
		this.configurationManager.addHostManager(host);
		// 2. Create the factory
		try {
			factory.createQueue("Sabri", host);
		} catch (IllegalStateException e) {
			logger.error("Error while waiting", e);
		}
	}

}
