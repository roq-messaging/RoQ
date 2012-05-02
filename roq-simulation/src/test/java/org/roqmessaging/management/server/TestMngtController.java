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
package org.roqmessaging.management.server;

import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roq.simulation.RoQAllLocalLauncher;

/**
 * Class TestMngtController
 * <p> Description: This test will cover the functional tests of the management server
 * 
 * @author sskhiri
 */
public class TestMngtController {
	private RoQAllLocalLauncher launcher = null;
	private Logger logger = Logger.getLogger(TestMngtController.class);
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.launcher = new RoQAllLocalLauncher();
		this.launcher.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.launcher.tearDown();
	}


	@Test
	public void test() {
		fail("Not yet implemented");
	}

}
