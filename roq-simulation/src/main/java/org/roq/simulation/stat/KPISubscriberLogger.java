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
import org.bson.BSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.utils.RoQUtils;

/**
 * Class KPISubscriberLogger
 * <p>
 * Description: Logs the KPI got by the statistic monitor
 * 
 * @author sskhiri
 */
public class KPISubscriberLogger extends KPISubscriber {

	private BufferedWriter bufferedOutput;
	// the logger
	private Logger logger = Logger.getLogger(KPISubscriber.class);

	/**
	 * @param globalConfiguration
	 *            the IP address of the global configuration
	 * @param qName
	 *            the queue from which we want receive statistic.
	 */
	public KPISubscriberLogger(String globalConfiguration, String qName, boolean useFile) {
		super(globalConfiguration, qName);
		// init file if required
		if (useFile) {
			try {
				FileWriter output = new FileWriter(("output" + RoQUtils.getInstance().getFileStamp()), true);
				bufferedOutput = new BufferedWriter(output);
			} catch (IOException e) {
				logger.error("Error when openning file", e);
			}
		} else {
			// Redirect the output in the system.out
			bufferedOutput = new BufferedWriter(new OutputStreamWriter(System.out));
		}
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		super.shutDown();
		try {
			this.bufferedOutput.close();
		} catch (IOException e) {
			logger.error("Error while closing the buffer output stream", e);
		}

	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return "KPI subscriber Logger";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.roq.simulation.stat.KPISubscriber#processStat(java.lang.Integer)
	 */
	@Override
	public void processStat(Integer CMD, BSONObject statObj) {
		switch ((Integer) statObj.get("CMD")) {
		case RoQConstant.STAT_EXCHANGE_ID:
			logger.info(" Stat from Exchange  " + statObj.get("ID") + " .");
			break;

		case RoQConstant.STAT_TOTAL_SENT:
			logger.info("1 producer finished, sent " + statObj.get("TotalSent") + " messages.");
			try {
				bufferedOutput.write("PROD," + RoQUtils.getInstance().getFileStamp() + ",FINISH,"
						+ statObj.get("PublisherID") + "," + statObj.get("TotalSent"));
				bufferedOutput.newLine();
				bufferedOutput.flush();
			} catch (IOException e) {
				logger.error("Error when writing the report in the output stream", e);
			}
			break;
		case RoQConstant.STAT_PUB_MIN:
			try {
				bufferedOutput.write("SUB," + RoQUtils.getInstance().getFileStamp() + ",STAT,"
						+ statObj.get("SubscriberID") + "," + statObj.get("Total"));
				bufferedOutput.newLine();
				bufferedOutput.flush();

			} catch (IOException e) {
				logger.error("Error when writing the report in the output stream", e);
			}
			break;
		case RoQConstant.STAT_EXCHANGE_MIN:
			try {
				bufferedOutput.write("EXCH," + RoQUtils.getInstance().getFileStamp() + ","
						+ statObj.get("Minute") + "," + statObj.get("TotalProcessed") + ","
						+ statObj.get("Processed") + "," + statObj.get("TotalThroughput") + ","
						+ statObj.get("Throughput") + "," + statObj.get("Producers"));
				bufferedOutput.newLine();
				bufferedOutput.flush();
			} catch (IOException e) {
				logger.error("Error when writing the report in the output stream", e);
			}
			break;
		case RoQConstant.STAT_TOTAL_RCVD:
			try {
				bufferedOutput.write("LIST," + RoQUtils.getInstance().getFileStamp() + ","
						+ statObj.get("Minute") + "," + statObj.get("TotalReceived") + ","
						+ statObj.get("Received") + "," + statObj.get("SubsriberID") + ","
						+ statObj.get("MeanLat"));
				bufferedOutput.newLine();
				bufferedOutput.flush();
			} catch (IOException e) {
				logger.error("Error when writing the report in the output stream", e);
			}
			break;
		}

	}

}
