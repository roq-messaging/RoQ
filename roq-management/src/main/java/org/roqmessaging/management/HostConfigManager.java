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
package org.roqmessaging.management;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;


/**
 * Class HostConfigManager
 * <p> Description: Responsible for the local management of the queue elements. For each host it will track the list 
 * of monitors and exchanges.
 * 
 * @author sskhiri
 */
public class HostConfigManager implements Runnable {
	
	//ZMQ elements
	private ZMQ.Socket[] globalMonitorSub, globalMonitorPub;
	private ZMQ.Context context;
	//Logger
	private Logger logger = Logger.getLogger(HostConfigManager.class);
	//Global manager config
	private String globalManagerAddress = null;
	//The properties with global manager location
	private String managerLocations[] = null;
	
	/**
	 * Constructor
	 * @param propertyFilePath the path for the property file to load.
	 */
	public HostConfigManager(String propertyFilePath){
		//load properties
		try {
			loadProperties(propertyFilePath);
		} catch (UnknownHostException e) {
			logger.error("Error while reading property file in "+ propertyFilePath, e);
		}
		assert this.managerLocations!=null;
		
		// ZMQ Init
		context = ZMQ.context(1);
		configureSocketArray();
	}
	
	/**
	 * Create socket configuration according to the number of Global managers
	 */
	private void configureSocketArray() {
		globalMonitorPub = new ZMQ.Socket[managerLocations.length];
		globalMonitorPub = new ZMQ.Socket[managerLocations.length];
		for (int i = 0; i < this.managerLocations.length; i++) {
			String globalManager_i = this.managerLocations[i];
			globalMonitorPub[i] = context.socket(ZMQ.PUB);
			globalMonitorPub[i].bind("tcp://"+globalManager_i+":5573");

			globalMonitorSub[i] = context.socket(ZMQ.SUB);
			globalMonitorSub[i].bind("tcp://"+globalManager_i+":5571");
			globalMonitorSub[i].subscribe("".getBytes());
		}
		
	}

	/**
	 * Load the manager location from the property file.
	 * @param propertyFilePath the path to the property file.
	 * @throws UnknownHostException 
	 * @throws IOException 
	 */
	private void loadProperties(String propertyFilePath) throws UnknownHostException {
		Properties defaultProps = new Properties();
		try {
			FileInputStream in = new FileInputStream("src/main/resources/config.properties");
			defaultProps.load(in);
			in.close();
		} catch (FileNotFoundException e) {
			logger.warn("No property file in "+ propertyFilePath);
			this.managerLocations = new String[]{InetAddress.getLocalHost().getAddress().toString()};
			return;
		} catch (IOException e) {
			logger.error("Error while reading property file in "+ propertyFilePath, e);
		}
		String configArray=defaultProps.getProperty("config.managers");
		this.managerLocations = configArray.split(",");
		if (this.managerLocations.length ==0) {
			this.managerLocations = new String[]{InetAddress.getLocalHost().getAddress().toString()};
			logger.warn("No config manager property file setting to local address");
		}
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO Auto-generated method stub

	}

}
