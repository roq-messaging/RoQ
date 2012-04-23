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
package org.roq.simulation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roq.simulation.stat.KPISubscriber;
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.management.LogicalQFactory;

/**
 * Class TestStatMonitor
 * <p> Description: Test the Statistic monitor behavior
 * 
 * @author sskhiri
 */
public class TestStatMonitor {
	private RoQAllLocalLauncher launcher = null;
	private 	KPISubscriber kpiSubscriber = null;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.launcher = new RoQAllLocalLauncher();
		this.launcher.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.launcher.tearDown();
		this.kpiSubscriber.shutDown();
	}

	@Test
	public void test() {
		//1. Create a Queue
		IRoQLogicalQueueFactory logicalQFactory = new LogicalQFactory(launcher.getConfigurationServer());
		logicalQFactory.createQueue("queue1", RoQUtils.getInstance().getLocalIP());
		
		//2. Init the KPI subscriber
		kpiSubscriber = new KPISubscriber(launcher.getConfigurationServer(), "queue1", false);
		new Thread(kpiSubscriber).start();
		
		//3 Wait &. Check the content
		try {
			Thread.sleep(30000);
			
			//Delete the queue
			logicalQFactory.removeQueue("queue1");
			Thread.sleep(2000);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
