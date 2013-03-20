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

import static org.junit.Assert.assertEquals;

import org.apache.log4j.Logger;
import org.roq.simulation.stat.KPILogicalQSubscriber;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class TestHostExchangeInfo
 * <p> Description: Test whether the exchange is answering correclty. The test creates 
 * exchanges on a queue and check whether the number of exchanges is correct.
 * 
 * @author sskhiri
 */
public class TestHostExchangeInfo extends RoQTestCase {
	//logger
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

		public void testExchangeInfo() {
			String qName ="queueInfoXchange";
			try {
				Logger.getLogger(this.getClass().getName()).info("Starting main TestHostExchangeInfo  ");
				//1. Create a queue
				this.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP());
				//2. Attach subscriber
				attachSUbscriber(qName);
				//3. Create subscriber
				KPILogicalQSubscriber subscriber = new KPILogicalQSubscriber(RoQUtils.getInstance().getLocalIP(), qName);
				new Thread(subscriber).start();
				subscriber.setProducerToCheck(0);
				subscriber.setXchangeToCheck(1);
				Thread.sleep(6000);
				
				//4 Attach publisher
				IRoQPublisher publisher = attachPublisher(qName);
				subscriber.setProducerToCheck(1);
				Thread.sleep(6000);
				
				//5. send messages
				sendMsg(publisher);
				
				//6. Check the number of exchange
				assertEquals(1, askHostExchangeInfo());
				
				//6. Add exchange
				factory.createExchange(qName, RoQUtils.getInstance().getLocalIP());
				subscriber.setXchangeToCheck(2);
				Thread.sleep(5000);
				assertEquals(2, askHostExchangeInfo());
				
				factory.createExchange(qName, RoQUtils.getInstance().getLocalIP());
				subscriber.setXchangeToCheck(3);
				Thread.sleep(5000);
				assertEquals(3, askHostExchangeInfo());
				
				
				//7. Shutdown
				factory.removeQueue(qName);
				subscriber.shutDown();
				Thread.sleep(5000);
				
			} catch (Exception e) {
				e.printStackTrace();
		}
	}

		/**
		 * @return the number of exchanges given by the host
		 */
	private int askHostExchangeInfo() {
		ZMQ.Socket hostSocket = ZMQ.context(1).socket(ZMQ.REQ);
		hostSocket.connect("tcp://" + RoQUtils.getInstance().getLocalIP() + ":5100");
		// Need to be able to send a getExchangeInfo() on each host as defined
		// by #98
		hostSocket.send(Integer.toString(RoQConstant.CONFIG_INFO_EXCHANGE).getBytes(), 0);
		// Answer contains
		// [OK or FAIL], [Number of exchange on host], [max limit of exchange
		// defined in property]
		String resultHost = new String(hostSocket.recv(0));
		logger.debug("Revieved "+ resultHost);
		if (Integer.parseInt(resultHost) == RoQConstant.OK) {
			// [Number of exchange on host]
			resultHost = new String(hostSocket.recv(0));
			int exchange = Integer.parseInt(resultHost);
			resultHost = new String(hostSocket.recv(0));
			int limit = Integer.parseInt(resultHost);
			logger.debug("Host "+ RoQUtils.getInstance().getLocalIP()+ " has already "+exchange +" and the limit is "+ limit);
			int freeSlot = limit-exchange;
			logger.debug("There are "+ freeSlot+ " free slots");
			return exchange;
		}
		return 0;
	}

}
