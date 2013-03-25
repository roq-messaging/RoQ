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

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roqmessaging.loaders.TestLoaderDecription;

/**
 * Class TestLoadDesc
 * <p> Description: Test the load description and serialization.
 * 
 * @author sskhiri
 */
public class TestLoadDesc {
	private Logger logger = Logger.getLogger(TestLoadDesc.class);

	@Test
	public void testJSonDescription() throws Exception {
		TestLoaderDecription desc = new TestLoaderDecription();
		logger.info(desc.toString());
		String json = desc.serializeInJason();
		logger.debug(json);
		desc.load(json);
	}
	
	@Test
	public void testJLoad() throws Exception {
		TestLoaderDecription desc = new TestLoaderDecription();
		String description = "{\"maxPub\":5,\"duration\":5.0,\"rate\":3,\"maxSub\":2,\"payload\":10,\"delay\":5,\"spawnRate\":8}";
		logger.debug(description);
		desc.load(description);
	}

}
