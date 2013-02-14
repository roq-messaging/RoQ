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
package org.roqmessaging.state;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.zeromq.ZMQ;

/**
 * Class PublisherConfigState
 * <p> Description: DAO defiing the state of the configuration. 
 * The exchange it should connect and the valid configuration.
 * 
 * @author sskhiri
 */
public class PublisherConfigState {
	private boolean valid = false;
	private ZMQ.Socket exchPub = null;
	private String monitor = null;
	private boolean timeStampServer = false;
	private String publisherID = null;
	//Locking configuration
	private Lock lock = new ReentrantLock();
	/**
	 * @return the valid
	 */
	public boolean isValid() {
		return valid;
	}
	/**
	 * @param valid the valid to set
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	/**
	 * @return the exchPub
	 */
	public ZMQ.Socket getExchPub() {
		return exchPub;
	}
	/**
	 * @param exchPub the exchPub to set
	 */
	public void setExchPub(ZMQ.Socket exchPub) {
		this.exchPub = exchPub;
	}
	/**
	 * @return the lock
	 */
	public Lock getLock() {
		return lock;
	}
	/**
	 * @param lock the lock to set
	 */
	public void setLock(Lock lock) {
		this.lock = lock;
	}
	/**
	 * @return the monitor
	 */
	public String getMonitor() {
		return monitor;
	}
	/**
	 * @param monitor the monitor to set
	 */
	public void setMonitor(String monitor) {
		this.monitor = monitor;
	}
	/**
	 * @return the timeStampServer
	 */
	public boolean isTimeStampServer() {
		return timeStampServer;
	}
	/**
	 * @param timeStampServer the timeStampServer to set
	 */
	public void setTimeStampServer(boolean timeStampServer) {
		this.timeStampServer = timeStampServer;
	}
	/**
	 * @return the publisherID
	 */
	public String getPublisherID() {
		return publisherID;
	}
	/**
	 * @param publisherID the publisherID to set
	 */
	public void setPublisherID(String publisherID) {
		this.publisherID = publisherID;
	}
	

}
