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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQSubscriber;
import org.zeromq.ZMQ;

/**
 * Class SubClientLib
 * <p> Description: The subscriber client library used for receiving messages.
 * 
 * @author Nam-Luc Tran
 */
public class SubscriberConnectionManager implements Runnable {
	private Logger logger = Logger.getLogger(SubscriberConnectionManager.class);

	private ZMQ.Context context;
	private String s_monitorStat;
	private ZMQ.Poller items;
	private byte[] subkey;

	private ZMQ.Socket initReq;

	private ArrayList<String> knownHosts;

	private ZMQ.Socket monitorSub;
	private ZMQ.Socket exchSub;

	private ZMQ.Socket tstmpReq;

	private int received=0;
	private int totalReceived=0;
	private int minute=0;

	private int subsriberID=0;

	private long latency;
	private int latenced;
	private boolean tstmp;
	
	//Define when the thread must stop
	private volatile boolean running = true;
	//Ssubscriber to deliver the message
	private IRoQSubscriber subscriber = null;
	
	/**
	 * @param monitor the monitor address to bind
	 * @param monitorStat the monitor stat address to bind
	 * @param subKey the subscriber must filter on that key
	 * @param ID the subscriber ID
	 * @param tstmp true if we use a timestamp server
	 */
	public SubscriberConnectionManager(String monitor, String monitorStat, String subKey, int ID, boolean tstmp) {
		this.context = ZMQ.context(1);
		this.s_monitorStat = monitorStat;
		this.subkey = subKey.getBytes();

		//as the monitor is and address as tcp://ip:base port
		int basePort = extractBasePort(monitor);
		String portOff = monitor.substring(0, monitor.length()-"xxxx".length());
		
		this.monitorSub = context.socket(ZMQ.SUB);
		monitorSub.connect(portOff+(basePort+3));
		monitorSub.subscribe(subkey);

		this.initReq = this.context.socket(ZMQ.REQ);
		this.initReq.connect(portOff+(basePort+1));

		this.received = 0;
		this.totalReceived = 0;
		this.minute = 0;
		this.subsriberID = ID;
		this.latency = 0;
		this.latenced = 0;

		this.tstmp = tstmp;
		if (tstmp) {
			this.tstmpReq = context.socket(ZMQ.REQ);
			this.tstmpReq.connect(portOff + ":5900");
		}
	}

	class Stats extends TimerTask {
		private ZMQ.Socket statsPub;

		public Stats() {
			this.statsPub = context.socket(ZMQ.PUB);
			statsPub.connect(s_monitorStat);
		}

		public void run() {
			totalReceived += received;

			long meanLat;
			if (latenced == 0) {
				meanLat = 0;
			} else {
				meanLat = Math.round(latency / latenced);
			}
			logger.debug("Total latency: " + latency + " Received: " + received + " Latenced: " + latenced + " Mean: "
					+ meanLat + " " + "milliseconds");

			statsPub.send(
					(new Integer(RoQConstant.STAT_TOTAL_RCVD).toString()+"," + minute + "," + totalReceived + "," + received + "," + subsriberID + "," + meanLat).getBytes(), 0);
			minute++;
			received = 0;
			latency = 0;
			latenced = 0;
		}
	}

	/**
	 * <br>
	 * 1. send an hello msg to the monitor <br>
	 * 2. receives the list of broker (Exchanges) to subscribe
	 * 
	 * @return 0 if it connects to exchanges
	 */
	private int init() {
		logger.info("Init sequence");
		initReq.send((RoQConstant.CHANNEL_INIT_SUBSCRIBER + ",Hello").getBytes(), 0);
		String response = new String(initReq.recv(0));
		if (!response.equals("")) {
			String[] brokerList = response.split(",");
			this.exchSub = context.socket(ZMQ.SUB);
			this.exchSub.subscribe("".getBytes());
			for (int i = 0; i < brokerList.length; i++) {
				exchSub.connect("tcp://" + brokerList[i] );
				knownHosts.add(brokerList[i]);
				logger.info("connected to " + brokerList[i]);
			}
			return 0;
		} else {
			logger.info("No exchange available");
			return 1;
		}
	}

	private void computeLatency(long recLat) {
		long nowi = System.currentTimeMillis();
		// long nowi = //use getTimestamp//
		latency = latency + (nowi - recLat);
		latenced++;
		if (nowi - recLat < 0) {
			logger.info("ERROR: now = " + nowi + " ,recLat = " + recLat);
		}
	}

	@SuppressWarnings("unused")
	private byte[] getTimestamp() {
		tstmpReq.send("".getBytes(), 0);
		return tstmpReq.recv(0);
	}
	
	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * @param running the running to set
	 */
	public void shutdown(){
		this.running = false;
	}
	
	/**
	 * @param listener the subscriber to register.
	 */
	public void setMessageListener(IRoQSubscriber listener){
		this.subscriber = listener;
	}
	

	public void run() {
		knownHosts = new ArrayList<String>();
		while (init() != 0) {
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				logger.error("Error when thread sleeping (init phase)", e);
			}
			logger.info("Retrying connection...");
		}

		this.items = context.poller();
		this.items.register(monitorSub);
		this.items.register(exchSub);

		Timer timer = new Timer();
		timer.schedule(new Stats(), 0, 60000);

		logger.info("Worker connected");

		while (running) {
			items.poll(10);
			if (items.pollin(0)) { // Info from Monitor

				String info[] = new String(monitorSub.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);

				if (infoCode == RoQConstant.REQUEST_UPDATE_EXCHANGE_LIST && !info[1].equals("")) { // new Exchange
															// available message
					logger.info("listening to " + info[1]);
					if (!knownHosts.contains(info[1])) {
						exchSub.connect("tcp://" + info[1] );
						knownHosts.add(info[1]);
					}
				}
			}

			if (items.pollin(1)) {//From Exchange
				byte[] request;
				request = exchSub.recv(0);
				int part = 1;
				byte[] key = request;
				while (exchSub.hasReceiveMore()) {
					request = exchSub.recv(0);
					part++;
					if (part == 4 && this.tstmp) {
						computeLatency(Long.parseLong(new String(request, 0, request.length - 1)));
					}
				}
				//logger.debug("Recieving message " +  new String(request,0,request.length) + " key : "+ new String(request,0,request.length));
				//delivering to the message listener
				if(Arrays.equals(subkey, key)){
					this.subscriber.onEvent(request);
				}
				received++;
			}
		}
		knownHosts.clear();
		this.exchSub.close();
		this.initReq.close();
	}
	
	/**
	 * @param monitor the host address: tcp://ip:port
	 * @return the port as an int
	 */
	private int extractBasePort(String monitor) {
		String segment = monitor.substring("tcp://".length());
		return Integer.parseInt(segment.substring(segment.indexOf(":")+1));
	}

}
