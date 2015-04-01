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
package org.roq.simulation.stat;

import org.bson.BSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.timer.MonitorStatTimer;
import org.roqmessaging.management.stat.KPISubscriber;

/**
 * Class KPIQSubscriber
 * <p> Description: Test class for validating the Statistic behavior of the {@linkplain MonitorStatTimer}
 * 
 * @author sskhiri
 */
public class KPILogicalQSubscriber extends KPISubscriber {
	private int xchangeToCheck = 0;
	private int producerToCheck =0;

	/**
	 * @param gcm_address
	 *            the IP address of the global configuration manager
	 * @param gcm_interfacePort
	 *            the port used by the global configuration manager
	 *            to provide an interface to the topology
	 * @param qName the logical queue name
	 */
	public KPILogicalQSubscriber(String zkConnectionString, int gcm_interfacePort, String qName) {
		super(zkConnectionString, gcm_interfacePort, qName);
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "KPI Logical Q subscriber";
	}

	/**
	 * @see org.roqmessaging.management.stat.KPISubscriber#processStat(java.lang.Integer)
	 */
	@Override
	public void processStat(Integer CMD, BSONObject statObj, org.zeromq.ZMQ.Socket rcv) {
		switch (CMD.intValue()) {
		case RoQConstant.STAT_Q:
			super.logger.debug("Got stat for Logical Q");
			super.logger.debug(statObj.toString());
			//Check content
			checkField(statObj, "QName");
			checkField(statObj, "XChanges");
			checkField(statObj, "Producers");
			checkField(statObj, "Throughput");
			break;

		default:
			break;
		}
	}

	/**
	 * @return the xchangeToCheck
	 */
	public int getXchangeToCheck() {
		return xchangeToCheck;
	}

	/**
	 * @param xchangeToCheck the xchangeToCheck to set
	 */
	public void setXchangeToCheck(int xchangeToCheck) {
		this.xchangeToCheck = xchangeToCheck;
	}

	/**
	 * @return the producerToCheck
	 */
	public int getProducerToCheck() {
		return producerToCheck;
	}

	/**
	 * @param producerToCheck the producerToCheck to set
	 */
	public void setProducerToCheck(int producerToCheck) {
		this.producerToCheck = producerToCheck;
	}

}
