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
package org.roqmessaging.management;

import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.zeromq.ZMQ;

/**
 * Class GlobalConfigTImer
 * <p> Description: timer task that publish every minute the new queue topology configuration.
 * 
 * @author sskhiri
 */
public class GlobalConfigTimer extends TimerTask {
	//ZMQ init
	private ZMQ.Socket mngtPubSocket = null;
	private ZMQ.Context context;
	//logger
	private Logger logger = Logger.getLogger(GlobalConfigTimer.class);
	//The global configuration 
	private GlobalConfigurationManager configurationManager = null;
	//The serializer
	private RoQSerializationUtils serializationUtils = null;
	//TODO test the timer and the update configuration.

	/**
	 * 
	 */
	public GlobalConfigTimer(GlobalConfigurationManager manager) {
		this.context = ZMQ.context(1);
		this.mngtPubSocket = context.socket(ZMQ.PUB);
		this.mngtPubSocket.bind("tcp://*:5002");
		
		this.configurationManager = manager;
		this.serializationUtils = new RoQSerializationUtils();
	}

	/**
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		logger.debug("Sending Global configuration update to Management Subscribers ...");
		//1. Get the configuration 
		//2. Publish the configuration
		this.mngtPubSocket.send(new Integer(RoQConstant.MNGT_UPDATE_CONFIG).toString().getBytes(), ZMQ.SNDMORE);
		this.mngtPubSocket.send(serializationUtils.serialiseObject(this.configurationManager.getQueueHostLocation()), 0);
	}
	
	/**
	 * @see java.util.TimerTask#cancel()
	 */
	@Override
	public boolean cancel() {
		this.mngtPubSocket.close();
		return super.cancel();
	}

}
