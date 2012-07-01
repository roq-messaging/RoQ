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
package org.roqmessaging.core.timer;

import java.util.ArrayList;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.core.Monitor;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.stat.StatisticMonitor;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.state.ExchangeState;
import org.zeromq.ZMQ;

/**
 * Class MonitorStatTimer
 * <p> Description: specific timer class that sends stat to the {@linkplain StatisticMonitor} about the
 *  queue topology as the number of exchanges and the number of total clients connected.
 *  Notice that currently the stat monitor server is intended to run on the sam machine as the monitor. If it is not the case anymore in the  future versions 
 *  we should revised the mode in which we define the stat monitor server.
 * 
 * @author sskhiri
 */
public class MonitorStatTimer extends TimerTask {
	//the monitor for the logical queue we send stat
	private Monitor queueMonitor = null;
	//ZMQ configuration
	private ZMQ.Context context;
	private ZMQ.Socket statSocket;
	//Logger
	private Logger logger = Logger.getLogger(MonitorStatTimer.class);
	// KPI calculated
	private int numberProducers =0;
	private int totalThroughput =0;
	
	/**
	 * @param queueMonitor the queue monitor which sends stat
	 * @param period the timer period
	 */
	public MonitorStatTimer(Monitor queueMonitor) {
		super();
		this.queueMonitor = queueMonitor;
		this.context = ZMQ.context(1);
		this.statSocket = context.socket(ZMQ.PUB);
		String statMonitorServer = "tcp://"+RoQUtils.getInstance().getLocalIP()+":"+this.queueMonitor.getStatPort();
		this.statSocket.connect(statMonitorServer);
		this.logger.debug("Connecting to "+statMonitorServer);
	}

	/**
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		//1. Re compute KPI
		getALlProcuders(this.queueMonitor.getExhcangeMetaData());
		//2. Prepare message
		String message= RoQConstant.STAT_Q +","+this.queueMonitor.getName()+","
	+this.queueMonitor.getExhcangeMetaData().size()
	+","+numberProducers+","+ totalThroughput;
		logger.info("Stat monitor: "+ message);
		//3. send message
		statSocket.send(message.getBytes(), 0);
	}
	
	/** Stop the sockets.
	 * @see java.util.TimerTask#cancel()
	 */
	public void shutTdown() {
		logger.debug("Canceling the Monitor stat timer");
		this.statSocket.close();
	}

	/**
	 * @param exhcangeMetaData the set of meta data from queues
	 * @return the total number of producers connected to the logical queue
	 */
	private void getALlProcuders(ArrayList<ExchangeState> exhcangeMetaData) {
		this.totalThroughput=0;
		this.numberProducers =0;
		for (ExchangeState exchangeState : exhcangeMetaData) {
			numberProducers+=exchangeState.getNbProd();
			totalThroughput+=exchangeState.getThroughput();
		}
	}
}
