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
		QueueManagementState q1 = new QueueManagementState("queue1", "127.0.1.1", false);
		queues.add(q1);
		QueueManagementState q2 = new QueueManagementState("queue2", "127.0.1.2", false);
		queues.add(q2);
		QueueManagementState q3 = new QueueManagementState("queue3", "127.0.1.3", false);
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

}
