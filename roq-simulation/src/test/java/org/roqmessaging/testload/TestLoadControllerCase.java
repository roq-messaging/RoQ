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
@Ignore
public class TestLoadControllerCase extends RoQTestCase {

	@Test
	public void testEnd2EndTestLoad() {
		TestLoaderDecription desc = new TestLoaderDecription();
		String description = "{\"maxPub\":5,\"duration\":1,\"rate\":10,\"maxSub\":5,\"payload\":1,\"delay\":5,\"spawnRate\":1}";
		try {
			desc.load(description);
			TestLoadController controller = new TestLoadController(desc, RoQUtils.getInstance().getLocalIP(), RoQUtils
					.getInstance().getLocalIP());
			controller.start();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
