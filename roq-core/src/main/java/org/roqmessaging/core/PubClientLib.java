// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.core;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.zeromq.ZMQ;

public class PubClientLib implements Runnable {
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
	private boolean running;
	private boolean tstmp;

	public PubClientLib(String monitor, int rate, int minutes, int payload,
			boolean tstmp) {
		this.context = ZMQ.context(1);
		this.monitorSub = context.socket(ZMQ.SUB);
		monitorSub.connect("tcp://" + monitor + ":5573");
		this.s_ID = UUID.randomUUID().toString();
		System.out.println("I am " + s_ID);
		monitorSub.subscribe("".getBytes());
		this.rate = rate;
		this.s_MonitorHostname = monitor;
		this.initReq = context.socket(ZMQ.REQ);
		this.initReq.connect("tcp://" + monitor + ":5572");

		this.sending = true;
		this.sent = 0;
		this.minutes = 0;
		this.minutesLimit = minutes;

		this.totalSent = 0;
		this.payloadSize = payload;

		// Init of timestamp socket. Only for benchmarking purposes
		this.tstmpReq = context.socket(ZMQ.REQ);
		this.tstmpReq.connect("tcp://" + monitor + ":5900");

		this.running = true;
		this.tstmp = tstmp;

	}

	class RateLimiter extends TimerTask {
		private ZMQ.Socket statsPub;

		public RateLimiter() {
			statsPub = context.socket(ZMQ.PUB);
			statsPub.connect("tcp://" + s_MonitorHostname + ":5800");
		}

		public void run() {
			sending = true;
			System.out.println("Sent " + sent + " messages previous minute.");
			statsPub.send(
					("12," + s_ID + "," + minutes + "," + sent).getBytes(), 0);
			totalSent += sent;
			minutes++;
			sent = 0;
			if (minutesLimit > 0 && minutes == minutesLimit) {
				statsPub.send(("11," + s_ID + "," + totalSent).getBytes(), 0);
				running = false;
			}
		}
	}

	private int init(int code) {
		// Code must be 2(first connection) or 3(panic procedure)!
		initReq.send((Integer.toString(code) + "," + s_ID).getBytes(), 0);
		String exchg = new String(initReq.recv(0));
		if (!exchg.equals("")) {
			this.exchPub = this.context.socket(ZMQ.PUB);
			this.exchPub.connect("tcp://" + exchg + ":5559");
			System.out.println("Connected to " + exchg);
			this.s_currentExchange = exchg;
			return 0;
		} else {
			System.out.println("no exchange available");
			return 1;
		}
	}

	private byte[] getTimestamp() {
		return (Long.toString(System.currentTimeMillis()) + " ").getBytes();
	}
	
	public void run() {
		while (init(2) != 0) {
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Retrying connection...");
		}

		byte[] key = "manche".getBytes();

		byte[] msg = new byte[payloadSize - 8];
		msg[msg.length - 1] = 0;

		ZMQ.Poller items = context.poller(2);
		items.register(monitorSub);
		Timer timer = new Timer();
		timer.schedule(new RateLimiter(), 60000, 60000);
		System.out.println("Producer online");
		while (running) {
			items.poll(1);
			if (items.pollin(0)) { // Info from Monitor
				String info[] = new String(monitorSub.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);

				switch (infoCode) {
				case 1:
					// Relocation notice
					if (info[1].equals(s_ID)) {
						exchPub.close();
						exchPub = context.socket(ZMQ.PUB);
						exchPub.connect("tcp://" + info[2] + ":5559");
						s_currentExchange = info[2];
						System.out.println("I'm caught! Moving to " + info[2]);
					}
					break;
				case 2:
					// Panic
					if (info[1].equals(s_currentExchange)) {
						System.out.println("Panic, my exchange is lost! "
								+ info[1]);

						exchPub.close();
						while (init(3) != 0) {
							System.out
									.println("Exchange lost. Waiting for reallocation.");
							try {
								Thread.sleep(1500);
							} catch (InterruptedException e) {

								e.printStackTrace();
							}
						}
					}
					break;
				}
			} else {
				if (sending) {
					exchPub.send(key, ZMQ.SNDMORE);
					exchPub.send(s_ID.getBytes(), ZMQ.SNDMORE);

					if (this.tstmp) {
						exchPub.send(msg, ZMQ.SNDMORE);
						exchPub.send(getTimestamp(), 0);
					}

					else {
						exchPub.send(msg, 0);
					}

					try {
						Thread.sleep(0, 100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					sent++;
					if (sent == rate)
						sending = false;
				}
			}
		}
	}

	public static void main(String[] args){
		PubClientLib pubClient = new PubClientLib(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]), Boolean.parseBoolean(args[4])); //monitor, msg/min, duration, payload
		Thread t = new Thread(pubClient);
		t.start();
	}

}