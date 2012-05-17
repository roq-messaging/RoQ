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
package org.roq.simulation.stat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQUtils;
import org.zeromq.ZMQ;

/**
 * Class KPISubscriber
 * <p> Description: This class shows we can subscribe 
 * 
 * @author sskhiri
 */
public class KPISubscriber implements Runnable, IStoppable{
	//ZMQ configuration
	private ZMQ.Context context = null;
	
	//KPI socket
	private ZMQ.Socket kpiSocket = null;
	
	//The configuration server
	private String configurationServer = null;
	//the Qname to subscriber
	private String qName = null;
	
	//Define whether the thread must continue to run
	private volatile boolean active = true;
	
	//Define whether we need to use a file to store the data
	private boolean useFile = false;
	private BufferedWriter bufferedOutput;
	
	//the looger
	private Logger logger = Logger.getLogger(KPISubscriber.class);
	
	/**
	 * @param globalConfiguration the IP address of the global configuration
	 * @param qName the queue from which we want receive statistic. 
	 */
	public KPISubscriber(String globalConfiguration, String qName, boolean useFile) {
		this.useFile = useFile;
		//ZMQ Init
		this.context = ZMQ.context(1);
		//Copy parameters
		this.configurationServer = globalConfiguration;
		this.qName = qName;
		//init subscription
		subscribe();
		//init file if required
		if(useFile){
			try {
				FileWriter output = new FileWriter(("output" + RoQUtils.getInstance().getFileStamp()), true);
				bufferedOutput = new BufferedWriter(output);
			} catch (IOException e) {
				logger.error("Error when openning file", e);
			}
		}else{
			//Redirect the output in the system.out
			bufferedOutput = new BufferedWriter(new OutputStreamWriter(System.out));
		}
	}
	
	/**
	 * Subscribe to the statistic stream got from the global configuration
	 * @throws IllegalStateException if the monitor stat is not present in the cache
	 */
	private void subscribe() throws IllegalStateException {
		// 1. Get the location in BSON
		// 1.1 Create the request socket
		ZMQ.Socket globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://" + this.configurationServer + ":5000");

		// 1.2 Send the request
		globalConfigReq.send((RoQConstant.BSON_CONFIG_GET_HOST_BY_QNAME + "," + qName).getBytes(), 0);
		byte[] configuration = globalConfigReq.recv(0);
		BSONObject dConfiguration = BSON.decode(configuration);
		String monitorStatServer = (String) dConfiguration.get(RoQConstant.BSON_STAT_MONITOR_HOST);
		Assert.assertNotNull(monitorStatServer);

		// 2. Register a socket to the stat monitor
		kpiSocket = context.socket(ZMQ.SUB);
		kpiSocket.connect(monitorStatServer);
		kpiSocket.subscribe("".getBytes());
		logger.debug("Connected to Stat monitor " + monitorStatServer);

	}



	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		ZMQ.Poller poller = context.poller(1);
		poller.register(kpiSocket);
		
		while (active){
			poller.poll(2000);
			if(poller.pollin(0)){
				//Stat coming from the KPI stream
				BSONObject statObj = BSON.decode(kpiSocket.recv(0));
				logger.debug("Start analysing info code, the use files ="+this.useFile +", code ="+ statObj.get("CMD"));
				switch ((Integer)statObj.get("CMD")) {
				case RoQConstant.STAT_TOTAL_SENT:
					logger.info("1 producer finished, sent " + statObj.get("TotalSent") + " messages.");
					try {
						bufferedOutput.write("PROD," + RoQUtils.getInstance().getFileStamp() + ",FINISH," + statObj.get("PublisherID") 
								+ "," +  statObj.get("TotalSent"));
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case RoQConstant.STAT_PUB_MIN:
					try {
						bufferedOutput.write("SUB," + RoQUtils.getInstance().getFileStamp() + ",STAT," + statObj.get("SubscriberID")  + ","
								+statObj.get("Total"));
						bufferedOutput.newLine();
						bufferedOutput.flush();

					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case RoQConstant.STAT_EXCHANGE_MIN:
					try {
						bufferedOutput.write("EXCH,"
					+ RoQUtils.getInstance().getFileStamp() + "," +
					statObj.get("Minute") + ","
					+ statObj.get("TotalProcessed") + ","
								+ statObj.get("Processed")+ "," 
					+ statObj.get("TotalThroughput")+ ","
								+ statObj.get("Throughput") + "," 
					+ statObj.get("Producers"));
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case RoQConstant.STAT_TOTAL_RCVD:
					try {
						bufferedOutput.write("LIST," + RoQUtils.getInstance().getFileStamp() + "," + statObj.get("Minute") + ","
								+  statObj.get("TotalReceived") + "," +  statObj.get("Received")  + "," +  statObj.get("SubsriberID")  + "," +  statObj.get("MeanLat") );
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				}
			}
			
		}
		
	}

	/* (non-Javadoc)
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		this.active = false;
		this.kpiSocket.close();
		try {
			this.bufferedOutput.close();
		} catch (IOException e) {
			logger.error("Error while closing the buffer output stream", e);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "KPI subscriber";
	}

}
