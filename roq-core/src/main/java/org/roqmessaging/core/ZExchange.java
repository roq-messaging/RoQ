// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.zeromq.ZMQ;


public class ZExchange implements Runnable {

	private ArrayList<Producer> knownProd;
	private ZMQ.Context context;
	private ZMQ.Socket frontendSub;
	private ZMQ.Socket backendPub;
	private ZMQ.Socket monitorPub;
	private int processed;
	private String s_frontend;
	private String s_backend;
	private String s_monitor;
	private String s_monitorHostname;
	private long throughput;
	private long max_bw;
	private boolean active;
	private int totalProcessed;

	public ZExchange(String frontend, String backend, String monitor) {
		knownProd = new ArrayList<Producer>();
		processed = 0;
		this.s_monitorHostname = monitor;
		this.s_frontend = "tcp://*:" + frontend;
		this.s_backend = "tcp://*:" + backend;
		this.s_monitor = "tcp://" + monitor + ":5571";

		this.context = ZMQ.context(1);
		this.frontendSub = context.socket(ZMQ.SUB);
		this.backendPub = context.socket(ZMQ.PUB);
		// Caution, the following method as well as setSwap must be invoked before binding
		// Use these to (double) check if the settings were correctly set  
		// System.out.println(this.backend.getHWM());
		// System.out.println(this.backend.getSwap());
		this.backendPub.setHWM(3400);  

		this.frontendSub.bind(s_frontend);
		this.frontendSub.subscribe("".getBytes());

		// this.backend.setSwap(500000000);
		this.backendPub.bind(s_backend);
		this.monitorPub = context.socket(ZMQ.PUB);


		this.monitorPub.connect(s_monitor);

		this.max_bw = 5000; // bandwidth limit, in bytes/minute, per producer

		this.active = true;
	}

	class Heartbeat extends TimerTask {
		private ZMQ.Context hbcontext;
		private ZMQ.Socket hbsocket;

		public Heartbeat() {
			this.hbcontext = ZMQ.context(1);
			this.hbsocket = hbcontext.socket(ZMQ.PUB);
			hbsocket.connect(s_monitor);
		}
		public void run() {
			hbsocket.send(("5," + getLocalIP()).getBytes(), 0);
		}
	}

	class Stats extends TimerTask {
		private ZMQ.Context timercontext;
		private ZMQ.Socket timersocket;
		private ZMQ.Socket statsPub;

		private int minute;
		private long totalThroughput;

		public Stats() {
			this.timercontext = ZMQ.context(1);
			this.timersocket = timercontext.socket(ZMQ.PUB);
			timersocket.connect(s_monitor);
			this.statsPub = context.socket(ZMQ.PUB);
			this.statsPub.connect("tcp://" + s_monitorHostname + ":5800");
			this.minute = 0;
			this.totalThroughput = 0;
		}

		public void run() {
			timersocket.send(("4," + getLocalIP() + "," + getMostProducer()
					+ "," + throughput + "," + max_bw + "," + knownProd.size())
					.getBytes(), 0);
			System.out.println(getLocalIP() + " : minute " + minute + " : "
					+ throughput + " bytes/min, " + knownProd.size()
					+ " producers connected");
			System.out.println(getLocalIP() + " : minute " + minute + " : "
					+ getMostProducer());

			totalProcessed += processed;
			statsPub.send(("21," + minute + "," + totalProcessed + ","
					+ processed + "," + totalThroughput + "," + throughput
					+ "," + knownProd.size()).getBytes(), 0);

			for (int i = 0; i < knownProd.size(); i++) {
				if (!knownProd.get(i).isActive()) {
					knownProd.get(i).addInactive();
					if (knownProd.get(i).getInactive() > 0) {
						knownProd.remove(i);
					}
				} else {
					knownProd.get(i).reset();
				}
			}
			totalThroughput += throughput;
			minute++;
			throughput = 0;
			processed = 0;
		}
	}

	private void logPayload(long msgsize, String prodID) {
		throughput += msgsize;
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
		knownProd.add(new Producer(prodID));
		knownProd.get(knownProd.size() - 1).addBytesSent(msgsize);
		knownProd.get(knownProd.size() - 1).addMsgSent();
		System.out.println("A new challenger has come: "+prodID);

	}

	private class Producer {

		private long bytesSent;
		private int msgSent;
		private boolean active;
		private int inactive;
		private String ID;

		public Producer(String ID) {
			this.ID = ID;
			this.active = true;
			this.bytesSent = 0;
			this.inactive = 0;
			this.msgSent = 0;
		}

		public void addMsgSent() {
			this.msgSent++;
		}

		public String getID() {
			return ID;
		}

		public long getBytesSent() {
			return bytesSent;
		}

		public void addBytesSent(long bytesSent) {
			this.bytesSent += bytesSent;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public int getInactive() {
			return inactive;
		}

		public void addInactive() {
			this.inactive++;
		}

		public void reset() {
			this.active = false;
			this.inactive = 0;
			this.msgSent = 0;
			this.bytesSent = 0;
		}
	}

	private String getMostProducer() {
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
		System.out.println("Inititating shutdown sequence");
		this.active = false;
		this.monitorPub.send("6,shutdown".getBytes(), 0);
	}

	public void run() {
		System.out.println("Exchange Started");
		Timer timer = new Timer();
		timer.schedule(new Heartbeat(), 0, 5000);
		timer.schedule(new Stats(), 10, 60000);
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
			processed++;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		final String monitorHost = args[0];
		final ZMQ.Context shutDownContext;
		final ZMQ.Socket shutDownSocket;
		shutDownContext = ZMQ.context(1);
		shutDownSocket = shutDownContext.socket(ZMQ.PUB);
		shutDownSocket.connect("tcp://" + monitorHost + ":5571");
		shutDownSocket.setLinger(3500);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() // TODO ensure it waits few seconds in normal
								// functioning before closing everything
								// This section may need to be rewritten more
								// elegantly
			{
				try {
					Runtime.getRuntime().exec("date");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println("Shutting Down!");
				shutDownSocket.send(("6," + getLocalIP()).getBytes(), 0);
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		ZExchange exchange = new ZExchange("5559", "5560", args[0]);
		Thread t = new Thread(exchange);
		t.start();
	}

	public static String getLocalIP() {
		/**
		 * Returns the current local IP address or an empty string in error case
		 * / when no network connection is up.
		 * <p>
		 * The current machine could have more than one local IP address so
		 * might prefer to use {@link #getAllLocalIPs() } or
		 * {@link #getAllLocalIPs(java.lang.String) }.
		 * <p>
		 * If you want just one IP, this is the right method and it tries to
		 * find out the most accurate (primary) IP address. It prefers addresses
		 * that have a meaningful dns name set for example.
		 * 
		 * @return Returns the current local IP address or an empty string in
		 *         error case.
		 * @since 0.1.0
		 */
		String ipOnly = "";
		try {
			Enumeration<NetworkInterface> nifs = NetworkInterface
					.getNetworkInterfaces();
			if (nifs == null)
				return "";
			while (nifs.hasMoreElements()) {
				NetworkInterface nif = nifs.nextElement();
				// We ignore subinterfaces - as not yet needed.

				if (!nif.isLoopback() && nif.isUp() && !nif.isVirtual()) {
					Enumeration<InetAddress> adrs = nif.getInetAddresses();
					while (adrs.hasMoreElements()) {
						InetAddress adr = adrs.nextElement();
						if (adr != null
								&& !adr.isLoopbackAddress()
								&& (nif.isPointToPoint() || !adr
										.isLinkLocalAddress())) {
							String adrIP = adr.getHostAddress();
							String adrName;
							if (nif.isPointToPoint()) // Performance issues
														// getting hostname for
														// mobile internet
														// sticks
								adrName = adrIP;
							else
								adrName = adr.getCanonicalHostName();

							if (!adrName.equals(adrIP))
								return adrIP;
							else
								ipOnly = adrIP;
						}
					}
				}
			}
			return ipOnly;
		} catch (SocketException ex) {
			return "";
		}
	}

}
