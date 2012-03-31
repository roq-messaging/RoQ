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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Class TestProperties
 * <p>
 * Description: Test the property file
 * 
 * @author sskhiri
 */
public class TestProperties extends TestCase {
	private String monitorScript = "/usr/bin/roq/startMonitor.sh";
	private String exchangeScript = "/usr/bin/roq/startXchange.sh";

	@Test
	public void testEntry() throws IOException {
		// Properties defaultProps = new Properties();
		// FileInputStream in = new
		// FileInputStream("src/main/resources/config.properties");
		// defaultProps.load(in);
		// in.close();
		// String configArray=defaultProps.getProperty("config.managers");
		// String[] manager = configArray.split(",");
		// for (int i = 0; i < manager.length; i++) {
		// System.out.println(manager[i]);
		// }
	}

	public void testFileSystem() throws Exception {
		File script1 = new File(this.monitorScript);
		if (!script1.exists())
			System.out.println("False");
		File script2 = new File(this.exchangeScript);
		if (!script2.exists())
			System.out.println("False");
		System.out.println("True");
	}

}
