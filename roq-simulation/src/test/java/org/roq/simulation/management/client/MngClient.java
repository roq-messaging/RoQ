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
package org.roq.simulation.management.client;

import java.util.HashMap;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.zeromq.ZMQ;

/**
 * Class MngClientThread
 * <p>
 * Description: Test class that test the behavor if the management server and
 * its BSON request interface.
 * 
 * @author sskhiri
 */
public class MngClient {
	// ZMQ
	private ZMQ.Context context = null;
	private ZMQ.Socket requestSocket = null;

	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

	/**
	 * @param gcmAddr
	 *            the address of the global config server
	 */
	public MngClient(String gcmAddr) {
		// ZMQ init
		this.context = ZMQ.context(1);
		this.requestSocket = this.context.socket(ZMQ.REQ);
		this.requestSocket.connect("tcp://" + gcmAddr + ":5003");
	}

	/**
	 * Test the creation of a queue by the BSON interface.
	 * @param qName the queue under test
	 */
	public void testCreate(String qName) {
		try {
			IRoQSerializer serializer = new RoQBSONSerializer();
			// 1. Launch a create Queue request
			HashMap<String, String> fields = new HashMap<String, String>();
			fields.put("QName", qName);
			fields.put("Host", RoQUtils.getInstance().getLocalIP());
			byte[] encoded = serializer.serialiazeConfigRequest(RoQConstant.BSON_CONFIG_START_QUEUE, fields);
			requestSocket.send(encoded, 0);
			byte[] bres = requestSocket.recv(0);
			BSONObject answer = BSON.decode(bres);
			Assert.assertEquals(RoQConstant.OK, answer.get("RESULT"));
			Thread.sleep(4000);
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
		}
	}

	/**
	 * Test the removal of a queue by the BSON interface.
	 * @param qName the queue under test
	 */
	public void testRemove(String qName) {
		try {
			IRoQSerializer serializer = new RoQBSONSerializer();
			// 1. Init the request
			HashMap<String, String> fields = new HashMap<String, String>();
			fields.put("QName", qName);
			// Encode in BSON & send
			byte[] eRemoveRqs = serializer.serialiazeConfigRequest(RoQConstant.BSON_CONFIG_REMOVE_QUEUE, fields);
			requestSocket.send(eRemoveRqs, 0);
			// Get the result and check
			byte[] eEemoveAnswer = requestSocket.recv(0);
			BSONObject removeAnswer = BSON.decode(eEemoveAnswer);
			Assert.assertEquals(RoQConstant.OK, removeAnswer.get("RESULT"));
			Thread.sleep(4000);
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
		}
	}

	/**
	 * Test the stop method on the queue
	 * @param qName the queue under test
	 */
	public void testStop(String qName) {
		try {
			IRoQSerializer serializer = new RoQBSONSerializer();
			// 1. Init the request
			HashMap<String, String> fields = new HashMap<String, String>();
			fields.put("QName", qName);
			// Encode in BSON & send
			byte[] eRemoveRqs = serializer.serialiazeConfigRequest(RoQConstant.BSON_CONFIG_STOP_QUEUE, fields);
			requestSocket.send(eRemoveRqs, 0);
			// Get the result and check
			byte[] eEemoveAnswer = requestSocket.recv(0);
			BSONObject removeAnswer = BSON.decode(eEemoveAnswer);
			Assert.assertEquals(RoQConstant.OK, removeAnswer.get("RESULT"));
			Thread.sleep(4000);
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
		}
	
		
	}

}
