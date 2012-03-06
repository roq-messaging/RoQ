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
import org.roqmessaging.core.data.StatData;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class StatTimer
 * <p> Description: The stat timer gather statistic information about a producer. This {@link StatData} data 
 * object is used for sharing information between the exchange and the timer.
 * 
 * @author sskhiri
 */
public 	class ExchangeStatTimer extends TimerTask {
	private Logger logger = Logger.getLogger(ExchangeStatTimer.class);
	private ZMQ.Context timercontext;
	private ZMQ.Socket timersocket;
	private ZMQ.Socket statsPub;
	private int minute;
	private long totalThroughput;
	private StatData statistic = null;
	private Exchange xchange = null;

	public ExchangeStatTimer(Exchange xChangeRef,  StatData stat,  ZMQ.Context context ) {
		this.xchange = xChangeRef;
		this.timercontext = ZMQ.context(1);
		//TODO TImersocket  == ? statsPub?
		this.timersocket = timercontext.socket(ZMQ.PUB);
		this.statistic=stat;
		timersocket.connect(this.xchange.getS_monitor());
		this.statsPub = context.socket(ZMQ.PUB);
		this.statsPub.connect("tcp://" + stat.getS_monitorHostname() + ":5800");
		this.minute = 0;
		this.totalThroughput = 0;
	}

	public void run() {
		timersocket.send(("4," + RoQUtils.getInstance().getLocalIP() + "," + this.xchange.getMostProducer()
				+ "," + this.statistic.getThroughput() + "," + this.statistic.getMax_bw() + "," + this.xchange.getKnownProd().size())
				.getBytes(), 0);
		logger.info(RoQUtils.getInstance().getLocalIP() + " : minute " + minute + " : "
				+ this.statistic.getThroughput() + " bytes/min, " + this.xchange.getKnownProd().size()
				+ " producers connected");
		logger.info(RoQUtils.getInstance().getLocalIP() + " : minute " + minute + " : "
				+ this.xchange.getMostProducer());
       this.statistic.setTotalProcessed(this.statistic.getTotalProcessed()+this.statistic.getProcessed());
		statsPub.send(("21," + minute + "," + this.statistic.getTotalProcessed() + ","
				+ this.statistic.getProcessed() + "," + totalThroughput + "," + this.statistic.getThroughput()
				+ "," + this.xchange.getKnownProd().size()).getBytes(), 0);

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
		minute++;
		this.statistic.setThroughput(0);
		this.statistic.setProcessed(0);
	}
}
