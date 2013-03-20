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
package org.roqmessaging.core;

import org.apache.log4j.Logger;
import org.roqmessaging.core.interfaces.IStoppable;
import org.zeromq.ZMQ;

/**
 * Class ShutDownMonitor
 * <p>
 * Description: This thread can be launched with every {@link IStoppable}
 * element in order to expose a shutdown socket to stop the element. In this way
 * the element is not impacted by the socket polling.
 * 
 * @author sskhiri
 */
public class ShutDownMonitor implements Runnable {
	// The shutdown socket
	private ZMQ.Socket shutDownSocket;
	private ZMQ.Context context = null;
	// Logger
	private Logger logger = Logger.getLogger(ShutDownMonitor.class);
	// Element to monitor
	private IStoppable monitored = null;
	//Defines whether this is still active.
	private volatile boolean active = true;

	/**
	 * @param port
	 *            port to start the shutdown request socket
	 */
	public ShutDownMonitor(int port, IStoppable roqElement) {
		this.monitored = roqElement;
		this.context = ZMQ.context(1);
		this.shutDownSocket = context.socket(ZMQ.REP);
		this.shutDownSocket.bind("tcp://*:" + port);
		logger.debug("Shut down request socket to " + "tcp://*:" + (port));
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// 1. Set the poller
		ZMQ.Poller items = new ZMQ.Poller(1);
		items.register(shutDownSocket);

		// 2. Starting the loop
		logger.info("Shutdown monitor started for " + this.monitored.getName());

		// 2. Start the main run of the monitor
		while (this.active) {
			items.poll(50);
			if (items.pollin(0)) {
				String info = new String(shutDownSocket.recv(0));
				logger.info("Shutdown request received: " + info +" for "+ this.monitored.getName());
				int infoCode = Integer.parseInt(info);
				if (infoCode == RoQConstant.SHUTDOWN_REQUEST) {
					this.shutDownSocket.setSendTimeOut(0);
					this.shutDownSocket.send(Integer.toString(RoQConstant.OK).getBytes(), 0);
					shutDown();
				} else {
					logger.error("The shutdown Monitor got a wrong request !");
				}
			}
		}
		logger.info("Monitor has been shut down for "+this.monitored.toString());
	}

	/**
	 * Stops the IStoppable
	 */
	public void shutDown() {
		logger.info("Shutting down : " + this.monitored.getName());
		// close me
		this.active = false;
		//Close monitored
		this.monitored.shutDown();
		//Close socket
		this.shutDownSocket.close();
	}

}
