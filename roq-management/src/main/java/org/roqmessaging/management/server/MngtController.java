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

import org.apache.log4j.Logger;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.zeromq.ZMQ;

/**
 * Class MngtController
 * <p> Description: Controller that loads/receive  data from the {@linkplain GlobalConfigurationManager}
 * and refresh the stored data.
 * 
 * @author sskhiri
 */
public class MngtController implements Runnable{
	private Logger logger = Logger.getLogger(MngtController.class);
	//ZMQ 
	private ZMQ.Context context = null;
	private ZMQ.Socket mngtSubSocket = null;
	//running
	private volatile boolean active = true;
	
	/**
	 * Constructor.
	 * @param globalConfigAddress the address on which the global config server runs.
	 */
	public MngtController(String globalConfigAddress) {
		//Init ZMQ
		context = ZMQ.context(1);
		mngtSubSocket = context.socket(ZMQ.SUB);
		mngtSubSocket.connect("tcp://"+globalConfigAddress+":5000");
	}

	/**
	 * Runs in a while loop and wait for updated information from the {@linkplain GlobalConfigurationManager}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// ZMQ init of the subscriber socket
		ZMQ.Poller poller = context.poller(3);
		poller.register(mngtSubSocket);// 0

		while (this.active && !Thread.currentThread().isInterrupted()) {
			// Set the poll time out, it returns either when something arrive or
			// when it time out
			poller.poll(2000);
			if (poller.pollin(0)) {
				//An event arrives
			}
		}

	}

}
