// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.loaders;

import org.roqmessaging.core.SubscriberConnectionManager;

public class ZLoadSub {

	public static void main(String args[]) {
		int max = Integer.parseInt(args[0]);

		Thread subThreads[] = new Thread[max];
		int IDs = 0;
		for (int i = 0; i < max; i++) {
			System.out.println("Starting listener "+ (i+1) +"/" +max);
			SubscriberConnectionManager tempSub = new SubscriberConnectionManager("tcp://localhost:5571", "tcp://localhost:5800", "manche",  IDs, false);
			Thread t = new Thread(tempSub);
			subThreads[i] = t;
			subThreads[i].start();
			IDs++;
		}
	}
}
