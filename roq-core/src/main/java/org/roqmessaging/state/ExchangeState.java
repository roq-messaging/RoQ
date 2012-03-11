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
		private long throughput;
		private boolean alive;
		private int nbProd;
		private int lost;

		public int getNbProd() {
			return nbProd;
		}

		public void setNbProd(int nbProd) {
			this.nbProd = nbProd;
		}

		public void addNbProd() {
			this.nbProd++;
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

		public ExchangeState(String addr) {
			this.address = addr;
			this.throughput = 0;
			this.alive = true;
			this.lost = 0;
			this.nbProd = 0;
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

}
