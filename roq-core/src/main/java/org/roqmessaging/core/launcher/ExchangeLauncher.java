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
package org.roqmessaging.core.launcher;

import org.roqmessaging.core.Exchange;
import org.zeromq.ZMQ;

/**
 * Class ExchangeLauncher
 * <p>
 * Description: Launch an exchange instance with the specified configuration.
 * 
 * @author sskhiri
 */
public class ExchangeLauncher {

	/**
	 * Must contain 4 attributes: 1. The front port <br>
	 * 2. The back port <br>
	 * 3. the address of the monitor to bind tcp:// monitor:monitorPort<br>
	 * 4. The address of the stat monitor to bind tcp://monitor:statport<br>
	 * 5. The path in which the heartbeats wil be stored <br>
	 * 6. The number of seconds between each heartbeat <br>
	 * 
	 * example: 5559 5560 tcp://localhost:5571, tcp://localhost:5800
	 * 
	 * <p>
	 * Notice that this process must be stopped by the shutdown monitor process
	 * by using <code>
	 *     ZMQ.Socket shutDownExChange = ZMQ.context(1).socket(ZMQ.REQ);
			shutDownExChange.setSendTimeOut(0);
			shutDownExChange.connect("tcp://"+address+":"+(backport+1));
			shutDownExChange.send(Integer.toString(RoQConstant.SHUTDOWN_REQUEST).getBytes(), 0);
			shutDownExChange.close();
	 * </code>
	 * 
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Launching Exchange process with arg "+displayArg(args));
		if (args.length != 6) {
			System.out
					.println("The argument should be <int front port> <int back port> < tcp:// monitor:monitorPort> " +
							" <tcp:// monitor:statport> <localState path> <heartbeat Period>");
			return;
		}
		System.out.println("Starting Exchange with monitor " + args[2] + ", stat= " + args[3]);
		System.out.println("Front  port  " + args[0] + ", Back port= " + args[1]);
		
		try {
			int frontPort = Integer.parseInt(args[0]);
			int backPort = Integer.parseInt(args[1]);
			// Add a communication socket that will notify the monitor that this
			// instance stops
			final ZMQ.Context shutDownContext;
			final ZMQ.Socket shutDownSocket;
			shutDownContext = ZMQ.context(1);
			shutDownSocket = shutDownContext.socket(ZMQ.PUB);
			shutDownSocket.connect(args[2]);
			shutDownSocket.setLinger(3500);
			long hbPeriod = Long.parseLong(args[5]);
			// Instanciate the exchange
			final Exchange exchange = new Exchange(frontPort, backPort, args[2], args[3], args[4], hbPeriod);
			// Launch the thread
			Thread t = new Thread(exchange);
			t.start();
		} catch (NumberFormatException e) {
			System.out.println(" The arguments are not valid, must: <int: front port> <int: back port>");
		}
	}
	

	/**
	 * @param args the argument we recieved at the start up
	 * @return the concatenated string of argument
	 */
	private static String displayArg(String[] args) {
		String result="";
		for (int i = 0; i < args.length; i++) {
			result+=args[i] +", ";
		}
		return result;
	}


}
