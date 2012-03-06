// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.loaders;

import org.roqmessaging.core.PubClientLib;

public class ZLoadPub {

	/* Arguments:
	 * spawn rate (seconds), max prods,
	 * monitor, msg/min, duration
	 * (min), payload, delay 
	*/
	public static void main(String[] args) { 
		int rate = Integer.parseInt(args[0]);
		int max = Integer.parseInt(args[1]);
		Thread pubThreads[] = new Thread[max];
		System.out.println("Starting producers in " + args[6] + " minutes");
		try {
			Thread.sleep(Integer.parseInt(args[6]) * 1000 * 60);
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		for (int i = 0; i < max; i++) {
			System.out.println("Starting producer "+ (i+1) +"/" +max);
			PubClientLib tempPub = new PubClientLib(args[2],
					Integer.parseInt(args[3]), Integer.parseInt(args[4]),
					Integer.parseInt(args[5]), Boolean.parseBoolean(args[7]));
			Thread t = new Thread(tempPub);
			pubThreads[i] = t;
			pubThreads[i].start();
			try {
				Thread.sleep(rate * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
