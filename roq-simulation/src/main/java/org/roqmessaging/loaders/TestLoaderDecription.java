/**
 * Copyright 2013 EURANOVA
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
package org.roqmessaging.loaders;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Class TestLoaderDecription
 * <p> Description: Description of the test load. This is a data object that will be use by the Load controller
 * to launch the tests.
 *
 * <br>  The Spawn rate x: every x second we start a producer. This is similar as a warm-up period. Default =1 prod every second
 * <br>     Max producers y: the maximum number of producers to spawn. Default =1
 * <br>     Rate (msg/s) r: the number of messages per minute to be sent by each spawned producer. Default =1msg/s
 * <br>     Duration (min) d: the test duration.  Default =1 min
 * <br>     Payload (kb) p: the message content size, simulated by an array of px1000 size of byte. Default =1 kb
 * <br>     Number of subscribers s: the maximum number of producers to spawn . Default =1
 * <br>     Delay (s) de: the waiting time before starting the test.  Default = 5s

 * Uses  a JSON parser and developing a wrapper for creating a description from a JSON 
 * file http://www.mkyong.com/java/json-simple-example-read-and-write-json/
 * @author sskhiri
 */
public class TestLoaderDecription {
	private int spawnRate =1;
	private int maxProd = 1;
	private int rate =1;
	private float duration = 1.0f;
	private int payload = 1;
	private int maxSub = 1;
	private int delay = 5;
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
	/**
	 * @return the spawnRate
	 */
	public int getSpawnRate() {
		return spawnRate;
	}
	/**
	 * @param spawnRate the spawnRate to set
	 */
	public void setSpawnRate(int spawnRate) {
		this.spawnRate = spawnRate;
	}
	/**
	 * @return the maxProd
	 */
	public int getMaxProd() {
		return maxProd;
	}
	/**
	 * @param maxProd the maxProd to set
	 */
	public void setMaxProd(int maxProd) {
		this.maxProd = maxProd;
	}
	/**
	 * @return the rate
	 */
	public int getRate() {
		return rate;
	}
	/**
	 * @param rate the rate to set
	 */
	public void setRate(int rate) {
		this.rate = rate;
	}
	/**
	 * @return the duration
	 */
	public float getDuration() {
		return duration;
	}
	/**
	 * @param duration the duration to set
	 */
	public void setDuration(float duration) {
		this.duration = duration;
	}
	/**
	 * @return the payload
	 */
	public int getPayload() {
		return payload;
	}
	/**
	 * @param payload the payload to set
	 */
	public void setPayload(int payload) {
		this.payload = payload;
	}
	/**
	 * @return the maxSub
	 */
	public int getMaxSub() {
		return maxSub;
	}
	/**
	 * @param maxSub the maxSub to set
	 */
	public void setMaxSub(int maxSub) {
		this.maxSub = maxSub;
	}
	/**
	 * @return the delay
	 */
	public int getDelay() {
		return delay;
	}
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(int delay) {
		this.delay = delay;
	}
	
	/**
	 * Serialize the test description in a JSON format
	 * @return the JSON serialization of the test description
	 */
	@SuppressWarnings("unchecked")
	public String  serializeInJason() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("delay", this.getDelay());
		jsonObj.put("duration", this.getDuration());
		jsonObj.put("maxSub", this.getMaxSub());
		jsonObj.put("maxPub", this.getMaxProd());
		jsonObj.put("payload", this.getPayload());
		jsonObj.put("rate", this.getRate());
		jsonObj.put("spawnRate", this.getSpawnRate());
		return jsonObj.toJSONString();

	}
	
	/**
	 * Reads a JSON description of the test load.
	 * @throws ParseException a problem occurs when parsing the incoming json
	 * 
	 */
	public void load(final String jsonDescription) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject) parser.parse(jsonDescription);
		this.setDelay(
				jsonObject.get("delay")!=null?((Long)jsonObject.get("delay")).intValue():5);
		this.setDuration(
				jsonObject.get("duration")!=null?((Double)jsonObject.get("duration")).intValue():1.0f);
		this.setMaxProd(
				jsonObject.get("maxPub")!=null?((Long)jsonObject.get("maxPub")).intValue():1);
		this.setMaxSub(
				jsonObject.get("maxSub")!=null?((Long)jsonObject.get("maxSub")).intValue():1);
		this.setPayload(
				jsonObject.get("payload")!=null?((Long)jsonObject.get("payload")).intValue():1);
		this.setRate(
				jsonObject.get("rate")!=null?((Long)jsonObject.get("rate")).intValue():1);
		this.setSpawnRate(
				jsonObject.get("spawnRate")!=null?((Long)jsonObject.get("spawnRate")).intValue():1);
		
		logger.info("The Test load description is :");
		logger.info(this.toString());
	}
	
	/**
	 * Print the description of the test load.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[\nDuration of the test:" + this.getDuration()+"min\n" +
				"Delay before starting:" + this.getDelay()+ "s\n" +
				"Load Rate:" + this.getRate()+ "msg/s\n" +
				"Publishers:" + this.getMaxProd()+ "\n" +
				"Subscribers:" + this.getMaxSub()+ "\n" +
				"Payload:" + this.getPayload()+ "kb\n" +
				"Spawn Rate:" + this.getSpawnRate()+ "producer/s\n" +
				"]";
	}
}
