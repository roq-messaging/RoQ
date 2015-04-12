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

import java.io.IOException;
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
import org.roqmessaging.utils.LocalState;
import org.roqmessaging.utils.Time;
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
	
	private volatile boolean master;
	
	//Local State for heartbeats
	private LocalState localState;
	// Minimum time between two heartbeats (in millis)
	private long hbPeriod;
	private long lastHb;
	
	private Lock lock = new ReentrantLock();

	
	/**
	 * @param basePort default value must be 5571
	 * @param statPort default port for stat socket 5800
	 * @param qname the logical queue from which the monitor belongs to
	 * @param period the stat period for publication
	 */
	public Monitor(int basePort, int statPort, String qname, String period, String localStatePath, long hbPeriod, boolean master) {
		try {
			this.basePort = basePort;
			this.statPort = statPort;
			this.qName = qname;
			this.period = Integer.parseInt(period);
			knownHosts = new ArrayList<ExchangeState>();
			hostsToRemove = new ArrayList<Integer>();
			maxThroughput = 75000000L; // Maximum throughput per exchange, in
										// bytes/minute
			localState = new LocalState(localStatePath + "/" + basePort);
			this.hbPeriod = hbPeriod;
			this.master = master;
			context = ZMQ.context(1);

			producersPub = context.socket(ZMQ.PUB);
			producersPub.bind("tcp://*:" + (basePort + 2));
			logger.debug("Binding procuder to " + "tcp://*:" + (basePort + 2));

			brokerSub = context.socket(ZMQ.SUB);
			brokerSub.bind("tcp://*:" + (basePort));
			logger.debug("Binding broker Sub  to " + "tcp://*:" + (basePort));
			brokerSub.subscribe("".getBytes());

			initRep = context.socket(ZMQ.REP);
			initRep.bind("tcp://*:" + (basePort + 1));
			logger.debug("Init request socket to " + "tcp://*:" + (basePort + 1));

			listenersPub = context.socket(ZMQ.PUB);
			listenersPub.bind("tcp://*:" + (basePort + 3));

			heartBeat = context.socket(ZMQ.REP);
			heartBeat.bind("tcp://*:" + (basePort + 4));
			logger.debug("Heart beat request socket to " + "tcp://*:" + (basePort + 4));

		} catch (Exception e) {
			logger.error("Error while creating Monitor, ABORDED", e);
			return;
		}

		// Stat monitor init thread
		this.statMonitor = new StatisticMonitor(statPort, qname);
		new Thread(this.statMonitor).start();

		// shutodown monitor
		// initiatlisation of the shutdown thread
		this.shutDownMonitor = new ShutDownMonitor(basePort + 5, this);
		new Thread(shutDownMonitor).start();
		logger.debug("Started shutdown monitor on " + (basePort + 5));

	}
	
	/**
	 * @param address the address:front port:backport
	 * @return the index of the state in the knowhost or -1;
	 */
	private int hostLookup(String address) {
		String[] addressInfo = address.split(":");
		int index = 0;
		if(addressInfo.length==3){
			for (ExchangeState state_i : knownHosts) {
				if (state_i.match(addressInfo[0], addressInfo[1], addressInfo[2])) {
					return index;
				}else{
					index++;
				}
			}
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
			int tempP =  Integer.MAX_VALUE;;
			int indexLoad = 0;
			int indexPub = 0;
			//Less than 10% of the max throughput
			boolean coldStart = true;
			for (int i = 0; i < knownHosts.size(); i++) {
				if (knownHosts.get(i).getThroughput()>(this.maxThroughput/10)) coldStart =false;
				//Check the less overloaded
				if (knownHosts.get(i).getThroughput() < tempt) {
					logger.debug("Host "+i+ " has a throughput of "+ knownHosts.get(i).getThroughput() + " byte/min");
					tempt = knownHosts.get(i).getThroughput();
					indexLoad = i;
				}
				//Check the less assigned
				if(knownHosts.get(i).getNbProd()<tempP){
					tempP=knownHosts.get(i).getNbProd();
					indexPub = i;
				}
			}
			if(coldStart){
				//We chose the less assigned exchange
				freeHostAddress = knownHosts.get(indexPub).getAddress()+ ":"+ knownHosts.get(indexPub).getFrontPort();
				logger.info("Assigned Producer to exchange " +indexPub  + " (cold start) @ "+ freeHostAddress);
				//Then we need tp update the cache, since the number of prod will arrive with stat later
				tempP++;
				knownHosts.get(indexPub).setNbProd(tempP);
			}else{
				//we choose the less overloaded exchange
				freeHostAddress = knownHosts.get(indexLoad).getAddress()+ ":"+ knownHosts.get(indexLoad).getFrontPort();
				logger.info("Assigned Producer to exchange " +indexLoad+ " "+ freeHostAddress );
			}
			
			
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
			logger.trace("Log host procedure for "+ address +": "+ frontPort +"->"+ backPort);
			if (!knownHosts.isEmpty()) {
				for (ExchangeState exchange_i : knownHosts) {
					if(exchange_i.match(address, frontPort, backPort)){
						exchange_i.setAlive(true);
						 logger.trace("Host "+address+": "+ frontPort+"->"+ backPort+"  reported alive");
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
	 * @param address the exchange host address under the format IP:front port: back port
	 * @param throughput the current throughput
	 * @param nbprod the number of producers connected to the exchange
	 */
	private void updateExchgMetadata(String address, String throughput, String nbprod) {
		logger.debug("update Exchg Metadata");
		String[] addressInfo = address.split(":");
		if(addressInfo.length!=3){
			logger.error(new IllegalStateException("The message EVENT_MOST_PRODUCTIVE is nof formated correctly, the address is not as IP:front port:back port"));
			return;
		}
		if(!this.shuttingDown){
			for (ExchangeState state_i : knownHosts) {
				if(state_i.match(addressInfo[0], addressInfo[1], addressInfo[2])){
					state_i.setThroughput(Long.parseLong(throughput));
					state_i.setNbProd(Integer.parseInt(nbprod));
					logger.info("Host " + state_i.getAddress() + " : " + state_i.getFrontPort()+ "->" + state_i.getBackPort()+" :" + state_i.getThroughput()
							+ " bytes/min, " + state_i.getNbProd() + " users");
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
		int exch_index = hostLookup(exchg_addr);
		if (knownHosts.size() > 0 && exch_index != -1 && !this.shuttingDown) {
			logger.debug("Do we need to relocate ?");
			//TODO externalize 1.0, 0.90, and 0.20 tolerance values
			if (knownHosts.get(exch_index).getThroughput() > (java.lang.Math.round(maxThroughput * 1.10))) { 
				logger.debug("Is "+knownHosts.get(exch_index).getThroughput() + " > "+  (java.lang.Math.round(maxThroughput * 1.10)));
				//Limit case: exchange saturated with only one producer TODO raised alarm
				if (knownHosts.get(exch_index).getNbProd() == 1) { 
					logger.error("Limit Case we cannot relocate  the unique producer !");
					return "";
				} else {
					int candidate_index = getFreeHost_index(exch_index);
					if (candidate_index != -1) {
						String candidate = knownHosts.get(candidate_index).getAddress();
						if (knownHosts.get(candidate_index).getThroughput() + Long.parseLong(bytesSent) < (java.lang.Math
								.round(maxThroughput * 0.90))) {
							
							knownHosts.get(candidate_index).addThroughput(Long.parseLong(bytesSent));
							knownHosts.get(candidate_index).addNbProd();
							knownHosts.get(exch_index).lessNbProd();
							knownHosts.get(exch_index).lessThroughput(Long.parseLong(bytesSent));
							logger.info("Relocate a publisher on "+ candidate+":"+knownHosts.get(candidate_index).getFrontPort());
							return candidate+":"+knownHosts.get(candidate_index).getFrontPort();
						}else if (knownHosts.get(candidate_index).getThroughput() + Long.parseLong(bytesSent) < knownHosts
								.get(hostLookup(exchg_addr)).getThroughput()
								&& 
								(knownHosts.get(hostLookup(exchg_addr)).getThroughput() - knownHosts.get(candidate_index).getThroughput())
								> (java.lang.Math.round(knownHosts.get(	hostLookup(exchg_addr)).getThroughput() * 0.20))) {
							
							knownHosts.get(candidate_index).addThroughput(Long.parseLong(bytesSent));
							knownHosts.get(candidate_index).addNbProd();
							knownHosts.get(exch_index).lessNbProd();
							knownHosts.get(exch_index).lessThroughput(Long.parseLong(bytesSent));
							logger.info("Relocating for load optimization on "+ candidate+":"+knownHosts.get(candidate_index).getFrontPort());
							return candidate+":"+knownHosts.get(candidate_index).getFrontPort();
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
		
		ZMQ.Poller items = new ZMQ.Poller(3);
		items.register(brokerSub);//0
		items.register(initRep);//1

		logger.info("Monitor started isMaster: " + master);
		long lastPublish = System.currentTimeMillis();
		lastHb = Time.currentTimeMillis() - hbPeriod;
		
		// The Monitor active/backup mechanism
		// Remark: Once master we never becomes backup again
		while (this.active && !this.master) {
			HCMHeartbeat();
			items.poll(200);
			if (items.pollin(1)) {
				String info[] = new String(initRep.recv(0)).split(",");
				// Check if the message indicates that the monitor becomes the master
				if (Integer.parseInt(info[0]) == RoQConstant.EVENT_MONITOR_FAILOVER) {
					logger.info("Standby monitor has been activated");
					this.master = true;
					initRep.send((new Integer(RoQConstant.EVENT_MONITOR_ACTIVATED).toString() + ", ").getBytes(), 0);
				} else {
					initRep.send(("").getBytes(), 0);
				}
			}
		}
		reportTimer.schedule(this.monitorStat, 0, period);
		//2. Start the main run of the monitor
		while (this.active) {			
			//not really clean, workaround to the fact thats sockets cannot be shared between threads
			if (System.currentTimeMillis() - lastPublish > 10000) { 
				listenersPub.send(("2," + bcastExchg()).getBytes(), 0);
				lastPublish = System.currentTimeMillis();
				logger.debug("Alive hosts: " + bcastExchg() );
			}
			while (!hostsToRemove.isEmpty() && !this.shuttingDown) {
				try {
					producersPub.send((new Integer(RoQConstant.EXCHANGE_LOST).toString()+"," + knownHosts.get(hostsToRemove.get(0)).getAddress()).getBytes(), 0);
					knownHosts.remove((int) hostsToRemove.get(0));
					hostsToRemove.remove(0);
					logger.warn("Panic procedure initiated");
				}finally {
				}
			}
			
			HCMHeartbeat();
			//3. According to the channel bit used, we can define what kind of info is sent
			items.poll(100);
			if (items.pollin(0)) { // Info from Exchange
					String info[] = new String(brokerSub.recv(0)).split(",");
					// Check if exchanges are present: this happens when the
					// queue is shutting down a client is asking for a
					// connection
					if (info.length > 1) {
						infoCode = Integer.parseInt(info[0]);
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
							logger.trace("Getting Heart Beat information");
							if (info.length == 4) {
								if (logHost(info[1], info[2], info[3]) == 1) {
									listenersPub.send((new Integer(RoQConstant.REQUEST_UPDATE_EXCHANGE_LIST).toString()
											+ "," + info[1]+":"+ info[2]).getBytes(), 0);
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
						String freeHost = getFreeHostForPublisher();
						logger.debug("Received init request from producer. Assigned on " +freeHost);
						initRep.send(freeHost.getBytes(), 0);
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
		try {
			// 0 indicates that the process has been shutdown by the user & have not timed out
			localState.put("HB", 0);
		} catch (IOException e) {
			logger.error("Failed to stop properly the process, it will be restarted...");
			e.printStackTrace();
		}
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
		heartBeat.close();
	}
	
	/**
	 * Write the heartbeat in the system
	 */
	private void HCMHeartbeat() {
		// Write Heartbeat
		if ((Time.currentTimeMillis() - lastHb) >= hbPeriod) {
			try {
				long current = Time.currentTimeSecs();
				logger.info("Writing hb " + basePort + " " + current);
				localState.put("HB", current);
				lastHb = Time.currentTimeMillis();
			} catch (IOException e) {
				logger.info("Failed to write in local db: " + e);
			}
		}
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
