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
			configDao.setFormatDB(config.getBoolean("formatDB"));
			configDao.setUseCloud(config.getBoolean("use.cloud"));
			if(configDao.isUseCloud()){
				configDao.setCloudEndPoint(config.getString("cloud.endpoint"));
				configDao.setCloudUser(config.getString("cloud.user"));
				configDao.setCloudPasswd(config.getString("cloud.password"));
			}
		} catch (Exception configE) {
			logger.error("Error while reading configuration file - skipped but set the default configuration", configE);
		}
		
		return configDao;
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
			configDao.setGcmAddress(config.getString("gcm.address"));
			configDao.setExchangeFrontEndPort(config.getInt("exchange.base.port"));
			configDao.setMonitorBasePort(config.getInt("monitor.base.port"));
			configDao.setStatMonitorBasePort(config.getInt("statmonitor.base.port"));
			configDao.setStatPeriod(config.getInt("monitor.stat.period"));
			configDao.setMaxNumberEchanges(config.getInt("exchange.max.perhost"));

			if (config.containsKey("network.interface"))
				configDao.setNetworkInterface(config.getString("network.interface"));
		} catch (Exception configE) {
			logger.error("Error while reading configuration file - skipped but set the default configuration", configE);
		}
		return configDao;
	}

}
