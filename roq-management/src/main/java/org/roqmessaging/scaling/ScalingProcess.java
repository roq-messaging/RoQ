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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.RoQConstantInternal;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.stat.KPISubscriber;
import org.roqmessaging.scaling.policy.NullScalingPolicy;
import org.roqmessaging.scaling.policy.IScalingPolicy;
import org.zeromq.ZMQ;

/**
 * Class ScalingProcess
 * <p> Description: Subscriber to the Statistic channel and process specific stat in order 
 * to evaluate auto scaling rules.
 * 1. Spawning the Scaling process at the host monitor level in order to let the module indep
 * 2. Push/Pull request or Req/Resp to get the last auto scaling rule associated to this queue - later a real pub/sub system must be put in place
 * 3. Evaluation of the rule
 * 
 * @author sskhiri
 */
public class ScalingProcess extends KPISubscriber {
	// ZMQ
	private ZMQ.Context context = null;
	//Management controller socket - enables to get the auto scaling  configuration
	private ZMQ.Socket requestSocket = null;
	//Pull socket that will listen for configuration update
	private ZMQ.Socket pullListnerConfigSocket = null;
	//The queue name on which the scaling process is bound
	private String qName = null;
	//The auto scaling config got from the mngt server
	private AutoScalingConfig scalingConfig = null;
	//Request serialiazer
	private IRoQSerializer serializer =null;
	//Logger
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
	//The scaling policy
	private IScalingPolicy scalingPolicy = null;
	//The shut down monitor
	private ShutDownMonitor shutDownMonitor = null;
	//The cloud properties
	//TODO 
	/**
	 * Notice the scaling process starts a shutdown monitor on the listener port +1. We advice to start it on port 5802. 
	 * @param globalConfiguration the GCM IP address
	 * @param qName the name of the queue we want to connect.
	 * @param listnerPort is the port on which the scaling process will listen for push request when a new configuration will
	 * be published
	 */
	public ScalingProcess(String globalConfiguration, String qName, int listnerPort) {
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
		//Init the scaling policy
		this.scalingPolicy= new NullScalingPolicy();
		
		//Prepare the push listener registration
		this.pullListnerConfigSocket=  this.context.socket(ZMQ.PULL);
		String localListenerAddress = "tcp://"+RoQUtils.getInstance().getLocalIP()+":"+listnerPort;
		this.pullListnerConfigSocket.bind(localListenerAddress);
		//Start the registration operation
		this.registerListener( localListenerAddress);
		
		//initiatlisation of the shutdown thread
		this.shutDownMonitor = new ShutDownMonitor(listnerPort+1, this);
		new Thread(shutDownMonitor).start();
		logger.debug("Started  scaling shutdown monitor on "+ (listnerPort+1));
	}

	/**
	 * Ask the management controller to register the listener for auto scaling update.
	 * @param localListenerAddress the address on which the auto scaling update arrive
	 */
	private void registerListener( String localListenerAddress) {
		try {
			// 1. Launch a register listener request
			BSONObject bsonObject = new BasicBSONObject();
			bsonObject.put("CMD", RoQConstant.BSON_CONFIG_REGISTER_FOR_AUTOSCALING_RULE_UPDATE);
			bsonObject.put("QName", qName);
			bsonObject.put("Address", localListenerAddress);
			logger.info("Request Register for Auto scaling UPDATES");
			logger.info(bsonObject.toString());
			byte[] encoded = BSON.encode(bsonObject);

			// 2. Send the request
			requestSocket.send(encoded, 0);

			// 3. Check the result
			byte[] bres = requestSocket.recv(0);
			
			BSONObject result = BSON.decode(bres);
			if ((Integer)result.get("RESULT")==RoQConstant.FAIL){
				throw new IllegalStateException("The request failed: "+ result.get("COMMENT"));
			}
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
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
 * Override the run in order to add a socket (for the update config listener) to the poller.
 * 
 * @see org.roqmessaging.management.stat.KPISubscriber#run()
 */
@Override
	public void run() {
		ZMQ.Poller poller = context.poller(1);
		poller.register(kpiSocket);
		poller.register(pullListnerConfigSocket);
		while (active) {
			poller.poll(2000);
			if (active & poller.pollin(0)) {
				do {
					// Stat coming from the KPI stream
					BSONObject statObj = BSON.decode(kpiSocket.recv(0));
					logger.debug("Start analysing info code " + statObj.get("CMD"));
					processStat((Integer) statObj.get("CMD"), statObj, kpiSocket);
				} while (kpiSocket.hasReceiveMore());
			}
			if (active & poller.pollin(1)) {
				// New auto scaling config update
				logger.debug("Receiving autoscaling update ");
				this.scalingConfig = serializer.unserializeConfig(pullListnerConfigSocket.recv(0));
			}
		}
		//Closing sockets
		this.kpiSocket.setLinger(0);
		this.pullListnerConfigSocket.setLinger(0);
		poller.unregister(kpiSocket);
		poller.unregister(pullListnerConfigSocket);
		this.pullListnerConfigSocket.close();
		this.kpiSocket.close();

	}

	/**
	 * @see org.roqmessaging.management.stat.KPISubscriber#processStat(java.lang.Integer, org.bson.BSONObject)
	 */
	@Override
	public void processStat(Integer CMD, BSONObject statObj, org.zeromq.ZMQ.Socket statSocket) {
		if(this.scalingConfig!=null){
			this.logger.info(" Processing in AUTO SCALING process Stat "+ CMD + " : ");
			this.logger.debug(statObj.toString());
			boolean overloaded = false;
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
				HashMap<String, Double> context = new HashMap<String, Double>();
				String xchangeID = (String) statObj.get("X_ID");
				while(statSocket.hasReceiveMore()){
					statObj = BSON.decode(statSocket.recv(0));
					logger.debug("In the Exchange stat message  " + statObj.toString());
					Integer cmd = (Integer) statObj.get("CMD");
					if(cmd.intValue() ==21){
						if(checkField(statObj, "Throughput"))
							context.put(RoQConstantInternal.CONTEXT_KPI_XCHANGE_EVENTS, Double.parseDouble((String) statObj.get("Throughput")));
						if(evaluateXchangeRule(context)){
							overloaded = true;
							logger.info("Xchange auto scaling rule triggered "+ this.scalingConfig.getXgRule().toString() + "for exchange "+ xchangeID);
						}else{
							logger.info("Xchange auto scaling rule NOT triggered "+ this.scalingConfig.getXgRule().toString() + "for exchange "+ xchangeID);
						}
					}
					if(cmd.intValue() ==22){
						if(checkField(statObj, "MEMORY") && checkField(statObj, "CPU")){
							context.put(RoQConstantInternal.CONTEXT_KPI_HOST_CPU, Double.parseDouble((String) statObj.get("CPU")));
							context.put(RoQConstantInternal.CONTEXT_KPI_HOST_RAM,Double.parseDouble((String) statObj.get("MEMORY")));
							if(evaluateHostRule(context)){
								overloaded=true;
								logger.info("Host auto scaling rule triggered "+ this.scalingConfig.getHostRule().toString()+ "for exchange "+ xchangeID + " Host");
							}else{
								logger.info("Host auto scaling rule NOT  triggered "+ this.scalingConfig.getHostRule().toString()+ "for exchange "+ xchangeID + " Host");
							}
						}
					}
				}
				if(overloaded){
					this.scalingPolicy.scaleOut(context, this.qName);
				}
				break;
				
			case 23:
				//This is a stat at the logical queue level only 1 message in the enveloppe
				/**
				 * { "CMD" : 23 , "QName" : "queueTestStat:5500" , "XChanges" : "1" , "Producers" : "0" , "Throughput" : "0"}
				 * */
				context = new HashMap<String, Double>();
				String qName =(String) statObj.get("QName");
				logger.debug("Evaluating auto scaling rule for logical queue  " + qName);
				if(checkField(statObj, "XChanges")
						&& checkField(statObj, "Producers")
						&& checkField(statObj, "Throughput")){
					//Context creation
					context.put(RoQConstantInternal.CONTEXT_KPI_Q_XCHANGE_NUMBER, Double.valueOf((String) statObj.get("XChanges")));
					context.put(RoQConstantInternal.CONTEXT_KPI_Q_PRODUCER_NUMBER, Double.valueOf((String) statObj.get("Producers")));
					context.put(RoQConstantInternal.CONTEXT_KPI_Q_THROUGPUT, Double.valueOf((String) statObj.get("Throughput")));
					//Autoscaling rule checking
					if(this.scalingConfig.getqRule()!=null){
						if(this.scalingConfig.getqRule().isOverLoaded(context)){
							overloaded= true;
							logger.info("Logical Queue  auto scaling rule triggered "+ this.scalingConfig.getqRule().toString() + "for Q  "+ qName);
						}else{
							logger.info("Logical Queue  auto scaling rule  NOT triggered "+ this.scalingConfig.getqRule().toString() + "for Q  "+ qName);
						}
					}
				}else{
					logger.warn("The Queue level stat does not contain the mandatory attributes to create the context");
				}
				if(overloaded){
					//TODO autoscaling action
				}
			break;

			default:
				break;
			}
		}
	}
	
	/**
	 * @param context the topology context fed by stat
	 * @return true when the rule is activated
	 */
	private boolean evaluateXchangeRule(HashMap<String, Double> context) {
		//Rule evaluation
		if (this.scalingConfig.getXgRule() != null) {
			if (this.scalingConfig.getXgRule().isOverLoaded(context)) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * @param context the current context of the topology
	 * @return true if the rule is activated
	 */
	private boolean evaluateHostRule(HashMap<String, Double> context) {
		if(this.scalingConfig.getHostRule()!=null){
			if(this.scalingConfig.getHostRule().isOverLoaded(context)){
				return true;
			}else{
				return false;
			}
		}
		return false;
	}

	/**
	 * @see org.roqmessaging.management.stat.KPISubscriber#shutDown()
	 */
	@Override
	public void shutDown() {
		logger.debug("Stopping the Scaling process for Q " +this.qName);
		this.requestSocket.setLinger(0);
		this.requestSocket.close();
		super.shutDown();
		logger.debug("Closing request socket");
	}

	/**
	 * @return the scalingPolicy
	 */
	public IScalingPolicy getScalingPolicy() {
		return scalingPolicy;
	}

	/**
	 * @param scalingPolicy the scalingPolicy to set
	 */
	public void setScalingPolicy(IScalingPolicy scalingPolicy) {
		this.scalingPolicy = scalingPolicy;
	}

}
