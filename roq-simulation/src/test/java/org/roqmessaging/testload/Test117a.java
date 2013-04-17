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
package org.roqmessaging.testload;

import java.util.Timer;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.loaders.SenderLoader;
import org.roqmessaging.loaders.TestLoadController;
import org.roqmessaging.loaders.TestLoaderDecription;

/**
 * Class Test117a
 * <p> Description: Test the first scenario defined in the Issue #117 for checking the static behavior.
 * ## Test 1: starting a producer after the cold start condition
 * 1. Starting a test load with 2 Publishers and 3 Subscribers. The load must be > 10% 
 * than the MAX exchange limit in order to overcome the cold start condition. <br>
 * 2. After 1 min, when we are sure that the cold start condition is not applicable : we create an exchange and we 
 * start a new producer and we check the allocation.
 * 
 * @author sskhiri
 */
public class Test117a extends RoQTestCase  {
	//The logger
	private Logger logger = Logger.getLogger(Test117a.class);

	@Test
	public void test() throws InterruptedException {
		// The Qname
		String qName = "test117-Q";
		// Init 1. create the test queue
		super.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP());
		// Init 2. let the queue start
		Thread.sleep(2000);
		// 3. Set a test description
		TestLoaderDecription desc = new TestLoaderDecription();
		//The limit is 75 Mi byte/min 
		// 10% of the limit is 7.5Mi byte/min => 125k byte/sec
		//With a rate of 12500 msg/sec of 10 byte we can reach the limit
		String description = "{\"maxPub\":1,\"duration\":4.0,\"throughput\":15000,\"maxSub\":1,\"payload\":10,\"delay\":5,\"spawnRate\":1}";
		// 4. Start the test
		try {
			//This should be re-implemented since it blocks in the start
			desc.load(description);
			TestLoadController controller = new TestLoadController(desc, RoQUtils.getInstance().getLocalIP(), qName);
			controller.start(true);
			//Wait for the the second part of the test
			Thread.sleep(100000);
			logger.info("TEST 117-------------------------------------Starting The exchange----------------------------");
			// create an exchange
			super.factory.createExchange(qName, RoQUtils.getInstance().getLocalIP());
			Thread.sleep(30000);
			//Start a producer
			logger.info("TEST 117-------------------------------------Starting The producer----------------------------");
			SenderLoader loader = new SenderLoader(100, 5, RoQUtils.getInstance().getLocalIP(), qName);
			Timer timerLoad = new Timer("Loader Publisher");
			//Schedule it, the run will be called soon
			timerLoad.schedule(loader, 50,1000);
			Thread.sleep(30000);
			timerLoad.cancel();
			
		} catch (ParseException e) {
			super.logger.error(e);
		}
	}

}
