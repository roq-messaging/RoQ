// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.core;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.zeromq.ZMQ;

public class SubClientLib implements Runnable {
	private ZMQ.Context context;
	private String s_monitor;
	private ZMQ.Poller items;
	private byte[] key;

	private ZMQ.Socket initReq;

	private ArrayList<String> knownHosts;

	private ZMQ.Socket monitorSub;
	private ZMQ.Socket exchSub;

	private ZMQ.Socket tstmpReq;

	private int received;
	private int totalReceived;
	private int minute;

	private int ID;

	private long latency;
	private int latenced;
	private boolean tstmp;

	public SubClientLib(String monitor, String subKey, int ID, boolean tstmp) {
		this.context = ZMQ.context(1);
		this.s_monitor = "tcp://" + monitor;
		this.key = subKey.getBytes();

		this.monitorSub = context.socket(ZMQ.SUB);
		monitorSub.connect(s_monitor + ":5574");
		monitorSub.subscribe(key);

		this.initReq = this.context.socket(ZMQ.REQ);
		this.initReq.connect("tcp://" + monitor + ":5572");

		this.received = 0;
		this.totalReceived = 0;
		this.minute = 0;
		this.ID = ID;
		this.latency = 0;
		this.latenced = 0;

		this.tstmpReq = context.socket(ZMQ.REQ);
		this.tstmpReq.connect("tcp://" + monitor + ":5900");
		this.tstmp = tstmp;
	}

	class Stats extends TimerTask {
		private ZMQ.Socket statsPub;

		public Stats() {
			this.statsPub = context.socket(ZMQ.PUB);
			statsPub.connect(s_monitor + ":5800");
		}

		public void run() {
			totalReceived += received;

			long meanLat;
			if (latenced == 0) {
				meanLat = 0;
			} else {
				meanLat = Math.round(latency / latenced);
			}
			System.out.println("Total latency: " + latency + " Received: "
					+ received + " Latenced: " + latenced + " Mean: " + meanLat
					+ " " + "milliseconds");

			statsPub.send(("31," + minute + "," + totalReceived + ","
					+ received + "," + ID + "," + meanLat).getBytes(), 0);
			minute++;
			received = 0;
			latency = 0;
			latenced = 0;
		}
	}

	private int init() {
		System.out.println("Init sequence");
		initReq.send("1,Hello".getBytes(), 0);
		String response = new String(initReq.recv(0));
		if (!response.equals("")) {
			String[] brokerList = response.split(",");
			this.exchSub = context.socket(ZMQ.SUB);
			this.exchSub.subscribe("".getBytes());
			for (int i = 0; i < brokerList.length; i++) {
				exchSub.connect("tcp://" + brokerList[i] + ":5560");
				knownHosts.add(brokerList[i]);
				System.out.println("connected to " + brokerList[i]);
			}
			return 0;
		} else {
			System.out.println("No exchange available");
			return 1;
		}
	}

	private void computeLatency(long recLat) {
		long nowi = System.currentTimeMillis();
		// long nowi = //use getTimestamp//
		latency = latency + (nowi - recLat);
		latenced++;
		if (nowi - recLat < 0) {
			System.out.println("ERROR: now = " + nowi + " ,recLat = " + recLat);
		}
	}

	@SuppressWarnings("unused")
	private byte[] getTimestamp() {
		tstmpReq.send("".getBytes(), 0);
		return tstmpReq.recv(0);
	}

	public void run() {
		knownHosts = new ArrayList<String>();
		while (init() != 0) {
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Retrying connection...");
		}

		this.items = context.poller();
		this.items.register(monitorSub);
		this.items.register(exchSub);

		Timer timer = new Timer();
		timer.schedule(new Stats(), 0, 60000);

		System.out.println("Worker connected");

		while (true) {
			items.poll(10);
			if (items.pollin(0)) { // Info from Monitor

				String info[] = new String(monitorSub.recv(0)).split(",");
				int infoCode = Integer.parseInt(info[0]);

				if (infoCode == 1 && !info[1].equals("")) { // new Exchange
															// available message
					System.out.println("listening to " + info[1]);
					if (!knownHosts.contains(info[1])) {
						exchSub.connect("tcp://" + info[1] + ":5560");
						knownHosts.add(info[1]);
					}
				}
			}

			if (items.pollin(1)) {
				byte[] request;
				request = exchSub.recv(0);
				int part = 1;
				while (exchSub.hasReceiveMore()) {
					request = exchSub.recv(0);
					part++;
					if (part == 4 && this.tstmp) {
						computeLatency(Long.parseLong(new String(request, 0,
								request.length - 1)));
					}
				}
				received++;
			}
		}
	}

}
