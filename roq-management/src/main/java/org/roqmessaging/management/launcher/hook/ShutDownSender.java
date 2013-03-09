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
package org.roqmessaging.management.launcher.hook;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.zeromq.ZMQ;

/**
 * Class ShutDownSender
 * <p> Description: Generic object ables to stop shutdown monitor by sending stop signal.
 * 
 * @author sskhiri
 */
public class ShutDownSender {
	//The addres to send the shutdown signal
	private String address = null;
	//define whether the signal has already been sent
	private boolean sent = false;
	//The logger
	private Logger logger = Logger.getLogger(ShutDownSender.class);
	
	/**
	 * @param address the shut down monitor address to call
	 * @param port the shut down monitor port
	 */
	public ShutDownSender(String address) {
		this.address = address;
	}
	
	/**
	 * Send a shutdown signal to the remore shutdown monitor.
	 */
	public void shutdown(){
		if(!sent){
			ZMQ.Socket shutDownMonitor = ZMQ.context(1).socket(ZMQ.REQ);
			shutDownMonitor.setSendTimeOut(0);
			shutDownMonitor.connect(this.address);
			logger.info("Preparing shutdown signal to send to " +this.address );
			shutDownMonitor.send((Integer.toString(RoQConstant.SHUTDOWN_REQUEST)).getBytes(), 0);
			shutDownMonitor.close();
			sent =true;
		}else{
			logger.warn("The shutdown has already been sent");
		}
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
		//Since we change the address;
		sent=false;
	}

}
