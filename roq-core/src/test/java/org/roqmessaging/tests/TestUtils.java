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

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roqmessaging.core.utils.RoQUtils;

/**
 * Class TestUtils
 * <p> Description: Test the basic features of the utility classes.
 * 
 * @author sskhiri
 */
public class TestUtils {
	private Logger logger = Logger.getLogger(TestUtils.class);

	@Test
	public void testIP() {
		String address = RoQUtils.getInstance().getLocalIP();
		logger.debug("Local address to send with heart bit "+ address);
		assert address.length()>1;
	}

}
