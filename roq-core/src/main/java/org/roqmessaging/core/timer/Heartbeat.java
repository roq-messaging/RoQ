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
package org.roqmessaging.core.timer;

import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class Heartbit
 * <p> Description: The heartbeat function. This timer send the code "5" to his s_monitor.
 * 
 * @author sskhiri
 */
public class Heartbeat extends TimerTask {
	private Logger logger = Logger.getLogger(Heartbeat.class);
	private ZMQ.Context hbcontext;
	private ZMQ.Socket hbsocket;
	private int fwPort=0, bkPort=0;

	public Heartbeat(String  s_monitor, int frontPort, int backPort) {
		this.hbcontext = ZMQ.context(1);
		this.hbsocket = hbcontext.socket(ZMQ.PUB);
		this.hbsocket.connect(s_monitor);
		this.fwPort=frontPort;
		this.bkPort= backPort;
	}
	public void run() {
		String address = RoQUtils.getInstance().getLocalIP();
		logger.debug("Local address to send with heart bit "+  address+","+fwPort+","+ bkPort);
		hbsocket.send((new Integer(RoQConstant.EVENT_HEART_BEAT).toString()+"," +address+","+fwPort+","+ bkPort ).getBytes(), 0);
	}
}
