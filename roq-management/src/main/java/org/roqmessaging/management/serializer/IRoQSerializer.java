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

import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.server.state.QueueManagementState;

/**
 * Interface IRoQSerializer
 * <p> Description: define the common behavior of all serializer used for communicate with outside.
 * The client are intended to use the adapter factory pattern to select the right implementation.
 * 
 * @author sskhiri
 */
public interface IRoQSerializer {
	
	/**
	 * @param queues the list of queue to serialized
	 * @return the serialised version
	 */
	public byte[] serialiseQueues(List<QueueManagementState> queues);
	
	/**
	 * @param encodedQ the encoded Qs
	 * @return the list of Queue Management state
	 */
	public List<QueueManagementState> unSerializeQueues(byte [] encodedQ);
	
	/**
	 * @param hosts the list of RoQ host (ip address)
	 * @return the serialized message
	 */
	public byte[] serialiseHosts(ArrayList<String> hosts);
	
	/**
	 * @param encodedH the encoded list of hosts 
	 * @return the list of Host ip address
	 */
	public List<String> unSerializeHosts(byte [] encodedH);
	
	/**
	 * @param cmd the cmd ID to serialize
	 * @return the encoded cmd ID
	 */
	public byte[] serialiseCMDID(int cmd);
	
	/**
	 * @param Monitor the monitor host
	 * @param StatMonitor the stat monitor host
	 * @return the serialized byte array
	 */
	public byte[] serialiazeMonitorInfo(String monitor, String statMonitor);
	
	/**
	 * @param cmdID the command to execute on the queue
	 * @param fields the list of fields to include
	 * @return the serialized byte array
	 */
	public byte[] serialiazeConfigRequest(int cmdID, HashMap<String, String> fields);
	
	/**
	 * @param result the result of the request
	 * @param comment the comment on the request, can be null if the result is OK
	 * @return the serialized byte array
	 */
	public byte[] serialiazeConfigAnswer(int result, String comment);

	/**
	 * Notice that the configuration should not exist yet.
	 * @param qName the queue name on which we will create the auto scaling configuration
	 * @param scalingCfg the auto scaling configuration
	 * @param cmd the command to play either RoQConstant.BSON_CONFIG_ADD_AUTOSCALING_RULE or RoQConstant.BSON_CONFIG_GET_AUTOSCALING_RULE
	 * @return the encoded auto scaling configuration create request
	 */
	public byte[] serialiazeAutoScalingRequest(String qName, AutoScalingConfig scalingCfg, int cmd );
	
	/**
	 * Decode an autoscaling request in BSON
	 * @param encodedCfg the encoded auto scaling rule
	 * @return the autoscaling rule model
	 */
	public AutoScalingConfig unserializeConfig(byte[] encodedCfg);
	
	/**
	 * @param qName the queue name 
	 * @param scalingCfg the scaling configuration
	 * @return the serialization of the get auto scaling configuration answers.
	 */
	public byte[] serialiazeAutoScalingConfigAnswer(String qName, AutoScalingConfig scalingCfg);
	
}
