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
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQUtils;
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
	//The queue name
	private String qName = null;
	
	//Define whether we are running
	private volatile boolean running;
	
	private Logger logger = Logger.getLogger(StatisticMonitor.class);

	/**
	 * Init & constructor
	 * @param qname the name of the queue
	 */
	public StatisticMonitor(int statPort, String qname) {
		//init the socket
		context = ZMQ.context(1);
		this.statPort = statPort;
		this.qName = qname;
		//Start the sub socket from RoQ elements
		statSub = context.socket(ZMQ.SUB);
		statSub.bind("tcp://"+RoQUtils.getInstance().getLocalIP()+":"+ statPort);
		statSub.subscribe("".getBytes());
		logger.debug("Statistic Monitor on "+ "tcp://"+RoQUtils.getInstance().getLocalIP()+":"+ statPort);
		
		//Start the forwarder element
		kpiPub = context.socket(ZMQ.PUB);
		kpiPub.bind("tcp://*:"+ (statPort+1));
		logger.debug("Binding the Stat publisher port on "+ (statPort+1));
	}

	/**
	 * Implements a siple forward pattern
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		logger.info("Starting the Stat Monitor on " + RoQUtils.getInstance().getLocalIP() + ":" + (this.statPort));
		this.running = true;
		// Poller that will poll for a message on the stat sub
		ZMQ.Poller poller = context.poller(1);
		poller.register(this.statSub);
		// Running the thread
		while (this.running) {
			poller.poll(2000);
			if (poller.pollin(0)) {
				do {
					kpiPub.send(translateStream(statSub.recv(0)), statSub.hasReceiveMore() ? ZMQ.SNDMORE : 0);
				} while (this.statSub.hasReceiveMore() && this.running);
			}
		}
		logger.info("Stopping the statistic monitor thread " + this.statPort);
		this.statSub.close();
		this.kpiPub.close();
	}

	/**
	 * Translates the java encoding in BSON encoding
	 * @param recv the stat recieved by the RoQ elements
	 */
	private byte [] translateStream(byte[] recv) {
		logger.debug("Start translating the Statistic stream in BSON");
		String info[] = new String(recv).split(",");
		int infoCode = Integer.parseInt(info[0]);
		BSONObject statObj = null;
		
		//Check what are the stat types
		switch (infoCode) {
		
		/*  Stat from Publisher and subscriber*/
		case RoQConstant.STAT_TOTAL_SENT:
			logger.info("1 producer finished, sent " + info[2] + " messages.");
			statObj = new BasicBSONObject();
			statObj.put("CMD",RoQConstant.STAT_TOTAL_SENT);
			statObj.put("PublisherID", info[1]);
			statObj.put("TotalSent", info[2]);
			statObj.put("QName", this.qName);
			logger.debug(statObj.toString());
			return BSON.encode(statObj);
			
		case RoQConstant.STAT_PUB_MIN:
			statObj = new BasicBSONObject();
			statObj.put("CMD",RoQConstant.STAT_PUB_MIN);
			statObj.put("SubscriberID", info[1]);
			statObj.put("Total", info[2]);
			statObj.put("QName", this.qName);
			logger.debug(statObj.toString());
			return BSON.encode(statObj);
			
		case RoQConstant.STAT_TOTAL_RCVD:
			//Stat send from Subscriber 
			//31, minute + "," + totalReceived + "," + received + "," + subsriberID + "," + meanLat)
			statObj = new BasicBSONObject();
			statObj.put("CMD",RoQConstant.STAT_TOTAL_RCVD);
			statObj.put("Minute", info[1]);
			statObj.put("TotalReceived", info[2]);
			statObj.put("Received", info[3]);
			statObj.put("SubsriberID", info[4]);
			statObj.put("MeanLat", info[5]);
			logger.debug(statObj.toString());
			return BSON.encode(statObj);
			
			/*  Stat from Exchanges: these 3 messages are sent in the same message envelope*/
		case RoQConstant.STAT_EXCHANGE_ID:
			statObj = new BasicBSONObject();
			statObj.put("CMD",RoQConstant.STAT_EXCHANGE_ID);
			statObj.put("X_ID", info[1]);
			statObj.put("CMD",RoQConstant.STAT_EXCHANGE_ID);
			statObj.put("QName", this.qName);
			logger.debug(statObj.toString());
			return BSON.encode(statObj);
			
		case RoQConstant.STAT_EXCHANGE_MIN:
			//Stat send by the exchange
			//21,minute,totalProcessed,processed,totalthroughput,throughput,nbProd
			statObj = new BasicBSONObject();
			statObj.put("CMD",RoQConstant.STAT_EXCHANGE_MIN);
			statObj.put("Minute", info[1]);
			statObj.put("TotalProcessed", info[2]);
			statObj.put("Processed", info[3]);
			statObj.put("TotalThroughput", info[4]);
			statObj.put("Throughput", info[5]);
			statObj.put("Producers", info[6]);
			logger.debug(statObj.toString());
			return BSON.encode(statObj);
			
		case RoQConstant.STAT_EXCHANGE_OS_MIN:
			//Stat send by the exchange
			//22,cpu,memory
			statObj = new BasicBSONObject();
			statObj.put("CMD",RoQConstant.STAT_EXCHANGE_OS_MIN);
			statObj.put("CPU", info[1]);
			statObj.put("MEMORY", info[2]);
			logger.debug(statObj.toString());
			return BSON.encode(statObj);
			
			/*  Stat from Logical queue*/
			//TODO creating a monitor timer that will sends statistics 
			//23,QName, number of exchange registered, number of total producer, total througput on Q
			
		case RoQConstant.STAT_Q:
			statObj = new BasicBSONObject();
			statObj.put("CMD",RoQConstant.STAT_Q);
			statObj.put("QName", info[1]);
			statObj.put("XChanges", info[2]);
			statObj.put("Producers", info[3]);
			statObj.put("Throughput", info[4]);
			logger.debug(statObj.toString());
			return BSON.encode(statObj);
			
	}
		return BSON.encode(new BasicBSONObject());
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
