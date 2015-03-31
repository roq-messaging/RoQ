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
package org.roqmessaging.management.properties;

import junit.framework.Assert;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.internal.FileConfigurationReader;
import org.roqmessaging.management.config.internal.GCMPropertyDAO;
import org.roqmessaging.management.config.internal.HostConfigDAO;

/**
 * Class TestCommonsApacheConfiguration
 * <p> Description: Test the apache configuration framework
 * 
 * @author sskhiri
 */
public class TestCommonsApacheConfiguration {
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

	@Test
	public void test() {
		try {
			PropertiesConfiguration config = new PropertiesConfiguration();
			config.load("GCM.properties");
			int period = config.getInt("period");
			logger.info("Period = "+ period);
			Assert.assertEquals(60000, period);
			
			boolean formatDB = config.getBoolean("formatDB");
			logger.info("format DB = "+ formatDB);
			Assert.assertEquals(false, formatDB);
		} catch (ConfigurationException e) {
			logger.error(e);
		}
	}
	
	@Test
	public void testReader() {
		try {
		FileConfigurationReader reader = new FileConfigurationReader();
		GCMPropertyDAO dao = reader.loadGCMConfiguration("GCM-test.properties");
			logger.info("Period = "+ dao.getPeriod());
			Assert.assertEquals(60000, dao.getPeriod());
			
			logger.info("format DB = "+ dao.isFormatDB());
			Assert.assertEquals(false, dao.isFormatDB());
			
			logger.info("GCM base port  = "+ dao.ports.get("GlobalConfigurationManager.interface"));
			Assert.assertEquals(5000, dao.ports.get("GlobalConfigurationManager.interface"));
			
			
			CloudConfig cloudConfig = reader.loadCloudConfiguration("GCM-test.properties");
			logger.info("use cloud  = "+ cloudConfig.inUse);
			Assert.assertEquals(true, cloudConfig.inUse);
			
			logger.info("user cloud  = "+ cloudConfig.user);
			Assert.assertEquals("sabri", cloudConfig.user);
			
			logger.info("user Passwd  = "+ cloudConfig.password);
			Assert.assertEquals("sabsab", cloudConfig.password);
			
			logger.info("end point cloud  = "+ cloudConfig.endpoint);
			Assert.assertEquals("http://inferno.local:2633/RPC2", cloudConfig.endpoint);
			
			
			HostConfigDAO hostDao = reader.loadHCMConfiguration("HCM.properties");
			logger.info("zk.address = "+ hostDao.getZkAddress());
			Assert.assertEquals("localhost", hostDao.getZkAddress());
			Assert.assertEquals(5800, hostDao.getStatMonitorBasePort());
			Assert.assertEquals(5500, hostDao.getMonitorBasePort());
			Assert.assertEquals(6000, hostDao.getExchangeFrontEndPort());
			logger.info(hostDao.toString());
		} catch (ConfigurationException e) {
			logger.error(e);
		}
	}

}
