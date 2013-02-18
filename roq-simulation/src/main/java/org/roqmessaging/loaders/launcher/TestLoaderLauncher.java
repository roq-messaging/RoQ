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
package org.roqmessaging.loaders.launcher;

import java.io.File;
import java.util.Scanner;

import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.loaders.TestLoadController;
import org.roqmessaging.loaders.TestLoaderDecription;

/**
 * Class TestLoaderLauncher
 * <p> Description: Launcher for the Test Load controller. It initiates a complete test defined by the test 
 * description provided in JSON as argument. Notice that the second argument is the queue used for the test 
 * every test process must use the same queue name.
 * 
 * @author sskhiri
 */
public class TestLoaderLauncher {
	
	/**
	 * Starts the {@linkplain TestLoadController}
	 * @param args the argument should be [The JSON description file location, the queue name under test]
	 */
	public static void main(String[] args) {
		System.out.println("Starting the Load Test Controller with  " + args.length + " arguments");
		if(args.length!=2){
			System.out.println("The argument must be [test file description location, queue name (under test)]");
			System.exit(0);
		}
		System.out.println("Starting the Load Test Controller");
		// The Qname used for test
		String qName = args[1];
		//The test desc file location
		String file = args[0];
		System.out.println("Starting with test description from "+ file +", executing test on queue "+ qName);
		File fileDesc = new File(file);
		if(!fileDesc.isFile()){
			System.out.println("The file  "+ file +"does not exist or cannot be found!");
			System.exit(0);
		}
		try {
			TestLoaderDecription desc = new TestLoaderDecription();
			//Read the file content
			String description = new Scanner(fileDesc, "UTF-8").useDelimiter("\\A").next();
			//Will read the user input
			Scanner scan = new Scanner(System.in);
			// Warning the duration must have a ".0" otherwise it will be
			// considered
			// as a Long not a double.
			//String description = "{\"maxPub\":5,\"duration\":2.5,\"rate\":3000,\"maxSub\":5,\"payload\":20,\"delay\":5,\"spawnRate\":1}";
			desc.load(description);
			System.out.println("Are you sure to launch the test with this configuration[Y,N]: \n "+ desc.toString());
			if(checkYes(scan)){
				final TestLoadController controller = new TestLoadController(desc, RoQUtils.getInstance().getLocalIP(),
						qName);
				Runtime.getRuntime().addShutdownHook(new Thread() {
					/**
					 * Just stop the Test press ctrl C.
					 */
					@Override
					public void run() {
						System.out.println("Shutting down the Test loader contoller");
						controller.shutDown();
					}
				});

				// Start the test
				controller.start();
				// Maintain the main process openned
				while (true) {
					Thread.sleep(500);
				}
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Evaluate the user input [Y,N]
	 * @param scan the scanner that will scan the I/O
	 * @return true if the user enter yes
	 */
	private static boolean checkYes(Scanner scan) {
		String choice = scan.nextLine();
		if (choice.equalsIgnoreCase("Y"))
			return true;
		else
			return false;
	}

}
