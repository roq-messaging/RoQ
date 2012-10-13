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
package org.roqmessaging.management.serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.bson.BasicBSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Class BSONSerializer
 * <p> Description: Responsible for serializing the management configuration in BSON.
 * This class uses the Mongo DB driver for BSON (https://github.com/mongodb/mongo-java-driver/).
 * 
 * @author sskhiri
 */
public class RoQBSONSerializer implements IRoQSerializer {
	private Logger logger = Logger.getLogger(RoQBSONSerializer.class);
	//Bson decoder
	private BasicBSONDecoder decoder = new BasicBSONDecoder();

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#serialiseQueues(java.util.ArrayList)
	 */
	public byte[] serialiseQueues(List<QueueManagementState> queues) {
		logger.debug("Encoding Qs  in BSON ...");
		//1. We create a list of BSON objects
		List<BSONObject> bsonArray = new ArrayList<BSONObject>();
		//2. We add a line for each Q
		for (QueueManagementState queue_i : queues) {
			BSONObject oQ = new BasicBSONObject();
			oQ.put("Name", queue_i.getName());
			oQ.put("Host", queue_i.getHost());
			oQ.put("State", queue_i.isRunning());
			bsonArray.add(oQ);
		}
		
		//Build the main array containing all queues
		BSONObject  mainQ= new BasicBSONObject();
		mainQ.put("Queues", bsonArray);
		logger.debug(mainQ.toString());
		
		//Encode test
		BasicBSONEncoder encoder = new BasicBSONEncoder();
		return encoder.encode(mainQ);
		
	}

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#serialiseHosts(java.util.ArrayList)
	 */
	public byte[] serialiseHosts(ArrayList<String> hosts) {
		logger.debug("Encoding hosts in BSON ...");
		// Create the bson object
		BSONObject bsonObject = new BasicBSONObject();
		bsonObject.put("Hosts", hosts);
		logger.debug(bsonObject.toString());

		// Encode the object
		return  BSON.encode(bsonObject);
	}

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#serialiseCMDID(java.lang.String)
	 */
	public byte[] serialiseCMDID(int cmd) {
		BSONObject bsonObject = new BasicBSONObject();
		bsonObject.put("CMD", cmd);
		logger.debug("Encoding CMD ID in BSON= "+bsonObject.toString());
		return BSON.encode(bsonObject);
	}
	
	/**
	 * @param qName the queue name 
	 * @param scalingCfg the scaling configuration
	 * @return the serialization of the get auto scaling configuration answers.
	 */
	public byte[] serialiazeAutoScalingConfigAnswer(String qName, AutoScalingConfig scalingCfg){
		BSONObject bsonObject = new BasicBSONObject();
		bsonObject.put("RESULT",RoQConstant.OK );
		bsonObject.put("COMMENT", "The auto scaling configuration for queue "+ qName);
		bsonObject = serializeASConfig(bsonObject, scalingCfg);
		logger.debug("Encoding autoscaling answer in BSON= "+bsonObject.toString());
		return BSON.encode(bsonObject);
	}
	
	/**
	 * Serialize the autoscaling configuration
	 * @param qName the queue name on which the auto scaling configuration will be associated.
	 * @param scalingCfg the autoscaling configuration notice that the queue name must be set to the auto scaling configuration.
	 * @param cmd the command to play either RoQConstant.BSON_CONFIG_ADD_AUTOSCALING_RULE or RoQConstant.BSON_CONFIG_GET_AUTOSCALING_RULE
	 * @return the encoded request in BSON
	 */
	public byte[] serialiazeAutoScalingRequest(String qName, AutoScalingConfig scalingCfg, int cmd ){
		BSONObject bsonObject = new BasicBSONObject();
		bsonObject.put("CMD",cmd );
		bsonObject.put("QName", qName);
		bsonObject = serializeASConfig(bsonObject, scalingCfg);
		logger.debug("Encoding autoscaling request in BSON= "+bsonObject.toString());
		return BSON.encode(bsonObject);
	}
	
	/**
	 * @param bsonObject the bson object used for the encoding
	 * @param scalingCfg the scaling cofiguration
	 */
	private BSONObject serializeASConfig(BSONObject bsonObject, AutoScalingConfig scalingCfg) {
		bsonObject.put(RoQConstant.BSON_AUTOSCALING_CFG_NAME, scalingCfg.getName());
		if(scalingCfg.getHostRule()!=null){
			BSONObject hostObject = new BasicBSONObject();
			hostObject.put(RoQConstant.BSON_AUTOSCALING_HOST_CPU, scalingCfg.getHostRule().getCPU_Limit());
			hostObject.put(RoQConstant.BSON_AUTOSCALING_HOST_RAM , scalingCfg.getHostRule().getRAM_Limit());
			bsonObject.put(RoQConstant.BSON_AUTOSCALING_HOST, hostObject);
		}
		if(scalingCfg.getXgRule()!=null){
			BSONObject xchangeObject = new BasicBSONObject();
			xchangeObject.put(RoQConstant.BSON_AUTOSCALING_XCHANGE_THR, scalingCfg.getXgRule().getEvent_Limit());
			bsonObject.put(RoQConstant.BSON_AUTOSCALING_XCHANGE, xchangeObject);
		}
		if(scalingCfg.getqRule()!=null){
			BSONObject qObject = new BasicBSONObject();
			qObject.put(RoQConstant.BSON_AUTOSCALING_Q_THR_EXCH, scalingCfg.getqRule().getThrougputNumber());
			qObject.put(RoQConstant.BSON_AUTOSCALING_Q_PROD_EXCH , scalingCfg.getqRule().getProducerNumber());
			bsonObject.put(RoQConstant.BSON_AUTOSCALING_QUEUE, qObject);
		}
		return bsonObject;
	}

	/**
	 * Decode an autoscaling request in BSON
	 * @param encodedCfg the encoded auto scaling rule
	 * @return the autoscaling rule model
	 */
	public AutoScalingConfig unserializeConfig(byte[] encodedCfg){
		logger.debug("Unserializing encoded Q");
		AutoScalingConfig result = new AutoScalingConfig();
		BSONObject decodedCfg = decoder.readObject(encodedCfg);
		//1. Set the configuration name
		result.setName((String) decodedCfg.get(RoQConstant.BSON_AUTOSCALING_CFG_NAME));
		BSONObject hRule = (BSONObject) decodedCfg.get(RoQConstant.BSON_AUTOSCALING_HOST);
		if(hRule!=null){
			result.setHostRule(new HostScalingRule(((Integer)hRule.get(RoQConstant.BSON_AUTOSCALING_HOST_RAM)).intValue(), 
					((Integer)hRule.get(RoQConstant.BSON_AUTOSCALING_HOST_CPU)).intValue()));
			logger.debug("Host scaling rule : "+ result.getHostRule().toString());
		}
		//2. Extract the xchange rule
		BSONObject xRule = (BSONObject) decodedCfg.get(RoQConstant.BSON_AUTOSCALING_XCHANGE);
		if(xRule!=null){
			result.setXgRule(new XchangeScalingRule(((Integer)xRule.get(RoQConstant.BSON_AUTOSCALING_XCHANGE_THR)).intValue(), 
					 0f));
			logger.debug("Host scaling rule : "+ result.getHostRule().toString());
		}
		
		//3. Extract the Q-level rule
		BSONObject qRule = (BSONObject) decodedCfg.get(RoQConstant.BSON_AUTOSCALING_QUEUE);
		if(qRule!=null){
			result.setqRule(new LogicalQScalingRule(((Integer)qRule.get(RoQConstant.BSON_AUTOSCALING_Q_PROD_EXCH)).intValue(), 
					((Integer)qRule.get(RoQConstant.BSON_AUTOSCALING_Q_THR_EXCH)).intValue()));
			logger.debug("Host scaling rule : "+ result.getHostRule().toString());
		}
		return result;
	}
	

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#unSerializeQueues(byte[])
	 */
	public List<QueueManagementState> unSerializeQueues(byte[] encodedQ) {
		logger.debug("Unserializing encoded Q");
		BSONObject decodedQ = decoder.readObject(encodedQ);
		
		//Building the Queue state Array
		@SuppressWarnings("unchecked")
		ArrayList<BSONObject> dedodedList = (ArrayList<BSONObject>) decodedQ.get("Queues");
		List<QueueManagementState> queues = new ArrayList<QueueManagementState>();
		
		//Through the list of decoded object, we re build the states
		for (BSONObject bsonObject : dedodedList) {
			QueueManagementState state_i =  new QueueManagementState((String) bsonObject.get("Name"),
					(String) bsonObject.get("Host"),  (Boolean) bsonObject.get("State"), ((String)bsonObject.get("ASConfig")));
			queues.add(state_i);
			logger.debug(state_i.toString());
		}
		return queues;
	}

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#unSerializeHosts(byte[])
	 */
	@SuppressWarnings("unchecked")
	public List<String> unSerializeHosts(byte[] encodedH) {
		BSONObject newHostObject = decoder.readObject(encodedH);
		return (ArrayList<String>) newHostObject.get("Hosts");
	}

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#serialiazeMonitorInfo(java.lang.String, java.lang.String)
	 */
	public byte[] serialiazeMonitorInfo(String monitor, String statMonitor) {
		BSONObject answer = new BasicBSONObject();
		answer.put(RoQConstant.BSON_MONITOR_HOST, monitor);
		answer.put(RoQConstant.BSON_STAT_MONITOR_HOST, statMonitor);
		this.logger.debug("Serialize Get Host by QName:");
		this.logger.debug(answer);
		return BSON.encode(answer);
	}

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#serialiazeConfigRequest(int, java.lang.String)
	 */
	public byte[] serialiazeConfigRequest(int cmdID, HashMap<String, String> fields) {
		BSONObject request = new BasicBSONObject();
		request.put("CMD",cmdID);
		for (String key : fields.keySet()) {
			request.put(key	,fields.get(key));
		}
		return	BSON.encode(request);
	}

	/**
	 * @see org.roqmessaging.management.serializer.IRoQSerializer#serialiazeConfigAnswer(int, java.lang.String)
	 */
	public byte[] serialiazeConfigAnswer(int result, String comment) {
		BSONObject answer = new BasicBSONObject();
		answer.put("RESULT",result);
		answer.put("COMMENT", comment);
		logger.debug(answer.toString());
		return	BSON.encode(answer);
	}

}
