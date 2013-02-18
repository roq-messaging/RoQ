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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.stat.StatisticMonitor;
import org.roqmessaging.core.timer.MonitorStatTimer;
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
	//List of exchanges and their state
	private ArrayList<ExchangeState> knownHosts;
	//Position of the host to remove in the know host list
	private ArrayList<Integer> hostsToRemove;
	private long maxThroughput;
	//define whether the thread is active
	private volatile boolean active = true;
	//define whether the thread is shutting down
	private volatile boolean shuttingDown = false;
	private ZMQ.Context context;
	private ZMQ.Socket producersPub, brokerSub, initRep, listenersPub;
	//Monitor heart beat socket, client can check that monitor is alive
	private ZMQ.Socket heartBeat = null;
	private int basePort = 0, statPort =0;
	
	//The handle to the statistic monitor thread
	private StatisticMonitor statMonitor = null;
	//Shut down monitor
	private ShutDownMonitor shutDownMonitor;
	//The monitor statistic.
	private MonitorStatTimer monitorStat;
	private int period =60000;
	//The queue name
	private String qName = "name";
	
	private Lock lock = new ReentrantLock();

	
	/**
	 * @param basePort default value must be 5571
	 * @param statPort default port for stat socket 5800
	 * @param qname the logical queue from which the monitor belongs to
	 * @param period the stat period for publication
	 */
	public Monitor(int basePort, int statPort, String qname, String period){
		this.basePort = basePort;
		this.statPort = statPort;
		this.qName = qname;
		this.period = Integer.parseInt(period);
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
		
		//Stat monitor init thread
		this.statMonitor = new StatisticMonitor(statPort, qname);
		new Thread(this.statMonitor).start();
		
		//shutodown monitor
		//initiatlisation of the shutdown thread
		this.shutDownMonitor = new ShutDownMonitor(basePort+5, this);
		new Thread(shutDownMonitor).start();
		logger.debug("Started shutdown monitor on "+ (basePort+5));
		
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
		if(!this.shuttingDown){
			logger.debug("Log host procedure for "+ address +": "+ frontPort +"->"+ backPort);
			if (!knownHosts.isEmpty()) {
				for (ExchangeState exchange_i : knownHosts) {
					if(exchange_i.match(address, frontPort, backPort)){
						exchange_i.setAlive(true);
						 logger.debug("Host "+address+": "+ frontPort+"->"+ backPort+"  reported alive");
						return 0;
					}
				}
			}
			logger.info("Added new host: " + address+": "+ frontPort+"->"+ backPort);
			knownHosts.add(new ExchangeState(address,frontPort, backPort ));
			return 1;
		}else{
			logger.warn("We are not going to log the host "+ address + " the monitor is shutting down");
		}
		return 0;
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


	/**
	 * @param address the exchange host address 
	 * @param throughput the current throughput
	 * @param nbprod the number of producers connected to the exchange
	 */
	private void updateExchgMetadata(String address, String throughput, String nbprod) {
		logger.debug("update Exchg Metadata");
		if(!this.shuttingDown){
			for (int i = 0; i < knownHosts.size(); i++) {
				if (address.equals(knownHosts.get(i).getAddress())) {
					knownHosts.get(i).setThroughput(Long.parseLong(throughput));
					knownHosts.get(i).setNbProd(Integer.parseInt(nbprod));
					logger.info("Host " + knownHosts.get(i).getAddress() + " : " + knownHosts.get(i).getThroughput()
							+ " bytes/min, " + knownHosts.get(i).getNbProd() + " users");
				}
			}
		}else{
			logger.warn("We are not going to log the host "+ address + " the monitor is shutting down");
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
		logger.debug("Relocate exchange procedure");
		if (knownHosts.size() > 0 && hostLookup(exchg_addr) != -1 && !this.shuttingDown) {
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
		
		//1. Start the report timer & the queue stat timer
		Timer reportTimer = new Timer();
		reportTimer.schedule(new ReportExchanges(), 0, period+10);
		this.monitorStat = new MonitorStatTimer(this);
		reportTimer.schedule(this.monitorStat, 0, period);

		ZMQ.Poller items = context.poller(3);
		items.register(brokerSub);//0
		items.register(initRep);//1

		logger.info("Monitor started");
		long lastPublish = System.currentTimeMillis();
		
		//2. Start the main run of the monitor
		while (this.active) {
			//not really clean, workaround to the fact thats sockets cannot be shared between threads
			if (System.currentTimeMillis() - lastPublish > 10000) { 
				listenersPub.send(("2," + bcastExchg()).getBytes(), 0);
				lastPublish = System.currentTimeMillis();
				logger.info("Alive hosts: " + bcastExchg() + " , Free host: " + getFreeHostForPublisher());
			}
			while (!hostsToRemove.isEmpty() && !this.shuttingDown) {
				try {
					this.lock.lock();
					producersPub.send((new Integer(RoQConstant.EXCHANGE_LOST).toString()+"," + knownHosts.get(hostsToRemove.get(0)).getAddress()).getBytes(), 0);
					knownHosts.remove((int) hostsToRemove.get(0));
					hostsToRemove.remove(0);
					logger.warn("Panic procedure initiated");
				}finally {
					this.lock.unlock();
				}
			}
			
			//3. According to the channel bit used, we can define what kind of info is sent
			items.poll(2000);
			if (items.pollin(0)) { // Info from Exchange
								String info[] = new String(brokerSub.recv(0)).split(",");
					// Check if exchanges are present: this happens when the
					// queue is shutting down a client is asking for a
					// connection
					if (info.length > 1) {
						infoCode = Integer.parseInt(info[0]);
						logger.debug("Recieving message from Exhange:" + infoCode + " info array " + info.length);
						switch (infoCode) {
						case RoQConstant.DEBUG:
							// Broker debug code
							logger.info(info[1]);
							break;
						case RoQConstant.EVENT_MOST_PRODUCTIVE:
							// Broker most productive producer code
							updateExchgMetadata(info[1], info[4], info[6]);
							if (!info[1].equals("x")) {
								String relocation = relocateProd(info[1], info[3]); // ip,bytessent
								if (!relocation.equals("")) {
									logger.debug("relocating " + info[2] + " on " + relocation);
									producersPub.send((new Integer(RoQConstant.REQUEST_RELOCATION).toString() + ","
											+ info[2] + "," + relocation).getBytes(), 0);
								}
							}
							break;
						case RoQConstant.EVENT_HEART_BEAT:
							// Broker heartbeat code Registration
							if (info.length == 4) {
								if (logHost(info[1], info[2], info[3]) == 1) {
									listenersPub.send((new Integer(RoQConstant.REQUEST_UPDATE_EXCHANGE_LIST).toString()
											+ "," + info[1]).getBytes(), 0);
								}
							} else
								logger.error("The message recieved from the exchange heart beat"
										+ " does not contains 4 parts");
							break;

						case RoQConstant.EVENT_EXCHANGE_SHUT_DONW:
							// Broker shutdown notification
							logger.info("Broker " + info[1] + " has left the building");

							if (hostLookup(info[1]) != -1 && !this.shuttingDown) {
								knownHosts.remove(hostLookup(info[1]));
							}

							producersPub.send(
									(new Integer(RoQConstant.EXCHANGE_LOST).toString() + "," + info[1]).getBytes(), 0);
							break;
						}
					} else {
						logger.error(" Error when recieving information from Exchange there is no  info code !",
								new IllegalStateException("The exchange sent a request" + " without info code !"));
					}
			
			}

			if (items.pollin(1)) { // Init socket
				logger.debug("Received init request from either producer or listner");
				String info[] = new String(initRep.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);
				if (!this.shuttingDown) {
					switch (infoCode) {
					case RoQConstant.CHANNEL_INIT_SUBSCRIBER:
						logger.debug("Received init request from listener");
						initRep.send(bcastExchg().getBytes(), 0);
						break;
					case RoQConstant.CHANNEL_INIT_PRODUCER:
						logger.debug("Received init request from producer. Assigned on " + getFreeHostForPublisher());
						initRep.send(getFreeHostForPublisher().getBytes(), 0);
						break;

					case 3:
						logger.debug("Received panic init from producer");
						initRep.send(getFreeHostForPublisher().getBytes(), 0);
						// TODO: round
						// robin return
						// knownHosts.
						// should be
						// aware of the
						// profile of
						// the producer
						break;
					}
				}else{
					logger.info("Monitor is shuting down, no Exchange can be allocated");
					initRep.send("".getBytes(), 0);
				}
				
			}
		}
		// Exit running
		this.monitorStat.shutTdown();
		this.knownHosts.clear();
		reportTimer.cancel();
		closeSocket();
		logger.info("Monitor  "+ this.basePort+" Stopped");
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
	}


	/**
	 * Class ReportExchanges
	 * <p> Description: if a host do not send an heartbeat 
	 * it is considered as dead.
	 * 
	 * @author sskhiri
	 */
	class ReportExchanges extends TimerTask {
		public void run() {
			try {
				lock.lock();
				if (!knownHosts.isEmpty() && !shuttingDown) {
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
			} finally {
				lock.unlock();
			}
		}
	}


	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		this.shuttingDown = true;
		try {
			logger.info("Starting the shutdown procedure...");
			this.lock.lock();
			// Stopping all exchange
			logger.info("Stopping all exchanges...(" + this.knownHosts.size() + ")");
			for (ExchangeState exchangeState_i : this.knownHosts) {
				String address = exchangeState_i.getAddress();
				int backport = exchangeState_i.getBackPort();
				logger.info("Stopping exchange on " + address + ":" + (backport + 1));
				ZMQ.Socket shutDownExChange = ZMQ.context(1).socket(ZMQ.REQ);
				shutDownExChange.setSendTimeOut(-1);
				shutDownExChange.connect("tcp://" + address + ":" + (backport + 1));
				if (!shutDownExChange.send(Integer.toString(RoQConstant.SHUTDOWN_REQUEST).getBytes(), 0)) {
					logger.error("Error while sending shutdown request to exchange", new IllegalStateException(
							"The message has not been sent"));
				} else {
					logger.info("Sent success fully Stopping exchange on " + address + ":" + (backport + 1));
				}
				shutDownExChange.close();
			}
			this.statMonitor.shutDown();
			this.active = false;
			//When we shut down the exchanges, they send a exchange lost notification.
			//Some time the notification does not arrive because the monitor has already left
			//This could lead to not remove the exchange properly.
			Thread.sleep(2500);
		} catch (Exception  e) {
			logger.error("Error when running the monitor ", e);
		}finally{
			this.lock.unlock();
		}
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return this.qName +":"+ this.basePort;
	}
	
	
	/**
	 * @return list of registered exchanges and their current states.
	 */
	public ArrayList<ExchangeState> getExhcangeMetaData(){
		return this.knownHosts;
	}

	/**
	 * @return the statPort
	 */
	public int getStatPort() {
		return statPort;
	}

	/**
	 * @param statPort the statPort to set
	 */
	public void setStatPort(int statPort) {
		this.statPort = statPort;
	}

}
