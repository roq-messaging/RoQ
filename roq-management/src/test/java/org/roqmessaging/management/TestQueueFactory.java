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
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.utils.RoQUtils;

/**
 * Class TestQueueFactory
 * <p> Description: TODO
 * 
 * @author sskhiri
 */
public class TestQueueFactory {
	private Logger logger = Logger.getLogger(TestQueueFactory.class);
	private 	GlobalConfigurationManager configurationManager =null;
	
	/**
	 * 
	 */
	@Before
	private void setup() {
		//1. Start the configuration
		this.logger.info("Start global config thread");
		GlobalConfigurationManager configurationManager = new GlobalConfigurationManager();
		Thread configThread = new Thread(configurationManager);
		configThread.start();
		//Let 1 sec to init the thread
		try {
			this.wait(1000);
		} catch (InterruptedException e) {
			logger.error("Error while waiting", e);
		}
	}

	@Test
	public void testQueueTopologyRequest() {
		IRoQLogicalQueueFactory factory = new LogicalQueueFactory(RoQUtils.getInstance().getLocalIP().toString());
		
	}
	
	/**
	 * 
	 */
	@After
	private void tearDown() {
		this.configurationManager.shutDown();

	}

}
