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
package org.roqmessaging.testload;

import org.json.simple.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.loaders.TestLoadController;
import org.roqmessaging.loaders.TestLoaderDecription;

/**
 * Class TestLoadController
 * <p> Description: Test a complete test scenario at the Test Load controller level.
 * 
 * @author sskhiri
 */
public class TestLoadControllerCase extends RoQTestCase {

	@Test
	public void testEnd2EndTestLoad() throws InterruptedException {
		//The Qname
		String qName = "performance-test";
		//Init 1. create the test queue 
		super.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP());
		//Init 2. let the queue start 
		Thread.sleep(4000);
		//3. Set a test description
		TestLoaderDecription desc = new TestLoaderDecription();
		//Warning the diration must have a ".0" otherwise it will be considered as a Long not a double.
		String description = "{\"maxPub\":5,\"duration\":1.0,\"rate\":200,\"maxSub\":5,\"payload\":1,\"delay\":5,\"spawnRate\":1}";
		//4. Start the test
		try {
			desc.load(description);
			TestLoadController controller = new TestLoadController(desc, RoQUtils.getInstance().getLocalIP(),qName );
			controller.start();
		} catch (ParseException e) {
			super.logger.error(e);
		}
	}

}
