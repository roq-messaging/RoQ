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
package org.roqmessaging.management.bson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Class BSONUnitTest
 * <p> Description: Test the basic encoding and decoding .
 * 
 * @author sskhiri
 */
public class BSONUnitTest {
	//under tests
	private RoQBSONSerializer serialiazer = new RoQBSONSerializer();
	//Logger
	private Logger logger = Logger.getLogger(BSONUnitTest.class);

	@SuppressWarnings("unchecked")
	@Test
	public void testHostList() {
		List<String> hosts = new ArrayList<String>();
		hosts.add("127.0.1.1");
		hosts.add("127.0.1.2");
		hosts.add("127.0.1.3");
		hosts.add("127.0.1.4");
		
		//Create the bson object
		BSONObject bsonObject = new BasicBSONObject();
		bsonObject.put("hosts", hosts);
		logger.debug(bsonObject.toString());
		
		//Encode the object
		final byte [] encodedHost = BSON.encode(bsonObject);
		
		//Decoding
		
		BasicBSONDecoder decoder = new BasicBSONDecoder();
		BSONObject newHostObject = decoder.readObject(encodedHost);
		logger.debug(newHostObject.toString());
		
		Assert.assertEquals(bsonObject.toString(), newHostObject.toString());
		ArrayList<String>  obj =  (ArrayList<String>) newHostObject.get("hosts");
		Assert.assertEquals(4, obj.size());
	}
	
	@Test
	public void testQueueEncoding() throws Exception {
		List<QueueManagementState> queues = new ArrayList<QueueManagementState>();
		QueueManagementState q1 = new QueueManagementState("queue1", "127.0.1.1", false, "conf1");
		queues.add(q1);
		QueueManagementState q2 = new QueueManagementState("queue2", "127.0.1.2", false, "conf2");
		queues.add(q2);
		QueueManagementState q3 = new QueueManagementState("queue3", "127.0.1.3", false,"conf3");
		queues.add(q3);
		
		testQueues(queues);
	}

	/**
	 * @param queues the list of queue state to encode and decode.
	 */
	private void testQueues(List<QueueManagementState> queues) {
		List<BSONObject> bsonArray = new ArrayList<BSONObject>();
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
		logger.debug("To encode:");
		logger.debug(mainQ.toString());
		
		//Encode test
		BasicBSONEncoder encoder = new BasicBSONEncoder();
		final byte[] encodedQueues = encoder.encode(mainQ);
		
		//Decode
		BasicBSONDecoder decoder = new BasicBSONDecoder();
		BSONObject decodedQ = decoder.readObject(encodedQueues);
		logger.debug(mainQ.toString());
		Assert.assertEquals(mainQ.toString(), decodedQ.toString());
		
		final byte[] encodedQueuesRoQ = serialiazer.serialiseQueues(queues);
		BSONObject decodedQRoQ = decoder.readObject(encodedQueuesRoQ);
		Assert.assertEquals(mainQ.toString(), decodedQRoQ.toString());
		
		@SuppressWarnings("unchecked")
		ArrayList<BSONObject> dedodedList = (ArrayList<BSONObject>) decodedQ.get("Queues");
		for (BSONObject bsonObject : dedodedList) {
			String name = (String) bsonObject.get("Name");
			String host = (String) bsonObject.get("Host");
			boolean running = (Boolean) bsonObject.get("State");
			logger.debug("Queue: "+ name +" "+ host +" "+ running);
		}
	}
	
	/**
	 * Tests the code in the Global configuration manager to encode 
	 * Monitor host and Stat host
	 * @throws Exception
	 */
	@Test
	public void testGetHostByQNameBSON() throws Exception {
		BSONObject answer = new BasicBSONObject();
		answer.put(RoQConstant.BSON_MONITOR_HOST,"tcp://127.0.1.1:5000");
		answer.put(RoQConstant.BSON_STAT_MONITOR_HOST, "tcp://127.0.0.1:5061");
		this.logger.debug("Test Get Host by QName:");
		this.logger.debug(answer);
		
		//Encode
		byte[] encoded = 	BSON.encode(answer);
		
		//Decode
		BSONObject decoded= 	BSON.decode(encoded);
		Assert.assertEquals(answer.toString(), decoded.toString());
		
		//Test with serializer
		byte[] encoded2 = 	serialiazer.serialiazeMonitorInfo("tcp://127.0.1.1:5000", "tcp://127.0.0.1:5061");
		Assert.assertEquals(answer.toString(), BSON.decode(encoded2).toString());
	}
	
	/**
	 * test the java encoding Vs BSON
	 * @throws Exception
	 */
	@Test
	public void testBSON_VS_JAVA() throws Exception {
		this.logger.debug("Test encoding JAVA");
		BSONObject request = new BasicBSONObject();
		request.put("CMD",RoQConstant.BSON_CONFIG_GET_HOST_BY_QNAME);
		request.put("QName", "myName");
		
		BSONObject request2 = new BasicBSONObject();
		request2.put("CMD",RoQConstant.BSON_CONFIG_GET_HOST_BY_QNAME);
		request2.put("QName", "SuperSabriSayen2");
			
		this.logger.debug(request);
		this.logger.debug(request2);
		
		//Encode
		byte[] encoded = 	BSON.encode(request);
		byte[] encoded2 = 	BSON.encode(request2);
		
		String string = new String(encoded);
		String string2 = new String(encoded2);
		logger.debug(string);
		logger.debug(string2);
		
		if(! string.contains(",")){
			logger.debug(BSON.decode(encoded).toString());
		}
	}
	
	/**
	 * Test the management interface query language
	 * @throws Exception
	 */
	@Test
	public void testBSONMngRequest() throws Exception {
		IRoQSerializer serializer = new RoQBSONSerializer();
		
		//1. Remove Queue
		HashMap<String, String> fields = new HashMap<String, String>();
		fields.put("QName", "myName");
		byte[] encoded = 	serializer.serialiazeConfigRequest(RoQConstant.BSON_CONFIG_REMOVE_QUEUE, fields);
		logger.debug(BSON.decode(encoded).toString());
		BSONObject decoded = BSON.decode(encoded);
		Assert.assertEquals(RoQConstant.BSON_CONFIG_REMOVE_QUEUE, decoded.get("CMD"));
		Assert.assertEquals("myName", decoded.get("QName"));
		
		byte[] encodedAnswer = serializer.serialiazeConfigAnswer(RoQConstant.FAIL, "The queue does not exist");
		logger.debug(BSON.decode(encodedAnswer).toString());
		BSONObject decodedAnswer = BSON.decode(encodedAnswer);
		Assert.assertEquals("The queue does not exist", decodedAnswer.get("COMMENT"));
		
	}
	
	@Test
	public void testBSONScalingRule() throws Exception {
		AutoScalingConfig config = new AutoScalingConfig(new HostScalingRule(30, 25), 
				new LogicalQScalingRule(10000, 1000), null);
		config.setName("Myconfig1");
		RoQBSONSerializer serializer = new RoQBSONSerializer();
		byte[] encoded = serializer.serialiazeAutoScalingRequest("queue1", config);
		AutoScalingConfig configDecoded = serializer.unserializeConfig(encoded);
		Assert.assertEquals(config.getHostRule().getCPU_Limit(), configDecoded.getHostRule().getCPU_Limit());
		Assert.assertEquals(config.getqRule().getProducerNumber(), configDecoded.getqRule().getProducerNumber());
		Assert.assertEquals(config.getqRule().getThrougputNumber(), configDecoded.getqRule().getThrougputNumber());
		
	}

}
