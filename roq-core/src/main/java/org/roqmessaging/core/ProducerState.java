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
package org.roqmessaging.core;

/**
 * Class ProducerState
 * <p> Description: used to describe the state of a particular produder in the exchange.
 * 
 * @author Sabri Skhiri
 */
public class ProducerState {

	private long bytesSent;
	private int msgSent;
	private boolean active;
	private int inactive;
	private String ID;

	public ProducerState(String ID) {
		this.ID = ID;
		this.active = true;
		this.bytesSent = 0;
		this.inactive = 0;
		this.msgSent = 0;
	}

	/**
	 * Called when a message has been sentg on the exchange by this
	 * producer.
	 */
	public void addMsgSent() {
		this.msgSent++;
	}
	
	/**
	 * @return the msgSent
	 */
	public int getMsgSent() {
		return msgSent;
	}

	/**
	 * @return the unique ID of the producer.
	 */
	public String getID() {
		return ID;
	}

	/**
	 * @return the number of byte sent by the producer.
	 */
	public long getBytesSent() {
		return bytesSent;
	}

	/**
	 * @param bytesSent the number of byte sent for 1 message
	 */
	public void addBytesSent(long bytesSent) {
		this.bytesSent += bytesSent;
	}

	/**
	 * @return true if the corresponding producer is conseidered as active.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active a producer is defined as inactive as soon as he spends 
	 * more than 1 cycle without sending messages.
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	public int getInactive() {
		return inactive;
	}

	public void addInactive() {
		this.inactive++;
	}

	/**
	 * Reset the value of the state.
	 */
	public void reset() {
		this.active = false;
		this.inactive = 0;
		this.msgSent = 0;
		this.bytesSent = 0;
	}
}
