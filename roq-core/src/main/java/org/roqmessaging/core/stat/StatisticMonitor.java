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
package org.roqmessaging.core.stat;

import org.apache.log4j.Logger;
import org.roqmessaging.core.interfaces.IStoppable;
import org.zeromq.ZMQ;

/**
 * Class StatisticMonitor
 * <p> Description: The monitor managing the statistic coming from the Sub, Pub and Exchanges. This class implement a ZMQ forward pattern 
 * to all KPI listeners.
 * </p>
 * Statport -> Statport+1
 * 
 * @author sskhiri
 */
public class StatisticMonitor implements Runnable, IStoppable {
	//ZMQ configuration
	private ZMQ.Context context =null;
	//Sub to RoQ elements
	private ZMQ.Socket  statSub= null;
	//Publisher to KPI listener
	private ZMQ.Socket  kpiPub= null;
	//The port on which we start
	private int statPort =0;
	
	//Define whether we are running
	private volatile boolean running;
	
	private Logger logger = Logger.getLogger(StatisticMonitor.class);

	/**
	 * Init & constructor
	 */
	public StatisticMonitor(int statPort) {
		//init the socket
		context = ZMQ.context(1);
		this.statPort = statPort;
		//Start the sub socket from RoQ elements
		statSub = context.socket(ZMQ.SUB);
		statSub.bind("tcp://*:"+ statPort);
		statSub.subscribe("".getBytes());
		
		//Start the forwarder element
		kpiPub = context.socket(ZMQ.PUB);
		kpiPub.bind("tcp://*:"+ (statPort+1));
	}

	/**
	 * Implements a siple forward pattern
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		this.running = true;
		//Poller that will poll for a message on the stat sub
		ZMQ.Poller poller = context.poller(3);
		poller.register(this.statSub);
		//Running the thread
		while (this.running) {
			do {
				if (poller.pollin(0)) {
					kpiPub.send(statSub.recv(0), statSub.hasReceiveMore() ? ZMQ.SNDMORE : 0);
					// if (!frontendSub.hasReceiveMore())
					// break;
				}
			}while (this.statSub.hasReceiveMore() && this.running);
		}
		logger.info("Stopping the statistic monitor thread "+this.statPort);
		this.statSub.close();
		this.kpiPub.close();
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		this.running= false;
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "Stat monitor "+ this.statPort;
	}

}
