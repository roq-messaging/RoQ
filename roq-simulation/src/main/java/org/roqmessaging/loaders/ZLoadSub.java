// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.loaders;

import java.util.ArrayList;

import org.roqmessaging.core.SubscriberConnectionManager;

public class ZLoadSub {

	public static void main(String args[]) {
		int max = Integer.parseInt(args[0]);

		Thread subThreads[] = new Thread[max];
		for (int i = 0; i < max; i++) {
			System.out.println("Starting listener "+ (i+1) +"/" +max);
			ArrayList<String> monitors = new ArrayList<String>();
			monitors.add("tcp://localhost:5571");
			ArrayList<String> statMonitors = new ArrayList<String>();
			statMonitors.add("tcp://localhost:5800");
			SubscriberConnectionManager tempSub = new SubscriberConnectionManager(1, monitors, statMonitors, "manche",  false);
			Thread t = new Thread(tempSub);
			subThreads[i] = t;
			subThreads[i].start();
		}
	}
}
