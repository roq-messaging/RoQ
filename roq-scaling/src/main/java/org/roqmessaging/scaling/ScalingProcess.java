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
package org.roqmessaging.scaling;

import java.net.Socket;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.RoQConstantInternal;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.stat.KPISubscriber;
import org.zeromq.ZMQ;

/**
 * Class ScalingProcess
 * <p> Description: Subscriber to the Statistic channel and process specific stat in order 
 * to evaluate auto scaling rules.
 * TODO:
 * 1. Spawning the Scaling process at the host monitor level in order to let the module indep
 * 2. Push/Pull reuquest or Req/Resp to get the last auto scaling rule associated to this queue - later a real pub/sub system must be put in place
 * 3. Evaluation of the rule
 * 
 * @author sskhiri
 */
public class ScalingProcess extends KPISubscriber {
	// ZMQ
	private ZMQ.Context context = null;
	//Management controller socket - enables to get the auto scaling  configuration
	private ZMQ.Socket requestSocket = null;
	//The queue name on which the scaling process is bound
	private String qName = null;
	//The auto scaling config got from the mngt server
	private AutoScalingConfig scalingConfig = null;
	//Request serialiazer
	private IRoQSerializer serializer =null;
	//Logger
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

	/**
	 * @param globalConfiguration the GCM IP address
	 * @param qName the name of the queue we want to connect.
	 */
	public ScalingProcess(String globalConfiguration, String qName) {
		super(globalConfiguration, qName);
		this.qName = qName;
		this.serializer = new RoQBSONSerializer();
		// ZMQ init
		this.context = ZMQ.context(1);
		//Prepare the request socket to the management controller
		this.requestSocket = this.context.socket(ZMQ.REQ);
		this.requestSocket.connect("tcp://" + globalConfiguration + ":5003");
		//Getting the initial auto scaling configuration
		this.scalingConfig = getAutoScalingConfig();
		if(this.scalingConfig==null){
			this.logger.info("Autoscaling process for queue "+ qName + " is innactive - no auto scaling rules");
		}
	}

	/**
	 * @return the autoscaling config by querying the mngt server in BSON
	 */
	private AutoScalingConfig getAutoScalingConfig() {
		try {
			// 1. Launch a get autoscaling Queue request
			BSONObject bsonObject = new BasicBSONObject();
			bsonObject.put("CMD", RoQConstant.BSON_CONFIG_GET_AUTOSCALING_RULE);
			bsonObject.put("QName", qName);
			logger.info("Request get Auto scaling info from auto scaling process");
			logger.info(bsonObject.toString());
			byte[] encoded = BSON.encode(bsonObject);

			// 2. Send the request
			requestSocket.send(encoded, 0);

			// 3. Check the result
			byte[] bres = requestSocket.recv(0);
			
			return  serializer.unserializeConfig(bres);
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
		}
		return null;
	}

	/**
	 * @see org.roqmessaging.management.stat.KPISubscriber#processStat(java.lang.Integer, org.bson.BSONObject)
	 */
	@Override
	public void processStat(Integer CMD, BSONObject statObj, org.zeromq.ZMQ.Socket statSocket) {
		if(this.scalingConfig!=null){
			this.logger.info(" Processing in AUTO SCALING process Stat "+ CMD);
			switch (CMD.intValue()) {
			case 20:
			    //Receiving statistic from an exchange
				/**
				 * Stats recieved from exchange
				 *  { "CMD" : 20 , "X_ID" : "XChange 1340470515568"}
				 *  { "CMD" : 21 , "Minute" : "1" , "TotalProcessed" : "500" , "Processed" : "500" , "TotalThroughput" : "0" , "Throughput" : "3890" , "Producers" : "1"}
				 *  { "CMD" : 22 , "CPU" : "1.12" , "MEMORY" : "4.6083984375"}
				 */
				//Building Exchange context
				HashMap<String, Double> xchangeCtx = new HashMap<String, Double>();
				int xchangeID = (Integer) statObj.get("X_ID");
				while(statSocket.hasReceiveMore()){
					statObj = BSON.decode(statSocket.recv(0));
					logger.debug("In the Exchange stat message  " + statObj.toString());
					Integer cmd = (Integer) statObj.get("CMD");
					if(cmd.intValue() ==21){
						xchangeCtx.put(RoQConstantInternal.CONTEXT_KPI_XCHANGE_EVENTS, (Double) statObj.get("Throughput"));
					}
					if(cmd.intValue() ==22){
						xchangeCtx.put(RoQConstantInternal.CONTEXT_KPI_HOST_CPU, (Double) statObj.get("CPU"));
						xchangeCtx.put(RoQConstantInternal.CONTEXT_KPI_HOST_RAM, (Double) statObj.get("MEMORY"));
					}
					//Rule evaluation
					boolean overloaded = false;
					if(this.scalingConfig.getXgRule().isOverLoaded(xchangeCtx)){
						overloaded= true;
						logger.info("Xchange auto scaling rule triggered "+ this.scalingConfig.getXgRule().toString() + "for exchange "+ xchangeID);
					}
					if(this.scalingConfig.getHostRule().isOverLoaded(xchangeCtx)){
						overloaded= true;
						logger.info("Host auto scaling rule triggered "+ this.scalingConfig.getHostRule().toString()+ "for exchange "+ xchangeID + " Host");
					}
				}
				break;
				
			case 23:
				//This is a stat at the logical queue level only 1 message in the enveloppe
				/**
				 * { "CMD" : 23 , "QName" : "queueTestStat:5500" , "XChanges" : "1" , "Producers" : "0" , "Throughput" : "0"}
				 * */
				//TODO

			default:
				break;
			}
		}
	}
	
	/**
	 * @see org.roqmessaging.management.stat.KPISubscriber#shutDown()
	 */
	@Override
	public void shutDown() {
		this.requestSocket.setLinger(0);
		this.requestSocket.close();
		super.shutDown();
		logger.debug("Closing request socket");
	}

}
