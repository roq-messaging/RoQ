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

import org.apache.log4j.Logger;
import org.junit.Test;
import org.roq.simulation.stat.KPILogicalQSubscriber;
import org.roq.simulation.test.RoQTestCase;
import org.roqmessaging.client.IRoQPublisher;
import org.roqmessaging.core.utils.RoQUtils;

/**
 * Class TestLogicalQStat 
 * <p> Description: Test the statistic sent by the logical queue.
 * 
 * @author sskhiri
 */
public class TestLogicalQStat extends RoQTestCase {

	@Test
	public void test() {
		String qName ="queueTestStat";
		try {
			Logger.getLogger(this.getClass().getName()).info("Starting main test in Test logical queue");
			//1. Create a queue
			this.factory.createQueue(qName, RoQUtils.getInstance().getLocalIP(), false);
			//2. Attach subscriber
			attachSUbscriber(qName);
			//3. Create subscriber
			KPILogicalQSubscriber subscriber = new KPILogicalQSubscriber(
					launcher.configurationServer,
					launcher.configurationServerInterfacePort,
					qName);
			subscriber.subscribe();
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
			
			//6. Add exchange
			factory.createExchange(qName, RoQUtils.getInstance().getLocalIP());
			subscriber.setXchangeToCheck(2);
			Thread.sleep(5000);
			
			
			//7. Shutdown
			factory.removeQueue(qName);
			subscriber.shutDown();
			Thread.sleep(5000);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
