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

import org.roqmessaging.core.Monitor;

/**
 * Class ExchangeState
 * <p> Description: Representation of an Exchange, it describes its state. Used in the {@link Monitor} to maintain a 
 * list of exchanges.
 * 
 * @author sskhiri
 */
public class ExchangeState {

		private String address;
		private long throughput =0;
		private boolean alive;
		private int nbProd =0;
		private int lost=0;
		//Port
		private int frontPort, backPort;

		public int getNbProd() {
			return nbProd;
		}

		public void setNbProd(int nbProd) {
			this.nbProd = nbProd;
		}

		public void addNbProd() {
			this.nbProd++;
		}
		
		public void lessNbProd() {
			this.nbProd--;
		}

		public boolean isAlive() {
			return alive;
		}

		public void setAlive(boolean alive) {
			this.alive = alive;
		}

		public int getLost() {
			return lost;
		}

		public void resetLost() {
			this.lost = 0;
		}

		public void addLost() {
			this.lost++;

		}

		/**
		 * @param addr the host address of the exchange
		 * @param frontPort the front port for the publisher
		 * @param backPort the back port for the subscriber
		 */
		public ExchangeState(String addr, String frontPort,  String backPort) {
			this.address = addr;
			this.throughput = 0;
			this.alive = true;
			this.lost = 0;
			this.nbProd = 0;
			this.backPort=Integer.parseInt(backPort);
			this.frontPort = Integer.parseInt(frontPort);
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String addr) {
			this.address = addr;
		}

		public long getThroughput() {
			return throughput;
		}

		public void setThroughput(long tr) {
			this.throughput = tr;
		}

		public void addThroughput(long tr) {
			this.throughput += tr;
		}
		
		public void lessThroughput(long tr) {
			this.throughput -= tr;
		}

		/**
		 * @return the frontPort
		 */
		public int getFrontPort() {
			return frontPort;
		}

		/**
		 * @param frontPort the frontPort to set
		 */
		public void setFrontPort(int frontPort) {
			this.frontPort = frontPort;
		}

		/**
		 * @return the backPort
		 */
		public int getBackPort() {
			return backPort;
		}

		/**
		 * @param backPort the backPort to set
		 */
		public void setBackPort(int backPort) {
			this.backPort = backPort;
		}

		/**
		 * @param addressToCompare the address of the exchange
		 * @param frontPortToCompare the front  port of the exchange
		 * @param backPortToCompare the back port
		 * @return true if the exchange matches
		 */
		public boolean match(String addressToCompare, String frontPortToCompare, String backPortToCompare) {
			if (addressToCompare.equals(this.getAddress()) && Integer.parseInt(frontPortToCompare)==this.getFrontPort()&&
					Integer.parseInt(backPortToCompare)==this.getBackPort() ) {return true;}
			else return false;
		}

}
