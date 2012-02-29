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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.zeromq.ZMQ;

public class Monitor implements Runnable {

	private ArrayList<Exchange> knownHosts;
	private ArrayList<Integer> hostsToRemove;
	private long maxThroughput;
	private ZMQ.Context context;

	private BufferedWriter bufferedOutput;

	private ZMQ.Socket producersPub, brokerSub, initRep, listenersPub, statSub;

	public Monitor() {
		knownHosts = new ArrayList<Exchange>();
		hostsToRemove = new ArrayList<Integer>();
		maxThroughput = 75000000L; // Maximum throughput per exchange, in bytes/minute

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

		try {
			FileWriter output = new FileWriter(("output" + getFileStamp()),
					true);
			bufferedOutput = new BufferedWriter(output);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
					// System.out.println("Host "+address+" reported alive");
					return 0;
				}
			}
		}
		System.out.println("Added new host: " + address);
		knownHosts.add(new Exchange(address));
		return 1;
	}

	class ReportExchanges extends TimerTask {
		public void run() {
			if (!knownHosts.isEmpty()) {
				for (int i = 0; i < knownHosts.size(); i++) {
					if (!knownHosts.get(i).isAlive()) {
						knownHosts.get(i).addLost();
						if (knownHosts.get(i).getLost() > 0) {
							System.out.println(knownHosts.get(i).getAddress()
									+ " is lost");
							hostsToRemove.add(i);
						}
					} else {
						knownHosts.get(i).setAlive(false);
					}
				}
			}
		}
	}

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

	private class Exchange {

		private String address;
		private long throughput;
		private boolean alive;
		private int nbProd;

		public int getNbProd() {
			return nbProd;
		}

		public void setNbProd(int nbProd) {
			this.nbProd = nbProd;
		}

		public void addNbProd() {
			this.nbProd++;
		}

		public boolean isAlive() {
			return alive;
		}

		public void setAlive(boolean alive) {
			this.alive = alive;
		}

		public int getLost() {
			return lost;
		}

		@SuppressWarnings("unused")
		public void resetLost() {
			this.lost = 0;
		}

		public void addLost() {
			this.lost++;

		}

		private int lost;

		public Exchange(String addr) {
			this.address = addr;
			this.throughput = 0;
			this.alive = true;
			this.lost = 0;
			this.nbProd = 0;
		}

		public String getAddress() {
			return address;
		}

		@SuppressWarnings("unused")
		public void setAddress(String addr) {
			this.address = addr;
		}

		public long getThroughput() {
			return throughput;
		}

		public void setThroughput(long tr) {
			this.throughput = tr;
		}

		public void addThroughput(long tr) {
			this.throughput += tr;
		}

	}

	private void updateExchgMetadata(String address, String throughput,
			String nbprod) {
		for (int i = 0; i < knownHosts.size(); i++) {
			if (address.equals(knownHosts.get(i).getAddress())) {
				knownHosts.get(i).setThroughput(Long.parseLong(throughput));
				knownHosts.get(i).setNbProd(Integer.parseInt(nbprod));
				System.out.println("Host " + knownHosts.get(i).getAddress()
						+ " : " + knownHosts.get(i).getThroughput()
						+ " bytes/min, " + knownHosts.get(i).getNbProd()
						+ " users");
			}
		}
	}

	private String relocateProd(String exchg_addr, String bytesSent) {
		if (knownHosts.size() > 0 && hostLookup(exchg_addr) != -1) {
			int exch_index = hostLookup(exchg_addr);
			if (knownHosts.get(exch_index).getThroughput() > (java.lang.Math
					.round(maxThroughput * 1.10))) { // TODO externalize 1.10,
														// 0.90
														// and 0.20 tolerance
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
						String candidate = knownHosts.get(candidate_index)
								.getAddress();
						if (knownHosts.get(candidate_index).getThroughput()
								+ Long.parseLong(bytesSent) < (java.lang.Math
									.round(maxThroughput * 0.90))) {
							knownHosts.get(candidate_index).addThroughput(
									Long.parseLong(bytesSent));
							knownHosts.get(candidate_index).addNbProd();
							return candidate;
						}

						else if (knownHosts.get(candidate_index)
								.getThroughput() + Long.parseLong(bytesSent) < knownHosts
								.get(hostLookup(exchg_addr)).getThroughput()
								&& (knownHosts.get(hostLookup(exchg_addr))
										.getThroughput() - knownHosts.get(
										candidate_index).getThroughput()) > (java.lang.Math
										.round(knownHosts.get(
												hostLookup(exchg_addr))
												.getThroughput() * 0.20))) {
							knownHosts.get(candidate_index).addThroughput(
									Long.parseLong(bytesSent));
							knownHosts.get(candidate_index).addNbProd();
							System.out
									.println("Relocating for load optimization");
							return candidate;
						}
					}
				}
			}
		}
		return "";
	}

	public void run() {

		Timer timer = new Timer();
		timer.schedule(new ReportExchanges(), 0, 10000);

		int infoCode;

		ZMQ.Poller items = context.poller(3);
		items.register(statSub);
		items.register(brokerSub);
		items.register(initRep);

		System.out.println("Monitor started");
		long lastPublish = System.currentTimeMillis();
		while (true) {
			if (System.currentTimeMillis() - lastPublish > 10000) { // not really clean, workaround to the fact that sockets cannot be shared between threads
				listenersPub.send(("2," + bcastExchg()).getBytes(), 0);
				lastPublish = System.currentTimeMillis();
				System.out.println("Alive hosts: " + bcastExchg()
						+ " , Free host: " + getFreeHost());
			}
			while (!hostsToRemove.isEmpty()) {
				producersPub.send(("2," + knownHosts.get(hostsToRemove.get(0))
						.getAddress()).getBytes(), 0);
				knownHosts.remove((int) hostsToRemove.get(0));
				hostsToRemove.remove(0);
				System.out.println("Panic procedure initiated");
			}

			items.poll(2000);
			
			if (items.pollin(0)) { // Stats info: 1* producers, 2* exch, 3* listeners 
				String info[] = new String(statSub.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);
				
				switch(infoCode){
				case 11:
					System.out.println("1 producer finished, sent " + info[2]
							+ " messages.");
					try {
						bufferedOutput.write("PROD," + getFileStamp()
								+ ",FINISH," + info[1] + "," + info[2]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 12:
					try {
						bufferedOutput.write("PROD," + getFileStamp()
								+ ",STAT," + info[1] + "," + info[2] + ","
								+ info[3]);
						bufferedOutput.newLine();
						bufferedOutput.flush();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 21:
					try {
						bufferedOutput.write("EXCH," + getFileStamp() + ","
								+ info[1] + "," + info[2] + "," + info[3] + ","
								+ info[4] + "," + info[5] + "," + info[6]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 31:
					try {
						bufferedOutput.write("LIST," + getFileStamp() + ","
								+ info[1] + "," + info[2] + "," + info[3] + ","
								+ info[4] + "," + info[5]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
			
			if (items.pollin(1)) { // Info from Exchange
				String info[] = new String(brokerSub.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);
				
				switch (infoCode){
				case 3:
					// Broker debug code
					System.out.println(info[1]);
					break;
				case 4:
					// Broker most productive producer code
					updateExchgMetadata(info[1], info[4], info[6]);
					if (!info[1].equals("x")) {
						String relocation = relocateProd(info[1], info[3]); // ip,bytessent
						if (!relocation.equals("")) {
							//System.out.println("relocating " + info[2] + " on " + relocation);
							producersPub.send(("1," + info[2] + "," + relocation)
									.getBytes(), 0);
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
					System.out.println("Broker " + info[1]
							+ " has left the building");

					if (hostLookup(info[1]) != -1) {
						knownHosts.remove(hostLookup(info[1]));
					}

					producersPub.send(("2," + info[1]).getBytes(), 0);
					break;
				}
			}

			if (items.pollin(2)) { // Init socket

				String info[] = new String(initRep.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);
				
				switch (infoCode) {
				case 1:
					// System.out.println("Received init request from listener");
					initRep.send(bcastExchg().getBytes(), 0);
					break;
				case 2:
					// System.out.println("Received init request from producer. Assigned on " + getFreeHost());
					initRep.send(getFreeHost().getBytes(), 0);
					break;
				
				case 3:
					// System.out.println("Received panic init from producer");
					initRep.send(getFreeHost().getBytes(), 0);// TODO: round robin return knownHosts. should be aware of the profile of the producer
					break;
				}	
			}
		}
	}

	private final static String getFileStamp() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("GMT+2:00"));
		return df.format(new Date());
	}

	public static void main(String[] args) {
		System.out.println(getFileStamp());
		Monitor monitor = new Monitor();
		Thread t = new Thread(monitor);
		t.start();
	}

}
