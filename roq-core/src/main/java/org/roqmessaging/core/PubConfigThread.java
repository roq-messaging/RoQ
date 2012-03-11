// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.core;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.roqmessaging.state.PublisherConfigState;
import org.zeromq.ZMQ;

public class PubConfigThread implements Runnable {
	private Logger logger = Logger.getLogger(PubConfigThread.class);

	private ZMQ.Context context;
	private String s_ID;
	private ZMQ.Socket monitorSub;
	private String s_currentExchange;

	private ZMQ.Socket initReq;
	private ZMQ.Socket tstmpReq;

	// Mesage to send
	byte[] key, msg;
	//Volatile to force a flush in main memory
	private volatile boolean running;
	//The publisher state DAO for handling the conf
	private PublisherConfigState configState = null;
	

	public PubConfigThread(String monitor, boolean tstmp) {
		this.context = ZMQ.context(1);
		this.monitorSub = context.socket(ZMQ.SUB);
		monitorSub.connect("tcp://" + monitor + ":5573");
		this.s_ID = UUID.randomUUID().toString();
		monitorSub.subscribe("".getBytes());
		this.initReq = context.socket(ZMQ.REQ);
		this.initReq.connect("tcp://" + monitor + ":5572");
		this.configState = new PublisherConfigState();
		this.configState.setMonitor(monitor);
		this.configState.setTimeStampServer(tstmp);
		this.configState.setPublisherID(this.s_ID);
		logger.info("Publisher client thread " + s_ID+" Connected to monitor : tcp://" + monitor + ":5572");

		if (tstmp) {
			// Init of timestamp socket. Only for benchmarking purposes
			this.tstmpReq = context.socket(ZMQ.REQ);
			this.tstmpReq.connect("tcp://" + monitor + ":5900");
			this.logger.debug("using time stamp server: "+ tstmp);
		}
		this.running = true;
		logger.info(" Publisher " + this.s_ID + " is running");
	}

	/**
	 * Initialize the connection to the exchange the publisher must send the message. It as asks the monitoring
	 * the list of available exchanges.
	 * @param code the code that must sent to the monitor 2 for the first connection and 3 in panic mode
	 * @return 1 if the list of exchanges received is empty, 1 otherwise
	 */
	private int init(int code) {
		// Code must be 2(first connection) or 3(panic procedure)!
		initReq.send((Integer.toString(code) + "," + s_ID).getBytes(), 0);
		//The answer must be the concatenated list of exchange
		String exchg = new String(initReq.recv(0));
		if (!exchg.equals("")) {
			try {
				this.configState.getLock().lock();
				this.configState.setExchPub(this.context.socket(ZMQ.PUB));
				this.configState.getExchPub().connect("tcp://" + exchg + ":5559");
				this.configState.setValid(true);
			}finally{
				this.configState.getLock().unlock();
			}
			logger.info("Connected to Exchange " + exchg);
			this.s_currentExchange = exchg;
			return 0;
		} else {
			logger.info("no exchange available");
			return 1;
		}
	}

	public void run() {
		logger.debug("Starting the publisher "+this.s_ID + " Thread");
		//0 means that the list of exchange has been received 
		while (init(2) != 0) {
			try {
				logger.info("Retrying connection...");
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Register in Pollin 0 position the monitor
		ZMQ.Poller items = context.poller(2);
		items.register(monitorSub);
		
		logger.info("Producer online");
		while (running) {
			items.poll(1);
			if (items.pollin(0)) { // Info from Monitor
				String info[] = new String(monitorSub.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);

				switch (infoCode) {
				case 1:
					// Relocation notice
					// Because the message is broadcasting to all publishers we need to filter on ID
					if (info[1].equals(s_ID)) {
						rellocateExchange(info[2]);
					}
					break;
				case 2:
					// Panic
					if (info[1].equals(s_currentExchange)) {
						logger.warn("Panic, my exchange is lost! " + info[1]);
						closeConnection();
						//Try to reconnect to new exchange - asking to monitor for reallocation
						while (init(3) != 0) {
							logger.warn("Exchange lost. Waiting for reallocation...");
							try {
								Thread.sleep(1500);
							} catch (InterruptedException e) {
								logger.error("Error when thread sleeping (re-allocation phase", e);
							}
						}
					}
					break;
				}
			}
		}
	}

	/**
	 * Closes the connection to the current exchange
	 */
	private void closeConnection() {
		try {
			this.configState.getLock().lock();
			this.configState.getExchPub().close();
			this.configState.setValid(false);
		} finally {
			this.configState.getLock().unlock();
		}
		
	}

	/**
	 * Rellocate the publisher configuration to another exchange
	 * @param exchange the new exchange to relocate the publisher configuration.
	 */
	private void rellocateExchange(String exchange) {
		try{
			this.configState.getLock().lock();
			this.configState.getExchPub().close();
			this.configState.setExchPub(context.socket(ZMQ.PUB));
			this.configState.getExchPub().connect("tcp://" + exchange + ":5559");
			this.configState.setValid(true);
			s_currentExchange = exchange;
			logger.info("Re-allocation order -  Moving to " + exchange);
		}finally{
			this.configState.getLock().unlock();
		}
	}

	/**
	 * @return the current configuration from which the {@linkplain PublisherClient} will connect the exchange.
	 */
	public PublisherConfigState getConfiguration(){
		return this.configState;
	}

}