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
package org.roqmessaging.scaling.policy;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.management.LogicalQFactory;
import org.zeromq.ZMQ;

/**
 * Class SimpleScalingPolicy
 * <p> Description: a simple scaling policy that first looks whether we need to create an exchange 
 * or spawning a new machine.
 * 
 * @author sskhiri
 */
public class SimpleScalingPolicy implements IScalingPolicy {
	//Log
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
	//Queue factory
	private LogicalQFactory qFactory = null;
	
	/**
	 * @param gCMAddress the address of the global configuration server.
	 */
	public SimpleScalingPolicy(String gCMAddress) {
		this.qFactory = new LogicalQFactory(gCMAddress);
	}

	/**
	 * 1. Get the list of host from the GCM
	 * 2. for each host ask the host the number of exchanges to the host directly
	 * 3. Check the limit and assign a candidate host
	 * @see org.roqmessaging.scaling.policy.IScalingPolicy#scaleOut(java.util.HashMap)
	 */
	public boolean scaleOut(HashMap<String, Double> context, String qName) {
		//0. Init the candidate host to null
		String candidate = null;
		int bestFreeSlot=0;
		//1. Get the list of host from the GCM
		this.qFactory.getConfigurationState().refreshConfiguration();
		//2. for each host ask the host the number of exchanges
		for (String host : this.qFactory.getConfigurationState().getHostManagerMap().keySet()) {
			ZMQ.Socket hostSocket = 	this.qFactory.getConfigurationState().getHostManagerMap().get(host);
			//Need to be able to send a getExchangeInfo() on each host as defined by #98
			hostSocket.send(Integer.toString(RoQConstant.CONFIG_INFO_EXCHANGE).getBytes(), 0);
			//Answer contains
			//[OK or FAIL], [Number of exchange on host], [max limit of exchange defined in property]
			String resultHost = new String(hostSocket.recv(0));
			if(Integer.parseInt(resultHost)== RoQConstant.OK){
				//[Number of exchange on host]
				resultHost = new String(hostSocket.recv(0));
				int exchange = Integer.parseInt(resultHost);
				resultHost = new String(hostSocket.recv(0));
				int limit = Integer.parseInt(resultHost);
				logger.info("Host "+ host+ " has already "+ exchange +" and the limit is "+ limit);
				int freeSlot = limit-exchange;
				if(freeSlot>bestFreeSlot){
					bestFreeSlot = freeSlot;
					candidate = host;
					logger.debug("Host "+ host+ " is a potential candidate");
				}
			}else{
				logger.error("The host located on "+ host +" cannot send back the exchange information");
			}
		}//enf of loop for
		
		if(candidate==null){
			logger.info("No candidate have been found : need to provision a new VM");
		}else{
			//Provision a new exchange on host
			if(!this.qFactory.createExchange(qName, candidate))
				logger.error("The scaling exchange command failed on host "+ candidate);
		}
		//3. If one has a few number we can create a new exchanges otherwise we need to spawn a new host with a brand new exchange.
		return false;
	}

}
