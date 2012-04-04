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
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.state.ExchangeState;
import org.zeromq.ZMQ;

/**
 * Class Monitor
 * <p> Description: 
 * <br>1. Maintain the state of the exchanges in place and their states. 
 * <br>2. manage the auto scaling
 * <br>3. the publisher re-allocation
 * 
 *  @author Nam-Luc Tran, Sabri Skhiri
 */
public class Monitor implements Runnable, IStoppable {

	private Logger logger = Logger.getLogger(Monitor.class);
	private ArrayList<ExchangeState> knownHosts;
	private ArrayList<Integer> hostsToRemove;
	private long maxThroughput;
	private ZMQ.Context context;
	private boolean active = true;
	private boolean useFile = false;
	
	private BufferedWriter bufferedOutput;
	private ZMQ.Socket producersPub, brokerSub, initRep, listenersPub, statSub;
	//Monitor heart beat socket, client can check that monitor is alive
	private ZMQ.Socket heartBeat = null;
	private int basePort = 0;
	//Shut down monitor
	private ShutDownMonitor shutDownMonitor;

	
	/**
	 * @param basePort default value must be 5571
	 * @param statPort default port for stat socket 5800
	 */
	public Monitor(int basePort, int statPort){
		this.basePort = basePort;
		knownHosts = new ArrayList<ExchangeState>();
		hostsToRemove = new ArrayList<Integer>();
		maxThroughput = 75000000L; // Maximum throughput per exchange, in
									// bytes/minute

		context = ZMQ.context(1);

		producersPub = context.socket(ZMQ.PUB);
		producersPub.bind("tcp://*:"+(basePort+2));
		logger.debug("Binding procuder to "+"tcp://*:"+(basePort+2));

		brokerSub = context.socket(ZMQ.SUB);
		brokerSub.bind("tcp://*:"+(basePort));
		logger.debug("Binding broker Sub  to "+"tcp://*:"+(basePort));
		brokerSub.subscribe("".getBytes());

		initRep = context.socket(ZMQ.REP);
		initRep.bind("tcp://*:"+(basePort+1));
		logger.debug("Init request socket to "+"tcp://*:"+(basePort+1));

		listenersPub = context.socket(ZMQ.PUB);
		listenersPub.bind("tcp://*:"+(basePort+3));
		
		heartBeat = context.socket(ZMQ.REP);
		heartBeat.bind("tcp://*:"+(basePort+4));
		logger.debug("Heart beat request socket to "+"tcp://*:"+(basePort+4));

		statSub = context.socket(ZMQ.SUB);
		statSub.bind("tcp://*:"+ statPort);
		statSub.subscribe("".getBytes());
		
		//shutodown monitor
		//initiatlisation of the shutdown thread TODO Test
		this.shutDownMonitor = new ShutDownMonitor(basePort+5, this);
		new Thread(shutDownMonitor).start();
		logger.debug("Started shutdown monitor on "+ (basePort+5));
		
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

	/**
	 * @return a free host address + the front port. That must be only used for publisher
	 */
	private String getFreeHostForPublisher() {
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
			freeHostAddress = knownHosts.get(index).getAddress()+ ":"+ knownHosts.get(index).getFrontPort();
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

	/**
	 * Register the host address of the exchange
	 * @param address the host address
	 * @param frontPort the front end port for publisher
	 * @param backPort the back end port for subscriber
	 * @return 1 if the host has been added otherwise it is hearbeat
	 */
	private int logHost(String address, String frontPort, String backPort) {
		int i;
		if (!knownHosts.isEmpty()) {
			for (i = 0; i < knownHosts.size(); i++) {
				if (address.equals(knownHosts.get(i).getAddress())) {
					knownHosts.get(i).setAlive(true);
					 logger.debug("Host "+address+": "+ frontPort+"->"+ backPort+"  reported alive");
					return 0;
				}
			}
		}
		logger.info("Added new host: " + address+": "+ frontPort+"->"+ backPort);
		knownHosts.add(new ExchangeState(address,frontPort, backPort ));
		return 1;
	}

	/**
	 * @return the concatenate list of exchanged registered at the monitor
	 */
	private String bcastExchg() {
		String exchList = "";
		if (!knownHosts.isEmpty()) {
			for (int i = 0; i < knownHosts.size(); i++) {
				exchList += knownHosts.get(i).getAddress()+":"+knownHosts.get(i).getBackPort();
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

	/**
	 * Evaluates the load on the exchange and select the less overloaded exchange if required.
	 * The relocation method does:
	 * 1. Get the host index in the exchange host list<br>
	 * 2. Check the max throughput value<br>
	 * 3. Check the limit case (1 publisher)<br>
	 * 4. Get a candidate: exchange that is still under the max throughput limit and that has the lower throughput<br>
	 * 5. Update the config state of the exchange candidate <br>
	 * 6. Send the relocate message to the publisher<br>
	 * @param exchg_addr the address of the current exchange
	 * @param bytesSent the bytesent per cycle sent through the exchange
	 * @return an empty string is there is nothing to do or the address of the new exchange if we need a re-location
	 */
	private String relocateProd(String exchg_addr, String bytesSent) {
		if (knownHosts.size() > 0 && hostLookup(exchg_addr) != -1) {
			int exch_index = hostLookup(exchg_addr);
			//TODO externalize 1.0, 0.90, and 0.20 tolerance values
			if (knownHosts.get(exch_index).getThroughput() > (java.lang.Math.round(maxThroughput * 1.10))) { 
				//Limit case: exchange saturated with only one producer TODO raised alarm
				if (knownHosts.get(exch_index).getNbProd() == 1) { 
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
						}else if (knownHosts.get(candidate_index).getThroughput() + Long.parseLong(bytesSent) < knownHosts
								.get(hostLookup(exchg_addr)).getThroughput()
								&& 
								(knownHosts.get(hostLookup(exchg_addr)).getThroughput() - knownHosts.get(candidate_index).getThroughput())
								> (java.lang.Math.round(knownHosts.get(	hostLookup(exchg_addr)).getThroughput() * 0.20))) {
							
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
		while (this.active & !Thread.currentThread().isInterrupted()) {
			//not really clean, workaround to the fact thats sockets cannot be shared between threads
			if (System.currentTimeMillis() - lastPublish > 10000) { 
				listenersPub.send(("2," + bcastExchg()).getBytes(), 0);
				lastPublish = System.currentTimeMillis();
				logger.info("Alive hosts: " + bcastExchg() + " , Free host: " + getFreeHostForPublisher());
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

				logger.debug("Start analysing info code, the use files ="+this.useFile);
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
				logger.debug("Recieving message from Exhange:"+infoCode  +" info array "+ info.length);
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
							 logger.debug("relocating " + info[2] + " on " + relocation);
							producersPub.send(("1," + info[2] + "," + relocation).getBytes(), 0);
						}
					}
					break;
				case 5:
					// Broker heartbeat code Registration
					if (info.length==4){
						if (logHost(info[1], info[2], info[3]) == 1) {
							listenersPub.send(("1," + info[1]).getBytes(), 0);
						}
					}	else logger.error("The message recieved from the exchange heart beat" +
							" does not contains 4 parts");
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

			if (items.pollin(2)) { // Init socket
				logger.debug("Received init request from either producer or listner");
				String info[] = new String(initRep.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);

				switch (infoCode) {
				case 1:
					logger.debug("Received init request from listener");
					initRep.send(bcastExchg().getBytes(), 0);
					break;
				case 2:
					 logger.debug("Received init request from producer. Assigned on "+ getFreeHostForPublisher());
					initRep.send(getFreeHostForPublisher().getBytes(), 0);
					break;

				case 3:
					logger.debug("Received panic init from producer");
					initRep.send(getFreeHostForPublisher().getBytes(), 0);// TODO: round
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
		closeSocket();
		logger.info("Monitor Stopped");
		// TODO send a clean shutdown to all producer and listener thread
		// special code

	}

	/**
	 * Closes all open sockets.
	 */
	private void closeSocket() {
		logger.info("Closing all sockets from " + getName());
		producersPub.close();
		brokerSub.close();
		initRep.close();
		listenersPub.close();
		statSub.close();

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


	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		logger.info("Starting the shutdown procedure...");
		this.active = false;
		
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "Monitor " + this.basePort;
	}

}
