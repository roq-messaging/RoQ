// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roq.simulation.clientLib;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.zeromq.ZMQ;

public class PubClientLib implements Runnable {
	private Logger logger = Logger.getLogger(PubClientLib.class);

	private ZMQ.Context context;
	private ZMQ.Socket exchPub;
	private String s_ID;
	private ZMQ.Socket monitorSub;
	private int rate;
	private String s_currentExchange;
	private String s_MonitorHostname;
	private int payloadSize;

	private ZMQ.Socket initReq;

	private ZMQ.Socket tstmpReq;

	private int sent;
	private int totalSent;

	private boolean sending;
	private int minutes;
	private int minutesLimit;
	//Volatile to force a flush in main memory
	private volatile boolean running;
	private boolean tstmp;

	public PubClientLib(String monitor, int rate, int minutes, int payload, boolean tstmp) {
		this.tstmp = tstmp;
		this.context = ZMQ.context(1);
		this.monitorSub = context.socket(ZMQ.SUB);
		monitorSub.connect("tcp://" + monitor + ":5573");
		this.s_ID = UUID.randomUUID().toString();
		logger.info("I am " + s_ID);
		monitorSub.subscribe("".getBytes());
		this.rate = rate;
		this.s_MonitorHostname = monitor;
		this.initReq = context.socket(ZMQ.REQ);
		this.initReq.connect("tcp://" + monitor + ":5572");
		logger.info(" Connected to monitor : tcp://" + monitor + ":5572");

		this.sending = true;
		this.sent = 0;
		this.minutes = 0;
		this.minutesLimit = minutes;

		this.totalSent = 0;
		this.payloadSize = payload;

		if (tstmp) {
			// Init of timestamp socket. Only for benchmarking purposes
			this.tstmpReq = context.socket(ZMQ.REQ);
			this.tstmpReq.connect("tcp://" + monitor + ":5900");
			this.logger.debug("using time stamp server: "+ tstmp);
		}
		this.running = true;
		logger.info(" Publisher " + this.s_ID + " is running");
	}

	class RateLimiter extends TimerTask {
		private ZMQ.Socket statsPub;

		public RateLimiter() {
			statsPub = context.socket(ZMQ.PUB);
			statsPub.connect("tcp://" + s_MonitorHostname + ":5800");
		}

		public void run() {
			sending = true;
			logger.info("Sent " + sent + " messages previous minute.");
			statsPub.send(("12," + s_ID + "," + minutes + "," + sent).getBytes(), 0);
			totalSent += sent;
			minutes++;
			sent = 0;
			if (minutesLimit > 0 && minutes == minutesLimit) {
				statsPub.send((new Integer(RoQConstant.STAT_TOTAL_SENT).toString()+"," + s_ID + "," + totalSent).getBytes(), 0);
				running = false;
			}
		}
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
			this.exchPub = this.context.socket(ZMQ.PUB);
			this.exchPub.connect("tcp://" + exchg + ":5559");
			logger.info("Connected to " + exchg);
			this.s_currentExchange = exchg;
			return 0;
		} else {
			logger.info("no exchange available");
			return 1;
		}
	}

	private byte[] getTimestamp() {
		return (Long.toString(System.currentTimeMillis()) + " ").getBytes();
	}

	public void run() {
		logger.debug("Starting the publisher "+this.s_ID);
		//0 means that the list of exchange has been received 
		while (init(2) != 0) {
			try {
				logger.info("Retrying connection...");
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		byte[] key = "manche".getBytes();

		byte[] msg = new byte[payloadSize - 8];
		msg[msg.length - 1] = 0;

		ZMQ.Poller items = context.poller(2);
		items.register(monitorSub);
		
		Timer timer = new Timer();
		timer.schedule(new RateLimiter(), 3000, 60000);
		logger.info("Producer online");
		while (running) {
			items.poll(200);
			if (running && items.pollin(0)) { // Info from Monitor
				String info[] = new String(monitorSub.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);

				switch (infoCode) {
				case 1:
					// Relocation notice
					// Because the message is broadcasting to all publishers we need to filter on ID
					if (info[1].equals(s_ID)) {
						exchPub.close();
						exchPub = context.socket(ZMQ.PUB);
						exchPub.connect("tcp://" + info[2] + ":5559");
						s_currentExchange = info[2];
						logger.info("I'm caught! Moving to " + info[2]);
					}
					break;
				case 2:
					// Panic
					if (info[1].equals(s_currentExchange)) {
						logger.warn("Panic, my exchange is lost! " + info[1]);
						exchPub.close();
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
			} else {
				//In this case the pollin index does not come from the monitor
				if (sending) {
					exchPub.send(key, ZMQ.SNDMORE);
					exchPub.send(s_ID.getBytes(), ZMQ.SNDMORE);

					if (this.tstmp) {
						exchPub.send(msg, ZMQ.SNDMORE);
						exchPub.send(getTimestamp(), 0);
					}else {
						exchPub.send(msg, 0);
					}

					try {
						Thread.sleep(0, 100);
					} catch (InterruptedException e) {
						logger.error("Error when thread sleeping (sending phase)", e);
					}
					sent++;
					if (sent == rate)
						sending = false;
				}
			}
		}
	}

}