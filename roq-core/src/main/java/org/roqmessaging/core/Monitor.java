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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class Monitor
 * <p> Description: 
 * <br>1. Maintain the state of the exchanges in place and their states. 
 * <br>2. manage the auto scaling
 * <br>3. the publisher re-allocation
 * 
 * @author Sabri Skhiri
 */
public class Monitor implements Runnable {

	private Logger logger = Logger.getLogger(Monitor.class);
	private ArrayList<ExchangeState> knownHosts;
	private ArrayList<Integer> hostsToRemove;
	private long maxThroughput;
	private ZMQ.Context context;
	private boolean active = true;
	private boolean useFile = false;
	

	private BufferedWriter bufferedOutput;

	private ZMQ.Socket producersPub, brokerSub, initRep, listenersPub, statSub;

	public Monitor() {
		knownHosts = new ArrayList<ExchangeState>();
		hostsToRemove = new ArrayList<Integer>();
		maxThroughput = 75000000L; // Maximum throughput per exchange, in
									// bytes/minute

		context = ZMQ.context(1);

		producersPub = context.socket(ZMQ.PUB);
		producersPub.bind("tcp://*:5573");

		brokerSub = context.socket(ZMQ.SUB);
		brokerSub.bind("tcp://*:5571");
		brokerSub.subscribe("".getBytes());

		initRep = context.socket(ZMQ.REP);
		initRep.bind("tcp://*:5572");

		listenersPub = context.socket(ZMQ.PUB);
		listenersPub.bind("tcp://*:5574");

		statSub = context.socket(ZMQ.SUB);
		statSub.bind("tcp://*:5800");
		statSub.subscribe("".getBytes());

		if(useFile){
			try {
				FileWriter output = new FileWriter(("output" + RoQUtils.getInstance().getFileStamp()), true);
				bufferedOutput = new BufferedWriter(output);
			} catch (IOException e) {
				logger.error("Error when openning file", e);
			}
		}else{
			bufferedOutput = new BufferedWriter(new OutputStreamWriter(System.out));
		}
	}

	private int hostLookup(String address) {
		for (int i = 0; i < knownHosts.size(); i++) {
			if (knownHosts.get(i).getAddress().equals(address))
				return i;
		}
		return -1;
	}

	private String getFreeHost() {
		String freeHostAddress = "";
		if (!knownHosts.isEmpty()) {
			long tempt = Long.MAX_VALUE;
			int index = 0;
			for (int i = 0; i < knownHosts.size(); i++) {
				if (knownHosts.get(i).getThroughput() < tempt) {
					tempt = knownHosts.get(i).getThroughput();
					index = i;
				}
			}
			freeHostAddress = knownHosts.get(index).getAddress();
		}
		return freeHostAddress;
	}

	private int getFreeHost_index(int cur_index) {
		int index = -1;
		if (!knownHosts.isEmpty()) {
			long tempt = Long.MAX_VALUE;
			for (int i = 0; i < knownHosts.size(); i++) {
				if (i == cur_index)
					continue;

				if (knownHosts.get(i).getThroughput() < tempt) {
					tempt = knownHosts.get(i).getThroughput();
					index = i;
				}
			}
		}
		return index;
	}

	private int logHost(String address) {
		int i;
		if (!knownHosts.isEmpty()) {
			for (i = 0; i < knownHosts.size(); i++) {
				if (address.equals(knownHosts.get(i).getAddress())) {
					knownHosts.get(i).setAlive(true);
					 logger.debug("Host "+address+" reported alive");
					return 0;
				}
			}
		}
		logger.info("Added new host: " + address);
		knownHosts.add(new ExchangeState(address));
		return 1;
	}

	/**
	 * @return the concatenate list of exchanged registered at the monitor
	 */
	private String bcastExchg() {
		String exchList = "";
		if (!knownHosts.isEmpty()) {
			for (int i = 0; i < knownHosts.size(); i++) {
				exchList += knownHosts.get(i).getAddress();
				if (i != knownHosts.size() - 1) {
					exchList += ",";
				}
			}
		}
		return exchList;
	}


	private void updateExchgMetadata(String address, String throughput, String nbprod) {
		for (int i = 0; i < knownHosts.size(); i++) {
			if (address.equals(knownHosts.get(i).getAddress())) {
				knownHosts.get(i).setThroughput(Long.parseLong(throughput));
				knownHosts.get(i).setNbProd(Integer.parseInt(nbprod));
				logger.info("Host " + knownHosts.get(i).getAddress() + " : " + knownHosts.get(i).getThroughput()
						+ " bytes/min, " + knownHosts.get(i).getNbProd() + " users");
			}
		}
	}

	private String relocateProd(String exchg_addr, String bytesSent) {
		if (knownHosts.size() > 0 && hostLookup(exchg_addr) != -1) {
			int exch_index = hostLookup(exchg_addr);
			if (knownHosts.get(exch_index).getThroughput() > (java.lang.Math.round(maxThroughput * 1.10))) { // TODO
																												// externalize
																												// 1.10,
																												// 0.90
																												// and
																												// 0.20
																												// tolerance
																												// values
				if (knownHosts.get(exch_index).getNbProd() == 1) { // Limit
																	// case:
																	// exchange
																	// saturated
																	// with only
																	// one prod
					return "";
				} else {
					int candidate_index = getFreeHost_index(exch_index);
					if (candidate_index != -1) {
						String candidate = knownHosts.get(candidate_index).getAddress();
						if (knownHosts.get(candidate_index).getThroughput() + Long.parseLong(bytesSent) < (java.lang.Math
								.round(maxThroughput * 0.90))) {
							knownHosts.get(candidate_index).addThroughput(Long.parseLong(bytesSent));
							knownHosts.get(candidate_index).addNbProd();
							return candidate;
						}

						else if (knownHosts.get(candidate_index).getThroughput() + Long.parseLong(bytesSent) < knownHosts
								.get(hostLookup(exchg_addr)).getThroughput()
								&& (knownHosts.get(hostLookup(exchg_addr)).getThroughput() - knownHosts.get(
										candidate_index).getThroughput()) > (java.lang.Math.round(knownHosts.get(
										hostLookup(exchg_addr)).getThroughput() * 0.20))) {
							knownHosts.get(candidate_index).addThroughput(Long.parseLong(bytesSent));
							knownHosts.get(candidate_index).addNbProd();
							logger.info("Relocating for load optimization");
							return candidate;
						}
					}
				}
			}
		}
		return "";
	}

	public void run() {
		int infoCode =0; 
		
		//1. Start the report timer
		Timer reportTimer = new Timer();
		reportTimer.schedule(new ReportExchanges(), 0, 10000);

		ZMQ.Poller items = context.poller(3);
		items.register(statSub);
		items.register(brokerSub);
		items.register(initRep);

		logger.info("Monitor started");
		long lastPublish = System.currentTimeMillis();
		
		//2. Start the main run of the monitor
		while (this.active) {
			//not really clean, workaround to the fact thats sockets cannot be shared between threads
			if (System.currentTimeMillis() - lastPublish > 10000) { 
				listenersPub.send(("2," + bcastExchg()).getBytes(), 0);
				lastPublish = System.currentTimeMillis();
				logger.info("Alive hosts: " + bcastExchg() + " , Free host: " + getFreeHost());
			}
			while (!hostsToRemove.isEmpty()) {
				producersPub.send(("2," + knownHosts.get(hostsToRemove.get(0)).getAddress()).getBytes(), 0);
				knownHosts.remove((int) hostsToRemove.get(0));
				hostsToRemove.remove(0);
				logger.warn("Panic procedure initiated");
			}

			items.poll(2000);

			//3. According to the channel bit used, we can define what kind of info is sent
			if (items.pollin(RoQConstant.CHANNEL_STAT)) { 
				// Stats info: 1* producers, 2* exch, 3*listeners
				String info[] = new String(statSub.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);

				logger.debug("Start analysing info code, the use fils ="+this.useFile);
				switch (infoCode) {
				case 11:
					logger.info("1 producer finished, sent " + info[2] + " messages.");
					try {
						bufferedOutput.write("PROD," + RoQUtils.getInstance().getFileStamp() + ",FINISH," + info[1]
								+ "," + info[2]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case 12:
					try {
						bufferedOutput.write("PROD," + RoQUtils.getInstance().getFileStamp() + ",STAT," + info[1] + ","
								+ info[2] + "," + info[3]);
						bufferedOutput.newLine();
						bufferedOutput.flush();

					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case 21:
					try {
						bufferedOutput.write("EXCH," + RoQUtils.getInstance().getFileStamp() + "," + info[1] + ","
								+ info[2] + "," + info[3] + "," + info[4] + "," + info[5] + "," + info[6]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case 31:
					try {
						bufferedOutput.write("LIST," + RoQUtils.getInstance().getFileStamp() + "," + info[1] + ","
								+ info[2] + "," + info[3] + "," + info[4] + "," + info[5]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				}
			}

			if (items.pollin(RoQConstant.CHANNEL_EXCHANGE)) { // Info from Exchange
				String info[] = new String(brokerSub.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);

				switch (infoCode) {
				case 3:
					// Broker debug code
					logger.info(info[1]);
					break;
				case 4:
					// Broker most productive producer code
					updateExchgMetadata(info[1], info[4], info[6]);
					if (!info[1].equals("x")) {
						String relocation = relocateProd(info[1], info[3]); // ip,bytessent
						if (!relocation.equals("")) {
							// logger.info("relocating " + info[2] +
							// " on " + relocation);
							producersPub.send(("1," + info[2] + "," + relocation).getBytes(), 0);
						}
					}
					break;
				case 5:
					// Broker heartbeat code
					if (logHost(info[1]) == 1) {
						listenersPub.send(("1," + info[1]).getBytes(), 0);
					}
					break;

				case 6:
					// Broker shutdown notification
					logger.info("Broker " + info[1] + " has left the building");

					if (hostLookup(info[1]) != -1) {
						knownHosts.remove(hostLookup(info[1]));
					}

					producersPub.send(("2," + info[1]).getBytes(), 0);
					break;
				}
			}

			if (items.pollin(RoQConstant.CHANNEL_INIT)) { // Init socket

				String info[] = new String(initRep.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);

				switch (infoCode) {
				case 1:
					logger.debug("Received init request from listener");
					initRep.send(bcastExchg().getBytes(), 0);
					break;
				case 2:
					 logger.debug("Received init request from producer. Assigned on "+ getFreeHost());
					initRep.send(getFreeHost().getBytes(), 0);
					break;

				case 3:
					logger.debug("Received panic init from producer");
					initRep.send(getFreeHost().getBytes(), 0);// TODO: round
																// robin return
																// knownHosts.
																// should be
																// aware of the
																// profile of
																// the producer
					break;
				}
			}
		}
		// Exit running
		reportTimer.cancel();
		this.knownHosts.clear();
		logger.info("Monitor Stopped");
		// TODO send a clean shutdown to all producer and listener thread
		// special code

	}

	/**
	 * removes states before stopping.
	 */
	public void cleanShutDown() {
		this.active = false;
	}
	

	class ReportExchanges extends TimerTask {
		public void run() {
			if (!knownHosts.isEmpty()) {
				for (int i = 0; i < knownHosts.size(); i++) {
					if (!knownHosts.get(i).isAlive()) {
						knownHosts.get(i).addLost();
						if (knownHosts.get(i).getLost() > 0) {
							logger.info(knownHosts.get(i).getAddress() + " is lost");
							hostsToRemove.add(i);
						}
					} else {
						knownHosts.get(i).setAlive(false);
					}
				}
			}
		}
	}

}
