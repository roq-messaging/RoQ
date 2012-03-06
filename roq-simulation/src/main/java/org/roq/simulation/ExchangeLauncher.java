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
package org.roq.simulation;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.roqmessaging.core.Exchange;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class ExchangeSimulator
 * <p> Description: Used for simulation purpose on cluster.
 * 
 * @author sskhiri
 */
public class ExchangeLauncher {

	/**
	 * @param args
	 */
		public static void main(String[] args) throws InterruptedException {
			final String monitorHost = args[0];
			final ZMQ.Context shutDownContext;
			final ZMQ.Socket shutDownSocket;
			shutDownContext = ZMQ.context(1);
			shutDownSocket = shutDownContext.socket(ZMQ.PUB);
			shutDownSocket.connect("tcp://" + monitorHost + ":5571");
			shutDownSocket.setLinger(3500);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				Logger logger = Logger.getLogger(ExchangeLauncher.class);
				
				@Override
				public void run() // TODO ensure it waits few seconds in normal
									// functioning before closing everything
									// This section may need to be rewritten more
									// elegantly
				{
					try {
						Runtime.getRuntime().exec("date");
					} catch (IOException e) {
						logger.error("Error when executing date)", e);
					}

					logger.info("Shutting down");
					shutDownSocket.send(("6," + RoQUtils.getInstance().getLocalIP()).getBytes(), 0);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						logger.error("Error when thread sleeping (shutting down phase))", e);
					}
				}
			});

			Exchange exchange = new Exchange("5559", "5560", args[0]);
			Thread t = new Thread(exchange);
			t.start();
		}

}
