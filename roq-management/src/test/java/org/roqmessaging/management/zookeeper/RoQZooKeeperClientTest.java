package org.roqmessaging.management.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.curator.test.TestingServer;
import org.apache.log4j.Logger;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.management.config.internal.CloudConfig;
import org.roqmessaging.management.config.scaling.AutoScalingConfig;
import org.roqmessaging.management.config.scaling.HostScalingRule;
import org.roqmessaging.management.config.scaling.LogicalQScalingRule;
import org.roqmessaging.management.config.scaling.XchangeScalingRule;
import org.roqmessaging.zookeeper.Metadata;

import junit.framework.TestCase;

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
	
	public void setUp() {
		log.info("test setUp()");        
        log.info("Starting ZooKeeper server");
        try {
			zkServer = new TestingServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        RoQZooKeeperConfig cfg = new RoQZooKeeperConfig();
		cfg.servers = zkServer.getConnectString();

		log.info("Starting ZooKeeper client");
		client = new RoQZooKeeperClient(cfg);
		client.start();
		client.initZkClusterNodes();
	}
	
	public void tearDown() {
		log.info("test tearDown()");       
        try {
        	client.closeGCM();
			zkServer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testCreateRemoveQueue() {
		log.info("");
		
		Metadata.Queue queue = new Metadata.Queue("Q");
		Metadata.HCM   hcm   = new Metadata.HCM("192.168.0.1");
		Metadata.Monitor monitor = new Metadata.Monitor("tcp://192.168.0.1:1234");
		Metadata.StatMonitor statMonitor = new Metadata.StatMonitor("tcp://192.168.0.1:9876");
		Metadata.Monitor monitor2 = new Metadata.Monitor("tcp://192.128.0.1:956");
		ArrayList<Metadata.BackupMonitor> monitorsList = new ArrayList<Metadata.BackupMonitor>();
		ArrayList<Metadata.HCM> monitorsHostList = new ArrayList<Metadata.HCM>();
		monitorsList.add(new Metadata.BackupMonitor(hcm.address + "," + monitor2.address));
		monitorsHostList.add(hcm);
		// First, make sure we start with a clean state.
		assertFalse(client.queueExists(queue));
		
		// Now try to add a queue and check the result.
		client.createQueue(queue, hcm, monitor, statMonitor, monitorsList);
		assertTrue(client.queueExists(queue));
		assertTrue(hcm.equals(client.getHCM(queue)));
		assertTrue(monitor.equals(client.getMonitor(queue)));
		assertTrue(statMonitor.equals(client.getStatMonitor(queue)));
		ArrayList<Metadata.BackupMonitor> testList = client.getBackUpMonitors(queue);
		assertTrue(testList.contains(new Metadata.BackupMonitor(hcm.address + "," + monitor2.address)));
		// Remove the host and check the result.
		client.removeQueue(queue);
		assertFalse(client.queueExists(queue));
	}
	
	public void testHCMState() {
		log.info("");
		Metadata.HCM   hcm   = new Metadata.HCM("192.168.0.1");
		
		client.addHCMState(hcm);
		client.addHCMBUMonitor(hcm, "queue1");
		client.addHCMBUMonitor(hcm, "queue2");
		client.addHCMMonitor(hcm, "queue3");
		
		assertTrue(client.getHCMBUMonitors(hcm).contains("queue1"));
		assertTrue(client.getHCMBUMonitors(hcm).contains("queue2"));
		assertTrue(client.getHCMMonitors(hcm).contains("queue3"));
		
		client.removeHCMState(hcm);
	}
	
	public void testCreateRemoveExchangeTransaction() {
		log.info("");
		String host = "192.168.0.1";
		
		client.createExchangeTransaction("Test", host);
		
		assertEquals(host, client.exchangeTransactionExists("Test"));
		
		client.removeExchangeTransaction("Test");
		
		assertEquals(null, client.exchangeTransactionExists("Test"));
		
	}
		
	public void testCreateRemoveQueueTransaction() {
		log.info("");
		String host = "192.168.0.1";
		
		client.createQTransaction("TestQ", host, new ArrayList<String>());
		
		assertEquals(host, client.qTransactionExists("TestQ"));
		
		client.removeQTransaction("TestQ");
		
		assertEquals(null, client.qTransactionExists("TestQ"));
		
	}
	
	
	public void testRemoveHCMTransaction() {
		log.info("");
		String host = "lolo://192.168.0.1:8000";
		
		client.createHcmRemoveTransaction(new Metadata.HCM(host));
		
		assertEquals(host, client.hcmRemoveTransactionExists(new Metadata.HCM(host)));
		
		try {
			client.removeHcmRemoveTransaction(new Metadata.HCM(host));
		} catch (Exception e) {
			assertEquals(false, true);
			e.printStackTrace();
		}
		
		assertEquals(null, client.hcmRemoveTransactionExists(new Metadata.HCM(host)));
		
	}
	
	public void testGetQueueList() {
		log.info("");
		try {		
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
			client.createQueue(queue1, hcm, monitor, statMonitor, new ArrayList<Metadata.BackupMonitor>());
			client.createQueue(queue2, hcm, monitor, statMonitor, new ArrayList<Metadata.BackupMonitor>());
			
			// Get the list of queues from ZooKeeper.
			List<Metadata.Queue> queues = client.getQueueList();
			assertEquals(2, queues.size());
			
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testQueueRunning() {
		log.info("");
		
		Metadata.Queue queue = new Metadata.Queue("Q");
		Metadata.HCM   hcm   = new Metadata.HCM("192.168.0.1");
		Metadata.Monitor monitor = new Metadata.Monitor("tcp://192.168.0.1:1234");
		Metadata.StatMonitor statMonitor = new Metadata.StatMonitor("tcp://192.168.0.1:9876");
		
		client.createQueue(queue, hcm, monitor, statMonitor, new ArrayList<Metadata.BackupMonitor>());
		
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
		
		client.createQueue(queue, hcm, monitor, statMonitor, new ArrayList<Metadata.BackupMonitor>());
		
		// Create its scaling config
		// Note: queue config is left null on purpose
		// to check whether null values are correctly read and written.
		AutoScalingConfig sc1 = new AutoScalingConfig();
		sc1.hostRule = new HostScalingRule(1, 2);
		sc1.xgRule   = new XchangeScalingRule(3, 4f);
		sc1.qRule    = new LogicalQScalingRule(5, 6);
		
		// add the config to zookeeper
		client.setHostScalingConfig(utils.serialiseObject(sc1.hostRule), queue);
		client.setExchangeScalingConfig(utils.serialiseObject(sc1.xgRule), queue);
		client.setQueueScalingConfig(utils.serialiseObject(sc1.qRule), queue);
		
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
	
	/**
	 * Actual tests
	 */
	public void testAddRemoveHCM() {
		log.info("");
		try {
			client.startServiceDiscovery();
			List<Metadata.HCM> hcms;
			
			// First, make sure we start with a clean state.
			hcms = client.getHCMList();
			assertEquals(hcms.size(), 0);
			
			// Now try to add a host and check the result.
			Metadata.HCM hcm = new Metadata.HCM("192.168.0.1");
			client.registerHCM(hcm);
			Thread.sleep(6000); // wait for the cache updates
			hcms = client.getHCMList();
			assertEquals(1, hcms.size());
			assertTrue(hcm.equals(hcms.get(0)));
			
			// Make sure the same host cannot be added twice.
			client.registerHCM(hcm);
			Thread.sleep(6000);
			hcms = client.getHCMList();
			assertEquals(1, hcms.size());
			
			// Remove the host and check the result.
			client.removeHCM(hcm);
			Thread.sleep(3000);
			hcms = client.getHCMList();
			assertEquals(0, hcms.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
