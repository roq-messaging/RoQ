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

import org.apache.log4j.Logger;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.GlobalConfigurationState;
import org.zeromq.ZMQ;

/**
 * Class KPISubscriber
 * <p> Description: This class shows we can subscribe 
 * 
 * @author sskhiri
 */
public class KPISubscriber implements Runnable{
	//ZMQ configuration
	private ZMQ.Context context = null;
	
	//KPI socket
	private ZMQ.Socket kpiSocket = null;
	
	//Global configuration
	private GlobalConfigurationState configurationState = null;
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
	private void subscribe() throws IllegalStateException{
		//1. Get the location of the stat monitor address to subscribe from the global configuration
		this.configurationState = new GlobalConfigurationState(this.configurationServer);
		this.configurationState.refreshConfiguration();
		String monitorStatServer = this.configurationState.getQueueMonitorStatMap().get(qName);
		if(monitorStatServer == null) throw new IllegalStateException("The stat monitor is not present in the global configuration cache !" +
				" Cannot subscribe KPI");
		//2. Register a socket to the stat monitor
		kpiSocket = context.socket(ZMQ.SUB);
		kpiSocket.bind(monitorStatServer);
		kpiSocket.subscribe("".getBytes()); 
	}



	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		ZMQ.Poller poller = context.poller(1);
		poller.register(kpiSocket);
		int infoCode =0;
		
		while (active){
			poller.poll(2000);
			if(poller.pollin(0)){
				//Stat comming from the KPI stream
				 
				// Stats info: 1* producers, 2* exch, 3*listeners
				String info[] = new String(kpiSocket.recv(0)).split(",");
				infoCode = Integer.parseInt(info[0]);

				logger.debug("Start analysing info code, the use files ="+this.useFile);
				switch (infoCode) {
				case RoQConstant.STAT_TOTAL_SENT:
					logger.info("1 producer finished, sent " + info[2] + " messages.");
					try {
						bufferedOutput.write("PROD," + RoQUtils.getInstance().getFileStamp() + ",FINISH," + info[1]
								+ "," + info[2]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case 12:
					try {
						bufferedOutput.write("PROD," + RoQUtils.getInstance().getFileStamp() + ",STAT," + info[1] + ","
								+ info[2] + "," + info[3]);
						bufferedOutput.newLine();
						bufferedOutput.flush();

					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case RoQConstant.STAT_MIN:
					try {
						bufferedOutput.write("EXCH," + RoQUtils.getInstance().getFileStamp() + "," + info[1] + ","
								+ info[2] + "," + info[3] + "," + info[4] + "," + info[5] + "," + info[6]);
						bufferedOutput.newLine();
						bufferedOutput.flush();
					} catch (IOException e) {
						logger.error("Error when writing the report in the output stream", e);
					}
					break;
				case RoQConstant.STAT_TOTAL_RCVD:
					try {
						bufferedOutput.write("LIST," + RoQUtils.getInstance().getFileStamp() + "," + info[1] + ","
								+ info[2] + "," + info[3] + "," + info[4] + "," + info[5]);
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

}
