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

import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.core.Exchange;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.data.StatDataState;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.monitoring.HostOSMonitoring;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class StatTimer
 * <p> Description: The stat timer gather statistic information about a producer. This {@link StatDataState} data 
 * object is used for sharing information between the exchange and the timer.
 * 
 * @author sskhiri
 */
public 	class ExchangeStatTimer extends TimerTask implements IStoppable {
	private Logger logger = Logger.getLogger(ExchangeStatTimer.class);
	private ZMQ.Context timercontext;
	private ZMQ.Socket monitorSocket;
	private ZMQ.Socket statSocket;
	private int minute;
	private long totalThroughput;
	private StatDataState statistic = null;
	private Exchange xchange = null;
	private HostOSMonitoring hostMonitoring = null;
	private volatile boolean open = true;

	public ExchangeStatTimer(Exchange xChangeRef,  StatDataState stat ) {
		this.xchange = xChangeRef;
		this.timercontext = ZMQ.context(1);
		this.statistic=stat;
		//init the monitor socket to send info for re-allocation
		this.monitorSocket = timercontext.socket(ZMQ.PUB);
		this.monitorSocket.connect(this.xchange.getS_monitor());
		//init the statistic socket that sends information to the stat monitor forwarder
		this.statSocket = timercontext.socket(ZMQ.PUB);
		this.statSocket.connect(stat.getStatHost());
		this.logger.debug("Connecting to "+ stat.getStatHost());
		//init
		this.minute = 0;
		this.totalThroughput = 0;
		this.hostMonitoring = new HostOSMonitoring();
	}

	public void run() {
		if(open){
		monitorSocket.send((new Integer(RoQConstant.EVENT_MOST_PRODUCTIVE).toString()+"," + RoQUtils.getInstance().getLocalIP() + "," + this.xchange.getMostProducer()
				+ "," + this.statistic.getThroughput() + "," + this.statistic.getMax_bw() + "," + this.xchange.getKnownProd().size())
				.getBytes(), 0);
		logger.info(RoQUtils.getInstance().getLocalIP() + " : minute " + minute + " : "
				+ this.statistic.getThroughput() + " bytes/min, " + this.xchange.getKnownProd().size()
				+ " producers connected");
		logger.debug(RoQUtils.getInstance().getLocalIP() + " : minute " + minute + " : "
				+ this.xchange.getMostProducer());
		
       this.statistic.setTotalProcessed(this.statistic.getTotalProcessed()+this.statistic.getProcessed());
       
   	statSocket.send((
			new Integer(RoQConstant.STAT_EXCHANGE_ID).toString()+","
	                + this.xchange.getID() ).getBytes(), ZMQ.SNDMORE);
       
		statSocket.send((
				new Integer(RoQConstant.STAT_EXCHANGE_MIN).toString()+","
		                + minute + "," 
						+ this.statistic.getTotalProcessed() + ","
					 	+ this.statistic.getProcessed() + "," 
		                + totalThroughput + ","
					   	+ this.statistic.getThroughput()	+ "," 
		                + this.xchange.getKnownProd().size()+",").getBytes()
		                , ZMQ.SNDMORE);
		
		//CPU & memory
		statSocket.send((
				new Integer(RoQConstant.STAT_EXCHANGE_OS_MIN).toString()+","
		                + this.hostMonitoring.getCPUUsage() + "," 
						+ this.hostMonitoring.getMemoryUsage()).getBytes()
		                , 0);

		for (int i = 0; i < this.xchange.getKnownProd().size(); i++) {
			if (!this.xchange.getKnownProd().get(i).isActive()) {
				this.xchange.getKnownProd().get(i).addInactive();
				if (this.xchange.getKnownProd().get(i).getInactive() > 0) {
					this.xchange.getKnownProd().remove(i);
				}
			} else {
				this.xchange.getKnownProd().get(i).reset();
			}
		}
		totalThroughput += this.statistic.getThroughput();
		if(this.minute< Integer.MAX_VALUE-1000){
			minute++;
		}else minute =0;
		
		this.statistic.setThroughput(0);
		this.statistic.setProcessed(0);
		}
	}
	
	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
			this.open = false;
			logger.info("Closing  socket");
			this.cancel();
			this.monitorSocket.setLinger(0);
			this.statSocket.setLinger(0);
			this.monitorSocket.close();
			this.statSocket.close();
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return this.getClass().getName();
	}
	
}
