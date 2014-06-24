package org.roqmessaging.management.zookeeper;

import java.util.List;

import org.apache.curator.test.TestingServer;
import org.apache.log4j.Logger;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class RoQZooKeeperClientTest extends TestCase {
	
	// Variables used in the test suite.
	private static final Logger log = Logger.getLogger(RoQZooKeeperClientTest.class);
	private static RoQZooKeeperClient client;
	private static TestingServer zkServer;
	
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public RoQZooKeeperClientTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSetup(new TestSuite(RoQZooKeeperClientTest.class)) {
	        protected void setUp() throws Exception {
	            log.info("Setting up test suite");
	            
	            log.info("Starting ZooKeeper server");
	            zkServer = new TestingServer();
	            
	            RoQZooKeeperConfig cfg = new RoQZooKeeperConfig();
	    		cfg.servers = zkServer.getConnectString();

	    		log.info("Starting ZooKeeper client");
	    		client = new RoQZooKeeperClient(cfg);
	    		client.start();
	        }
	        protected void tearDown() throws Exception {
	        	log.info("Tearing down test suite");
	            client.close();
	            zkServer.close();
	        }
		};
	}
	
	public void setUp() {
		log.info("test setUp()");
		
		// Make sure ZooKeeper is clean within the namespace we have
		// defined.
		client.clear();
	}
	
	public void tearDown() {
		log.info("test tearDown()");
	}
	
	/**
	 * Actual tests
	 */
	public void testAddRemoveHCM() {
		log.info("");

		List<Metadata.HCM> hcms;
		
		// First, make sure we start with a clean state.
		hcms = client.getHCMList();
		assertEquals(hcms.size(), 0);
		
		// Now try to add a host and check the result.
		Metadata.HCM hcm = new Metadata.HCM("192.168.0.1");
		client.addHCM(hcm);
		hcms = client.getHCMList();
		assertEquals(hcms.size(), 1);
		assertTrue(hcm.equals(hcms.get(0)));
		
		// Make sure the same host cannot be added twice.
		client.addHCM(hcm);
		hcms = client.getHCMList();
		assertEquals(hcms.size(), 1);
		
		// Remove the host and check the result.
		client.removeHCM(hcm);
		hcms = client.getHCMList();
		assertEquals(hcms.size(), 0);
	}
	
	public void testCreateRemoveQueue() {
		log.info("");
		
		Metadata.Queue queue = new Metadata.Queue("Q");
		Metadata.HCM   hcm   = new Metadata.HCM("192.168.0.1");
		Metadata.Monitor monitor = new Metadata.Monitor("tcp://192.168.0.1:1234");
		Metadata.StatMonitor statMonitor = new Metadata.StatMonitor("tcp://192.168.0.1:9876");
		
		// First, make sure we start with a clean state.
		assertFalse(client.queueExists(queue));
		
		// Now try to add a queue and check the result.
		client.createQueue(queue, hcm, monitor, statMonitor);
		assertTrue(client.queueExists(queue));
		assertTrue(hcm.equals(client.getHCM(queue)));
		assertTrue(monitor.equals(client.getMonitor(queue)));
		assertTrue(statMonitor.equals(client.getStatMonitor(queue)));
		
		// Remove the host and check the result.
		client.removeQueue(queue);
		assertFalse(client.queueExists(queue));
	}
	
	public void testGetQueueList() {
		log.info("");
		
		// Define the queue names.
		Metadata.Queue queue1 = new Metadata.Queue("wallace");
		Metadata.Queue queue2 = new Metadata.Queue("gromit");
		
		// For this test, we use the same data for both queues.
		// This should never happen in a real situation.
		Metadata.HCM   hcm   = new Metadata.HCM("192.168.0.1");
		Metadata.Monitor monitor = new Metadata.Monitor("tcp://192.168.0.1:1234");
		Metadata.StatMonitor statMonitor = new Metadata.StatMonitor("tcp://192.168.0.1:9876");
		
		// First, make sure we start with a clean state.
		assertFalse(client.queueExists(queue1));
		assertFalse(client.queueExists(queue2));
		
		// Now add the queues to ZooKeeper.
		client.createQueue(queue1, hcm, monitor, statMonitor);
		client.createQueue(queue2, hcm, monitor, statMonitor);
		
		// Get the list of queues from ZooKeeper.
		List<Metadata.Queue> queues = client.getQueueList();
		assertEquals(queues.size(), 2);
		
		// Check that both queues are received with the call.
		boolean queue1Found = false;
		boolean queue2Found = false;
		for (Metadata.Queue queue : queues) {
			if (queue.equals(queue1)) {
				queue1Found = true;
			}
			if (queue.equals(queue2)) {
				queue2Found = true;
			}
		}
		assertTrue(queue1Found);
		assertTrue(queue2Found);
	}
	
	public void testQueueRunning() {
		log.info("");
		
		Metadata.Queue queue = new Metadata.Queue("Q");
		Metadata.HCM   hcm   = new Metadata.HCM("192.168.0.1");
		Metadata.Monitor monitor = new Metadata.Monitor("tcp://192.168.0.1:1234");
		Metadata.StatMonitor statMonitor = new Metadata.StatMonitor("tcp://192.168.0.1:9876");
		
		// Now try to add a queue and check the result.
		client.createQueue(queue, hcm, monitor, statMonitor);
		
		client.setRunning(queue, true);
		assertTrue(client.isRunning(queue));
		
		client.setRunning(queue, false);
		assertFalse(client.isRunning(queue));
	}
	
	public void testCloudConfig() {
		log.info("");
		
		RoQSerializationUtils utils = new RoQSerializationUtils();
		
		CloudConfig cfg1 = new CloudConfig();
        cfg1.endpoint = "abc";
        cfg1.gateway = "def";
        cfg1.inUse = true;
        cfg1.password = "ghi";
        cfg1.user = "jkl";
        
        // Write to zookeeper
        byte[] out = utils.serialiseObject(cfg1);
        client.setCloudConfig(out);
        
        // modify configuration to test whether it can be overwritten
        cfg1.user = "mno";
        client.setCloudConfig(utils.serialiseObject(cfg1));
        
        // read from zookeeper
        byte[] in = client.getCloudConfig();
        CloudConfig cfg2 = utils.deserializeObject(in);
        
        assertTrue(cfg2.equals(cfg1));
	}
	
	public void testScalingConfig() {
		log.info("");
		
		RoQSerializationUtils utils = new RoQSerializationUtils();
		
		// Create a queue
		Metadata.Queue queue = new Metadata.Queue("Q");
		Metadata.HCM   hcm   = new Metadata.HCM("192.168.0.1");
		Metadata.Monitor monitor = new Metadata.Monitor("tcp://192.168.0.1:1234");
		Metadata.StatMonitor statMonitor = new Metadata.StatMonitor("tcp://192.168.0.1:9876");
		
		client.createQueue(queue, hcm, monitor, statMonitor);
		
		// Create its scaling config
		// Note: queue config is left null on purpose
		// to check whether null values are correctly read and written.
		AutoScalingConfig sc1 = new AutoScalingConfig();
		sc1.hostRule = new HostScalingRule(1, 2);
		sc1.xgRule   = new XchangeScalingRule(3, 4f);
		// sc1.qRule    = new LogicalQScalingRule(5, 6);
		
		// add the config to zookeeper
		client.setHostScalingConfig(utils.serialiseObject(sc1.hostRule), queue);
		client.setExchangeScalingConfig(utils.serialiseObject(sc1.xgRule), queue);
		// client.setQueueScalingConfig(utils.serialiseObject(sc1.qRule), queue);
		
		// modify configuration to test whether it can be overwritten
		sc1.xgRule.Throughput_Limit = 8;
		client.setExchangeScalingConfig(utils.serialiseObject(sc1.xgRule), queue);
		
		// read from zookeeper
		// Note that deserializing the queue scaling config returns null
		// after catching and logging an exception. It's normal.
		AutoScalingConfig sc2 = new AutoScalingConfig();
		sc2.hostRule = utils.deserializeObject(client.getHostScalingConfig(queue));
		sc2.xgRule   = utils.deserializeObject(client.getExchangeScalingConfig(queue));
		log.info("Next call to deserializeObject() is expected to catch an exception and log it.");
		sc2.qRule    = utils.deserializeObject(client.getQueueScalingConfig(queue));
		
		assertTrue(sc2.equals(sc1));
	}
}
