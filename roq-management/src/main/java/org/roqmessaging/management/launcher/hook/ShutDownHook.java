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
package org.roqmessaging.management.launcher.hook;

import org.roqmessaging.core.ShutDownMonitor;

/**
 * Class ShutDownHook
 * <p>
 * Description: provides a JVM hook to call when shutting down the VM.
 * It implements the adapter pattern to adapt the {@linkplain ShutDownMonitor}.
 * 
 * @author sskhiri
 */
public class ShutDownHook extends Thread {
	private ShutDownMonitor monitor = null;


	/**
	 * @param _monitor the shutdown monitor to call on shut dow.
	 */
	public ShutDownHook(ShutDownMonitor _monitor) {
		this.monitor= _monitor;
	}

	public void run() {
		System.out.println("Running Clean Up...");
		try {
			this.monitor.shutDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}