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

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.client.IRoQSubscriber;
import org.zeromq.ZMQ;

import com.google.common.math.LongMath;
import com.google.common.primitives.Longs;

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

	private volatile int received=0;
	private int totalReceived=0;
	private int minute=0;

	private String subsriberID="0";

	private long latency;
	private int latenced;
	
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
	public SubscriberConnectionManager(String monitor, String monitorStat, String subKey,  boolean tstmp) {
		this.context = ZMQ.context(1);
		this.s_monitorStat = monitorStat;
		this.subkey = subKey.getBytes();
		this.subsriberID = System.currentTimeMillis()+subKey;

		//as the monitor is and address as tcp://ip:base port
		int basePort = extractBasePort(monitor);
		String portOff = monitor.substring(0, monitor.length()-"xxxx".length());
		
		this.monitorSub = context.socket(ZMQ.SUB);
		monitorSub.connect(portOff+(basePort+3));
		monitorSub.subscribe("".getBytes());

		this.initReq = this.context.socket(ZMQ.REQ);
		this.initReq.connect(portOff+(basePort+1));

		this.received = 0;
		this.totalReceived = 0;
		this.minute = 0;
		this.latency = 0;
		this.latenced = 0;

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
			logger.debug("Subscriber Connecting stat monitor on "+ s_monitorStat);
		}

		public void run() {
			totalReceived += received;

			long meanLat;
			if (latenced == 0) {
				meanLat = 0;
			} else {
				meanLat = LongMath.divide(latency,  latenced, RoundingMode.DOWN);
			}
			logger.info("Total latency: " + latency + " Received: " + received + " Latenced: " + latenced + " Mean: "
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
			this.exchSub.setRcvHWM(10000000);
			logger.info("Connnecting with RcvHWM: "+this.exchSub.getRcvHWM());
			this.exchSub.subscribe(this.subkey);
			for (int i = 0; i < brokerList.length; i++) {
				connectToBroker(brokerList[i]);
			}
			return 0;
		} else {
			logger.info("No exchange available");
			return 1;
		}
	}

	private void connectToBroker(String broker) {
		logger.info("Connecting new exchange: "+ broker);
		if (!knownHosts.contains(broker)) {
			exchSub.connect("tcp://" + broker );
			knownHosts.add(broker);
			logger.info("connected to " + broker);
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
//		Comparator<byte[]> comparator = UnsignedBytes.lexicographicalComparator();
		int counter =0;
		while (init() != 0) {
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				logger.error("Error when thread sleeping (init phase)", e);
			}
			logger.info("Retrying connection...");
		}

		this.items = new ZMQ.Poller(2);
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

				if (infoCode == RoQConstant.REQUEST_UPDATE_EXCHANGE_LIST && !info[1].equals("")) {
					// new Exchange  available message
					connectToBroker(info[1]);
					
				}
			}

			if (items.pollin(1)) {//From Exchange
				byte[] request= null;
				//Get the key
				exchSub.recv(0);
				if(exchSub.hasReceiveMore()){
					//the ID of the publisher
					request = exchSub.recv(0);
				}
				if(exchSub.hasReceiveMore()){
					//the payload
					request = exchSub.recv(0);
				}
				if(exchSub.hasReceiveMore()){
					//the time stamp
					byte[] bTimeStamp = exchSub.recv(0);
					counter++;
					if(counter ==1000){
						computeLatency(Longs.fromByteArray(bTimeStamp));
						counter=0;
					}
				}
				//logger.debug("Recieving message " +  new String(request,0,request.length) + " key : "+ new String(request,0,request.length));
				//delivering to the message listener
				this.subscriber.onEvent(request!=null?request:new byte[]{});
				received++;
			}
		}
		this.logger.debug("Closing subscriber sockets");
		timer.cancel();
		timer.purge();
		knownHosts.clear();
		this.exchSub.setLinger(0);
		this.exchSub.close();
		this.initReq.setLinger(0);
		this.initReq.close();
		logger.info("Closed.");
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
