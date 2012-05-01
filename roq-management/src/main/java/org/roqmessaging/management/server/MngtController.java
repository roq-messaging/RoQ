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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.zeromq.ZMQ;

/**
 * Class MngtController
 * <p>
 * Description: Controller that loads/receive data from the
 * {@linkplain GlobalConfigurationManager} and refresh the stored data.
 * Global config manager: 5000 <br>
 * Global config manager pub sub port 5001<br>
 * Shutdown moonitor port 5002
 * 
 * 
 * @author sskhiri
 */
public class MngtController implements Runnable, IStoppable {
	private Logger logger = Logger.getLogger(MngtController.class);
	// ZMQ
	private ZMQ.Context context = null;
	private ZMQ.Socket mngtSubSocket = null;
	// running
	private volatile boolean active = true;
	private ShutDownMonitor shutDownMonitor = null;
	// utils for serialization
	private RoQSerializationUtils serializationUtils = null;
	//Management infra
	private MngtServerStorage storage = null;

	/**
	 * Constructor.
	 * 
	 * @param globalConfigAddress
	 *            the address on which the global config server runs.
	 */
	public MngtController(String globalConfigAddress) {
		init(globalConfigAddress, 5002);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param globalConfigAddress
	 *            the address on which the global config server runs.
	 *  @param shuttDownPort the port on which the shutdown monitor starts
	 */
	public MngtController(String globalConfigAddress, int shuttDownPort) {
		init(globalConfigAddress, shuttDownPort);
	}
	
	/**
	 * @param globalConfigAddress the global configuration server 
	 * @param shuttDownPort the port on which the shut down thread listens
	 */
	private void init(String globalConfigAddress,  int shuttDownPort ){
		// Init ZMQ
		context = ZMQ.context(1);
		mngtSubSocket = context.socket(ZMQ.SUB);
		mngtSubSocket.connect("tcp://" + globalConfigAddress + ":5001");
		// init variable
		this.serializationUtils = new RoQSerializationUtils();
		this.storage = new MngtServerStorage();
		//Shutdown thread configuration
		this.shutDownMonitor = new ShutDownMonitor(shuttDownPort, this);
	}

	/**
	 * Runs in a while loop and wait for updated information from the
	 * {@linkplain GlobalConfigurationManager}
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// ZMQ init of the subscriber socket
		ZMQ.Poller poller = context.poller(3);
		poller.register(mngtSubSocket);// 0

		// Init variables
		int infoCode = 0;

		// Start the running loop
		while (this.active && !Thread.currentThread().isInterrupted()) {
			// Set the poll time out, it returns either when something arrive or
			// when it time out
			poller.poll(2000);
			if (poller.pollin(0)) {
				// An event arrives start analysing code
				String info[] = new String(mngtSubSocket.recv(0)).split(",");
				// Check if exchanges are present: this happens when the queue
				// is shutting down a client is asking for a
				// connection
				infoCode = Integer.parseInt(info[0]);
				switch (infoCode) {
				case RoQConstant.MNGT_UPDATE_CONFIG:
					// Infocode, map(Q Name, host)
					if (mngtSubSocket.hasReceiveMore()) {
						HashMap<String, String> newConfig = this.serializationUtils.deserializeObject(mngtSubSocket
								.recv(0));
						storage.updateConfiguration(newConfig);
					} else {
						// Problem expected map here
						logger.error("Error, expected map of (Qname, host", new IllegalStateException(
								"Expected a second message" + " in the envelope"));
					}
					break;

				default:
					break;
				}
			}
		}
		logger.info("Stopping " + this.getClass().getName() + " cleaning sockets");
		this.mngtSubSocket.close();
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		this.active = false;

	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return this.getClass().getName() + " Server";
	}

}
