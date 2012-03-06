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
 * @author Nam-Luc Tran, Sabri Skhiri
 */

package org.roqmessaging.core;

import java.util.ArrayList;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.roqmessaging.core.data.StatData;
import org.roqmessaging.core.timer.ExchangeStatTimer;
import org.roqmessaging.core.timer.Heartbeat;
import org.zeromq.ZMQ;


/**
 * Class Exchange
 * <p> Description: The main component of the logical queue. All messages must 
 * go through this element.
 * 
 * @author Nam-Luc Tran, Sabri Skhiri
 */
public class Exchange implements Runnable {
	
	private Logger logger = Logger.getLogger(Exchange.class);

	private ArrayList<ProducerState> knownProd;
	private ZMQ.Context context;
	private ZMQ.Socket frontendSub;
	private ZMQ.Socket backendPub;
	private ZMQ.Socket monitorPub;
	private String s_frontend;
	private String s_backend;
	private String s_monitor;
	private boolean active;
	private StatData statistic=null;

	public Exchange(String frontend, String backend, String monitor) {
		knownProd = new ArrayList<ProducerState>();
		this.statistic = new StatData();
		this.statistic.setProcessed(0);
		this.statistic.setThroughput(0);
		this.statistic.setS_monitorHostname( monitor);
		this.statistic.setMax_bw( 5000); // bandwidth limit, in bytes/minute, per producer
		this.s_frontend = "tcp://*:" + frontend;
		this.s_backend = "tcp://*:" + backend;
		this.s_monitor = "tcp://" + monitor + ":5571";

		this.context = ZMQ.context(1);
		this.frontendSub = context.socket(ZMQ.SUB);
		this.backendPub = context.socket(ZMQ.PUB);
		// Caution, the following method as well as setSwap must be invoked before binding
		// Use these to (double) check if the settings were correctly set  
		// logger.info(this.backend.getHWM());
		// logger.info(this.backend.getSwap());
		this.backendPub.setHWM(3400);  

		this.frontendSub.bind(s_frontend);
		this.frontendSub.subscribe("".getBytes());

		// this.backend.setSwap(500000000);
		this.backendPub.bind(s_backend);
		this.monitorPub = context.socket(ZMQ.PUB);


		this.monitorPub.connect(s_monitor);

		this.active = true;
	}





	/**
	 * Log the size of the message in the producer state.
	 * @param msgsize the size of the message sent
	 * @param prodID sent by this producer
	 */
	private void logPayload(long msgsize, String prodID) {
		statistic.setThroughput(statistic.getThroughput()+ msgsize);
		if (!knownProd.isEmpty()) {
			for (int i = 0; i < knownProd.size(); i++) {
				if (prodID.equals(knownProd.get(i).getID())) {
					knownProd.get(i).addMsgSent();
					knownProd.get(i).setActive(true);
					knownProd.get(i).addBytesSent(msgsize);
					return;
				}
			}
		}
		knownProd.add(new ProducerState(prodID));
		knownProd.get(knownProd.size() - 1).addBytesSent(msgsize);
		knownProd.get(knownProd.size() - 1).addMsgSent();
		logger.info("A new challenger has come: "+prodID);

	}

	/**
	 * @return the most important producer from the list based on the 
	 * number of byte sent.
	 */
	public  String getMostProducer() {
		if (!knownProd.isEmpty()) {
			long max = 0;
			int index = 0;
			for (int i = 0; i < knownProd.size(); i++) {
				if (knownProd.get(i).getBytesSent() > max) {
					max = knownProd.get(i).getBytesSent();
					index = i;
				}
			}
			return knownProd.get(index).getID() + ","
					+ Long.toString(knownProd.get(index).getBytesSent());
		}
		return "x,x";
	}

	public void cleanShutDown() {
		logger.info("Inititating shutdown sequence");
		this.active = false;
		this.monitorPub.send("6,shutdown".getBytes(), 0);
	}

	public void run() {
		logger.info("Exchange Started");
		Timer timer = new Timer();
		timer.schedule(new Heartbeat(this.s_monitor), 0, 5000);
		timer.schedule(new ExchangeStatTimer(this, this.statistic, this.context), 10, 60000);
		int part;
		String prodID = "";
		while (this.active) {
			byte[] message;
			part = 0;
			while (true) {
				/*  ** Message multi part construction **
				 * 1: routing key
				 * 2: producer ID
				 * 3: payload
				 */ 
				message = frontendSub.recv(0);
				part++;
				if (part == 2) {
					prodID = new String(message);
				}
				if (part == 3) {
					logPayload(message.length, prodID);
				}
				backendPub.send(message, frontendSub.hasReceiveMore() ? ZMQ.SNDMORE
						: 0);
				if (!frontendSub.hasReceiveMore())
					break;
			}
			this.statistic.setProcessed(this.statistic.getProcessed()+1);
		}
	}


	/**
	 * @return the s_monitor
	 */
	public String getS_monitor() {
		return s_monitor;
	}

	/**
	 * @param s_monitor the s_monitor to set
	 */
	public void setS_monitor(String s_monitor) {
		this.s_monitor = s_monitor;
	}





	/**
	 * @return the knownProd
	 */
	public ArrayList<ProducerState> getKnownProd() {
		return knownProd;
	}


}
