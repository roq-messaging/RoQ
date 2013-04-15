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
package org.roqmessaging.core.data;

/**
 * Class StatData
 * <p> Description: Data object representing stat information for a particular exchange.
 * 
 * @author sskhiri
 */
public class StatDataState {
	private String statHost =null;
	private long throughput = 0;
	private long max_bw=0;
	private int totalProcessed =0;
	public int processed =0;
	
	
	/**
	 * @return the throughput
	 */
	public long getThroughput() {
		return throughput;
	}
	/**
	 * @param throughput the throughput to set
	 */
	public void setThroughput(long throughput) {
		this.throughput = throughput;
	}
	/**
	 * @return the max_bw
	 */
	public long getMax_bw() {
		return max_bw;
	}
	/**
	 *  bandwidth limit, in bytes/minute, per producer
	 * @param max_bw the max_bw to set
	 */
	public void setMax_bw(long max_bw) {
		this.max_bw = max_bw;
	}
	/**
	 * @return the totalProcessed
	 */
	public int getTotalProcessed() {
		return totalProcessed;
	}
	/**
	 * @param totalProcessed the totalProcessed to set
	 */
	public void setTotalProcessed(int totalProcessed) {
		this.totalProcessed = totalProcessed;
	}
	/**
	 * @return the processed
	 */
	public int getProcessed() {
		return processed;
	}
	/**
	 * @param processed the processed to set
	 */
	public void setProcessed(int processed) {
		this.processed = processed;
	}
	/**
	 * @return the statHost
	 */
	public String getStatHost() {
		return statHost;
	}
	/**
	 * @param statHost the statHost to set
	 */
	public void setStatHost(String statHost) {
		this.statHost = statHost;
	}

}
