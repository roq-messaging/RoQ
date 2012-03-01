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
package org.roqmessaging.tests;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roqmessaging.core.Exchange;
import org.roqmessaging.core.PubClientLib;
import org.roqmessaging.core.SubClientLib;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class BasicSetupTests
 * <p>
 * Description: This Test case test the basic setup of the Exchange with few
 * listeners and providers.
 * 
 * @author Sabri Skhiri
 */
public class BasicSetupTest {
	private Exchange xChange = null;
	private Thread threadPub = null;
	private Thread threadSub = null;

	/**
	 * Create the Exchange.
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		startExchange();
		startPublisherClient();
		startSubscriberClient();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.xChange.cleanShutDown();
		// TODO implementing a clean shutdown methd
		this.threadPub.stop();
		this.threadSub.stop();
	}

	@Test
	public void test() {
		assertNotNull(this.xChange);
	}

	/**
	 * Start an Exchange with Hardcoded value. Monitor host = "localhost" A
	 * potential evolution would be a configuration file from which the
	 * parameter are loaded.
	 */
	private void startExchange() {
		final String monitorHost = "localhost";
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
					e.printStackTrace();
				}

				System.out.println("Shutting Down!");
				shutDownSocket.send(("6," + RoQUtils.getInstance().getLocalIP()).getBytes(), 0);
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		this.xChange = new Exchange("5559", "5560", monitorHost);
		Thread t = new Thread(this.xChange);
		t.start();
	}

	/**
	 * Initiate a thread publisher
	 */
	private void startPublisherClient() {
		// Init parameters
		String monitor = "localhost";
		int rate = 5;
		int minutes = 1;
		int payload = 25;
		boolean tstmp = true;

		// Launching the pub client
		PubClientLib pubClient = new PubClientLib(monitor, rate, minutes, payload, tstmp); // monitor,
																							// msg/min,
																							// duration,
																							// payload
		this.threadPub = new Thread(pubClient);
		this.threadPub.start();
	}

	/**
	 * Initiate a thread subscriber
	 */
	private void startSubscriberClient() {
		String monitor = "localhost";
		String subKey = "manche";
		int ID = 0;
		boolean tstmp = true;
		SubClientLib SubClient = new SubClientLib(monitor, subKey, ID, tstmp);
		this.threadSub= new Thread(SubClient);
		this.threadSub.start();
	}

}
