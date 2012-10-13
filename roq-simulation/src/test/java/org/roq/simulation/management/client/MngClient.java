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
import org.bson.BasicBSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
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
	 * 
	 * @param qName
	 *            the queue under test
	 */
	public void testCreate(String qName) {
		try {
			IRoQSerializer serializer = new RoQBSONSerializer();
			// 1. Launch a create Queue request
			HashMap<String, String> fields = new HashMap<String, String>();
			fields.put("QName", qName);
			fields.put("Host", RoQUtils.getInstance().getLocalIP());
			byte[] encoded = serializer.serialiazeConfigRequest(RoQConstant.BSON_CONFIG_CREATE_QUEUE, fields);
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
	 * 
	 * @param qName
	 *            the queue under test
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
	 * 
	 * @param qName
	 *            the queue under test
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

	/**
	 * Test the start method on the queue
	 * 
	 * @param qName
	 *            the queue under test
	 */
	public void testStart(String qName) {
		try {
			logger.debug("Start Queue Test");
			IRoQSerializer serializer = new RoQBSONSerializer();
			// 1. Init the request
			HashMap<String, String> fields = new HashMap<String, String>();
			fields.put("QName", qName);
			// Encode in BSON & send
			byte[] eStartRqs = serializer.serialiazeConfigRequest(RoQConstant.BSON_CONFIG_START_QUEUE, fields);
			requestSocket.send(eStartRqs, 0);
			// Get the result and check
			byte[] eStartAnswer = requestSocket.recv(0);
			BSONObject removeAnswer = BSON.decode(eStartAnswer);
			Assert.assertEquals(RoQConstant.OK, removeAnswer.get("RESULT"));
			Thread.sleep(4000);
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
		}
	}

	/**
	 * @param qName
	 *            the queue name on which we are creating the auto scaling
	 *            configuration
	 * @param config
	 *            the configuration for the auto scaling policies.
	 */
	public void testAutoScaling(String qName, AutoScalingConfig config) {
		try {
			IRoQSerializer serializer = new RoQBSONSerializer();
			// 1. Launch a create Queue request
			byte[] encoded = serializer.serialiazeAutoScalingRequest(qName, config,
					RoQConstant.BSON_CONFIG_ADD_AUTOSCALING_RULE);
			// 2. Send the request
			requestSocket.send(encoded, 0);
			// 3. Check the result
			byte[] bres = requestSocket.recv(0);
			BSONObject answer = BSON.decode(bres);
			Assert.assertEquals(RoQConstant.OK, answer.get("RESULT"));
			// 4. Check the auto scaling configuration
			Assert.assertEquals(true, this.checkAutoScalingConfig(qName, config));
			Thread.sleep(4000);
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
		}
	}

	/**
	 * @param qName
	 *            the queue name on which we are creating the auto scaling
	 *            configuration
	 * @param config
	 *            the configuration for the auto scaling policies.
	 */
	public boolean checkAutoScalingConfig(String qName, AutoScalingConfig expecteConfig) {
		try {
			IRoQSerializer serializer = new RoQBSONSerializer();
			// 1. Launch a get autoscaling Queue request
			BSONObject bsonObject = new BasicBSONObject();
			bsonObject.put("CMD", RoQConstant.BSON_CONFIG_GET_AUTOSCALING_RULE);
			bsonObject.put("QName", qName);
			logger.info("Request get Auto scaling info");
			logger.info(bsonObject.toString());
			byte[] encoded = BSON.encode(bsonObject);

			// 2. Send the request
			requestSocket.send(encoded, 0);

			// 3. Check the result
			byte[] bres = requestSocket.recv(0);
			AutoScalingConfig config = serializer.unserializeConfig(bres);
			if (!config.getName().equals(expecteConfig.getName())) {
				return false;
			} else {
				if (expecteConfig.getHostRule() != null) {
					if (expecteConfig.getHostRule().getCPU_Limit() != config.getHostRule().getCPU_Limit()) {
						return false;
					}
				}
				if (expecteConfig.getXgRule() != null) {
					if (expecteConfig.getXgRule().getEvent_Limit() != config.getXgRule().getEvent_Limit()) {
						return false;
					}
				}
				if (expecteConfig.getqRule() != null) {
					if (expecteConfig.getqRule().getProducerNumber() != config.getqRule().getProducerNumber()) {
						return false;
					}
				}
				return true;
			}
		} catch (Exception e) {
			logger.error("Error when testing client ", e);
		}
		return false;
	}

}
