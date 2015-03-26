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
package org.roqmessaging.management.config.internal;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

/**
 * Class FileConfigurationReader
 * <p>
 * Description: Read the configuration from an apache commons configuration
 * 
 * @author sskhiri
 */
public class FileConfigurationReader {
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

	/**
	 * @param file
	 *            the GCM property file
	 * @return the GCM dao for properties
	 * @throws ConfigurationException
	 */
	public GCMPropertyDAO loadGCMConfiguration(String file) throws ConfigurationException {
		// 1. Define the DAO
		GCMPropertyDAO configDao = new GCMPropertyDAO();
		// 2. Load the file
		try {
			PropertiesConfiguration config = new PropertiesConfiguration();
			config.load(file);
			// 3. Set the properties
			configDao.setPeriod(config.getInt("period"));
			configDao.sethcmTIMEOUT(config.getInt("hcmTIMEOUT"));
			configDao.setFormatDB(config.getBoolean("formatDB"));
			configDao.hasCloudConfiguration(config.getBoolean("hasCloudConfiguration"));
			// initialize the ports used by the GCM
			configDao.ports.setBasePort(config.getInt("ports.base"));
			// intialize the zookeeper configuration
			configDao.zkConfig.servers = config.getString("zk.servers");
		} catch (Exception configE) {
			logger.error("Error while reading configuration file - skipped but set the default configuration", configE);
		}
		
		return configDao;
	}
	
	/**
	 * @param file the file which contains the cloud configuration
	 * @return an instance of CloudConfig
	 * @throws ConfigurationException
	 */
	public CloudConfig loadCloudConfiguration(String file) throws ConfigurationException {
		
		CloudConfig cloudConfig = new CloudConfig();
		
		try {
			PropertiesConfiguration config = new PropertiesConfiguration();
			config.load(file);

			cloudConfig.inUse = config.getBoolean("cloud.use");
			if (cloudConfig.inUse) {
				cloudConfig.endpoint = config.getString("cloud.endpoint");
				cloudConfig.user = config.getString("cloud.user");
				cloudConfig.password = config.getString("cloud.password");
				cloudConfig.gateway = config.getString("cloud.gateway");
			}
		} catch (Exception configE) {
			logger.error("Error while reading configuration file - skipped but set the default configuration", configE);
		}
		
		return cloudConfig;
	}
	/**
	 * @param file
	 *            the HCM property file
	 * @return the HCM dao for properties
	 * @throws ConfigurationException
	 */
	public HostConfigDAO loadHCMConfiguration(String file) throws ConfigurationException {
		// 1. Define the DAO
		HostConfigDAO configDao = new HostConfigDAO();
		// 2. Load the file
		try {
			PropertiesConfiguration config = new PropertiesConfiguration();
			config.load(file);
			// 3. Set the properties
			configDao.setGcmAddress(config.getString("gcm.address")!=null?config.getString("gcm.address"):configDao.getGcmAddress());
			configDao.ports.setBasePort(config.getInt("gcm.ports.base"));
			
			configDao.setExchangeFrontEndPort(config.getInt("exchange.base.port"));
			configDao.setMonitorBasePort(config.getInt("monitor.base.port"));
			configDao.setStatMonitorBasePort(config.getInt("statmonitor.base.port"));
			configDao.setStatPeriod(config.getInt("monitor.stat.period"));
			configDao.setMaxNumberEchanges(config.getInt("exchange.max.perhost"));
			configDao.setQueueInHcmVm(config.getBoolean("queue.hcm.vm"));
			configDao.setExchangeInHcmVm(config.getBoolean("exchange.hcm.vm"));
			configDao.setLocalStatePath(config.getString("localstate.path"));
			configDao.setMonitorTimeOut(config.getInt("monitor.timeout"));
			configDao.setMonitorMaxTimeToStart(config.getInt("monitor.maxtimetostart"));
			configDao.setExchangeHeap(config.getInt("exchange.vm.heap")!=-1?config.getInt("exchange.vm.heap"):256);
			if (config.containsKey("network.interface"))
				configDao.setNetworkInterface(config.getString("network.interface"));
		} catch (Exception configE) {
			logger.error("Error while reading configuration file - skipped but set the default configuration", configE);
		}
		return configDao;
	}

}
