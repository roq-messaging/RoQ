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

import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class Heartbit
 * <p> Description: The heartbeat function. This timer send the code "5" to his s_monitor.
 * 
 * @author sskhiri
 */
public class Heartbeat extends TimerTask {
	private ZMQ.Context hbcontext;
	private ZMQ.Socket hbsocket;

	public Heartbeat(String  s_monitor) {
		this.hbcontext = ZMQ.context(1);
		this.hbsocket = hbcontext.socket(ZMQ.PUB);
		this.hbsocket.connect(s_monitor);
	}
	public void run() {
		hbsocket.send(("5," + RoQUtils.getInstance().getLocalIP()).getBytes(), 0);
	}
}
