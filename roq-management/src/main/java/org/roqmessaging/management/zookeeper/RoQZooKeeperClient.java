package org.roqmessaging.management.zookeeper;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;


import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.log4j.Logger;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.zookeeper.Metadata;
import org.roqmessaging.zookeeper.Metadata.BackupMonitor;
import org.roqmessaging.zookeeper.Metadata.Monitor;
import org.roqmessaging.zookeeper.Metadata.Queue;
import org.roqmessaging.zookeeper.Metadata.StatMonitor;
import org.roqmessaging.zookeeper.RoQZKHelpers;
import org.roqmessaging.zookeeper.RoQZooKeeper;
import org.roqmessaging.zookeeper.Metadata.HCM;
import org.roqmessaging.management.GlobalConfigLeaderListener;
import org.roqmessaging.management.zookeeper.RoQZooKeeperConfig;

/**
 * TODO Split the class in two classes:
 * GCMClient
 * HCMClient
 */
public class RoQZooKeeperClient extends RoQZooKeeper {
	private final Logger log = Logger.getLogger(getClass());
	private LeaderLatch leaderLatch;
	private RoQZooKeeperConfig cfg;
	
	private ServiceDiscovery<HostDetails> serviceDiscovery = null;
	
	private ServiceInstance<HostDetails> instance;
	
	private ServiceCache<HostDetails> cache;
	
	public RoQZooKeeperClient(RoQZooKeeperConfig config) {
		super(config);
		log.info("");
		
		cfg = config;	
	}
	
	/**
	 * Called by The GCM when it shutdown
	 * @throws IOException
	 */
	public void closeGCM() throws IOException {
		if (cache != null)
			cache.close();
		closeServiceDiscovery();
		super.close();
	}
	
	/**
	 * Close only the hcm discovery service
	 * @throws IOException
	 */
	public void closeServiceDiscovery() throws IOException {
		if (serviceDiscovery != null)
			serviceDiscovery.close();
	}
	
	/**
	 * This method is used to block the process
	 * until it becomes the leader.
	 * @throws InterruptedException 
	 * @throws EOFException 
	 */
	public void startLeaderElection() {
		leaderLatch = new LeaderLatch(client, cfg.znode_gcm);
		try {
			leaderLatch.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start the hcm service discovery
	 * That will create a cache that contain the list of hcms 
	 * the cache is handled by a listener when a modification occurs
	 * A modification could indicate a new hcm, a removed hcm, or a crashed hcm
	 * @throws Exception
	 */
	public void startServiceDiscovery() throws Exception {
		String path = RoQZKHelpers.makePath(cfg.znode_hcm);
		JsonInstanceSerializer<HostDetails> serializer = 
				new JsonInstanceSerializer<HostDetails>(HostDetails.class);
		serviceDiscovery = ServiceDiscoveryBuilder.builder(HostDetails.class)
				.client(client).basePath(path).serializer(serializer).build();
		serviceDiscovery.start();
		cache = serviceDiscovery.serviceCacheBuilder().name("HCM").build();
		cache.addListener(new HcmListener(this));
		cache.start();
	}
	
	/**
	 * Create the zookeeper path tree for RoQ
	 */
	public void initZkClusterNodes() {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_queues);
		RoQZKHelpers.createZNodeAndParents(client, path);
		path = RoQZKHelpers.makePath(cfg.znode_queue_transactions);
		RoQZKHelpers.createZNodeAndParents(client, path);
		path = RoQZKHelpers.makePath(cfg.znode_exchange_transactions);
		RoQZKHelpers.createZNodeAndParents(client, path);
		path = RoQZKHelpers.makePath(cfg.znode_hcm_remove_transactions);
		RoQZKHelpers.createZNodeAndParents(client, path);
		path = RoQZKHelpers.makePath(cfg.znode_hcm_state);
		RoQZKHelpers.createZNodeAndParents(client, path);
		path = RoQZKHelpers.makePath(cfg.znode_queues_exchanges);
		RoQZKHelpers.createZNodeAndParents(client, path);
	}
	
	/**
	 * This method is used to block the process
	 * until it becomes the leader.
	 * @throws InterruptedException 
	 * @throws EOFException 
	 */
	public void waitUntilLeader() throws EOFException, InterruptedException {
		leaderLatch.await();
		// This listener allow to manage the case
		// where the connection with zookeeper is lost
		// ie: the process must stop to process messages
		leaderLatch.addListener(new GlobalConfigLeaderListener());
	}	

	/**
	 * This method return true if this instance of the zk client
	 * has the leadership
	 * @return true if it is the leader
	 */
	public boolean isLeader() {
		log.info("");
		return leaderLatch.hasLeadership();
	}
	
	/**
	 * Remove all the nodes in RoQ/
	 */
	public void clear() {
		log.info("");
		
		List<String> rootNodes = RoQZKHelpers.getChildren(client, "");
		
		// If something went wrong, abort.
		if (rootNodes == null) {
			log.debug("Helpers.getChildren() failed. Aborting clean().");
		}
		
		for (String node : rootNodes) {
			// Do not delete the gcm node because it is used for leader election
			// and it is handled by leaderLatch.
			if (!node.equals(cfg.znode_gcm)) {
				RoQZKHelpers.deleteZNodeAndChildren(client, "/"+node);
				log.info("--root node " +node +" deleted");
			}
		}
	}
		
	// Create the znode for the HCM if it does not already exist.
	public void setGCMLeader() {
		RoQUtils utils = RoQUtils.getInstance();
		String path = RoQZKHelpers.makePath(cfg.znode_leaderAddress);
		String address = utils.getLocalIP();
		log.info("Set GCM address: " + address + " in ZK path: " + path);
		//RoQZKHelpers.deleteZNode(client, path);
		RoQZKHelpers.createEphemeralZNode(client, path, address.getBytes());
	}

	/**
	 * Get the list of the HCM registered on the cluster
	 * @return a List of HCM addresses
	 * @throws Exception 
	 */
	public List<Metadata.HCM> getHCMList() throws Exception {
		log.info("");
		
		List<Metadata.HCM> hcms = new ArrayList<Metadata.HCM>();
		Collection<ServiceInstance<HostDetails>> instances = cache.getInstances();
		
		for (ServiceInstance<HostDetails> instance : instances) {
			log.info("host address: " + instance.getAddress());
			hcms.add(new Metadata.HCM(instance.getPayload().getAddress()));
		}

		return hcms;
	}
	
	/**
	 * This method remove explicitly a hcm Znode and its watcher
	 * There is actually two ways to lost a hcm znode
	 * by removing the node explicitly (when we use this) method.
	 * Because the hcm has crashed, in this case the watcher detects the node lost
	 * and the GCM triggers a recovery process
	 * @param hcm
	 * @throws Exception 
	 */
	public void removeHCM(Metadata.HCM hcm) throws Exception {
		log.info("");
		if (instance == null)
			throw new IllegalStateException();
		// RoQZKHelpers.deleteZNode(client, getZKPath(hcm));
		serviceDiscovery.unregisterService(instance);
		serviceDiscovery.close();
	}
	
	/**
	 * This method registers an ephemeral Znode
	 * in zookeeper. This Znode will be watched by 
	 * the GCM, in order to handle HCM crash
	 * @param hcm
	 * @throws Exception 
	 */
	public void registerHCM(HCM hcm) throws Exception {
		// Check whether the HCM is not register
		log.info("Registering hcm with address: " + hcm.address);
		instance = ServiceInstance.<HostDetails>builder()
				.name("HCM").id(hcm.zkNodeString()).payload(new HostDetails(hcm.address)).build();
		String path = RoQZKHelpers.makePath(cfg.znode_hcm);
		JsonInstanceSerializer<HostDetails> serializer = 
				new JsonInstanceSerializer<HostDetails>(HostDetails.class);
		serviceDiscovery = ServiceDiscoveryBuilder.builder(HostDetails.class)
				.client(client).basePath(path).serializer(serializer).thisInstance(instance).build();
		addHCMState(hcm);
		serviceDiscovery.start();
	}
	
	/**
	 * create the parent nodes for a gcm
	 * these nodes contain the state of the GCM
	 * @param hcm
	 */
	public void addHCMState(HCM hcm) {
		log.info("" + hcm.address);
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString());
		RoQZKHelpers.createZNode(client, path);
		path = RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString(), "backupMonitors");
		RoQZKHelpers.createZNode(client, path);
		path = RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString(), "masterMonitors");
		RoQZKHelpers.createZNode(client, path);
	}
	
	/**
	 * Add a Monitor in the hcmState
	 * @param hcm
	 * @param queueName
	 */
	public void addHCMMonitor(HCM hcm, String queueName) {
		log.info("" + hcm.address);
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString(), 
				"masterMonitors", queueName);
		RoQZKHelpers.createZNode(client, path);
	}

	/**
	 * Add a backup monitor in the hcmState
	 * @param hcm
	 * @param queueName
	 */
	public void addHCMBUMonitor(HCM hcm, String queueName) {
		log.info("" + hcm.address);
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString(), 
				"backupMonitors", queueName);
		log.info("" + path);
		RoQZKHelpers.createZNode(client, path);		
	}
	
	/**
	 * Remove a Monitor in the hcmState
	 * @param hcm
	 * @param queueName
	 */
	public void removeHCMMonitor(HCM hcm, String queueName) {
		log.info("" + hcm.address);
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString(), 
				"masterMonitors", queueName);
		RoQZKHelpers.deleteZNode(client, path);
	}

	/**
	 * Remove a backup monitor in the hcmState
	 * @param hcm
	 * @param queueName
	 */
	public void removeHCMBUMonitor(HCM hcm, String queueName) {
		log.info("" + hcm.address);
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_state, 
				hcm.zkNodeString(), "backupMonitors", queueName);
		RoQZKHelpers.deleteZNode(client, path);		
	}
	
	/**
	 * Get the list of monitors handled by an HCM
	 * @param hcm
	 * @return
	 */
	public List<String> getHCMMonitors(HCM hcm) {
		log.info("" + hcm.address);
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_state, 
				hcm.zkNodeString(), "masterMonitors");
		return RoQZKHelpers.getChildren(client, path);
	}

	/**
	 * Get the list of backup monitor handled by an HCM
	 * @param hcm
	 * @return
	 */
	public List<String> getHCMBUMonitors(HCM hcm) {
		log.info("" + hcm.address);
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_state, 
				hcm.zkNodeString(), "backupMonitors");
		return RoQZKHelpers.getChildren(client, path);	
	}
	
	/**
	 * Remove a HCMState from Zookeeper
	 * Called when the HCM is lost or stopped
	 * @param hcm
	 */
	public void removeHCMState(HCM hcm) {
		log.info("" + hcm.address);
		RoQZKHelpers.deleteZNodeAndChildren(client, 
				RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString()));
	}
	
	/**
	 * Check whether a logical queue
	 * exists in RoQ.
	 * @param queue
	 * @return
	 */
	public boolean queueExists(Metadata.Queue queue) {
		log.info("");
		return RoQZKHelpers.zNodeExists(client, getZKPath(queue));
	}
	
	
	/**
	 * This method create a transaction for the queue creation in ZK
	 * @param queueName
	 */
	public void createHcmRemoveTransaction (Metadata.HCM hcm) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_remove_transactions, hcm.zkNodeString());
		RoQZKHelpers.createZNode(client, path, hcm.address);
	}
	
	/**
	 * This method fetch if a transaction exists for the queue
	 * @param queueName
	 */
	public String hcmRemoveTransactionExists (Metadata.HCM  hcm) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_remove_transactions, hcm.zkNodeString());
		return RoQZKHelpers.getDataString(client, path);
	}
	
	
	
	/**
	 * This method remove the transaction node when the transaction has been completed
	 * @param queueName
	 */
	public void removeHcmRemoveTransaction (Metadata.HCM hcm) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_hcm_remove_transactions, hcm.zkNodeString());
		RoQZKHelpers.deleteZNode(client, path);
	}
	
	/**
	 * This method create a transaction for the queue creation in ZK
	 * @param queueName
	 */
	public void createQTransaction (String queueName, String host, ArrayList<String> backupHosts) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_queue_transactions, queueName);
		String payload = host;
		for (int i = 0; i < backupHosts.size(); i++) {
			payload += "," + backupHosts.get(i);
		}
		
		RoQZKHelpers.createZNode(client, path, payload);
	}
	
	/**
	 * Remove the node that keep queue's exchanges list
	 * @param monitor
	 */
	public void removeQueueExchanges(Metadata.Queue queue) {
		String path = RoQZKHelpers.makePath(cfg.znode_queues_exchanges, queue.name);
		RoQZKHelpers.deleteZNodeAndChildren(client, path);
	}
	
	/**
	 * This method fetch if a transaction exists for the queue
	 * @param queueName
	 */
	public String qTransactionExists (String queueName) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_queue_transactions, queueName);
		return RoQZKHelpers.getDataString(client, path);
	}
	
	/**
	 * This method remove the transaction node when the transaction has been completed
	 * @param queueName
	 */
	public void removeQTransaction (String queueName) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_queue_transactions, queueName);
		RoQZKHelpers.deleteZNode(client, path);
	}
	
	/**
	 * This method create the transaction node for an exchange creation process
	 * @param transID
	 */
	public void createExchangeTransaction (String transID, String targetHost) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_exchange_transactions, transID);
		RoQZKHelpers.createZNode(client, path, targetHost);
	}
	
	/**
	  * This method fetch if a transaction exists for the exchange creation
	 * @param transID
	 */
	public String exchangeTransactionExists (String transID) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_exchange_transactions, transID);
		return RoQZKHelpers.getDataString(client, path);
	}
	
	/**
	 * This method remove the transaction node when the transaction has been completed
	 * @param transID
	 */
	public void removeExchangeTransaction (String transID) {
		log.info("");
		String path = RoQZKHelpers.makePath(cfg.znode_exchange_transactions, transID);
		RoQZKHelpers.deleteZNode(client, path);
	}
	
	/**
	 * Add a queue in ZK and the state for that queue
	 * @param queue
	 * @param hcm
	 * @param monitor
	 * @param statMonitor
	 * @param monitorBU
	 */
	public void createQueue(Metadata.Queue queue, Metadata.HCM hcm, 
			Metadata.Monitor monitor, Metadata.StatMonitor statMonitor, 
			List<Metadata.BackupMonitor> monitorBU) {
		log.info("");
		
		String queuePath = getZKPath(queue);
		String monitorPath = RoQZKHelpers.makePath(queuePath, "monitor");
		String statMonitorPath = RoQZKHelpers.makePath(queuePath, "stat-monitor");
		String hcmPath = RoQZKHelpers.makePath(queuePath, "hcm");
		String scalingPath = RoQZKHelpers.makePath(queuePath, "scaling");
		String backupMonitorsPath = RoQZKHelpers.makePath(queuePath, "monitorBU");
		
		String pathHCMMonitorToAdd = RoQZKHelpers.makePath(cfg.znode_hcm_state,
				hcm.zkNodeString(), "masterMonitors", queue.name);
		
		ArrayList<String> pathsHCMBuMonitorToAdd = new ArrayList<String>();
		for (BackupMonitor backup : monitorBU) {
			pathsHCMBuMonitorToAdd.add(RoQZKHelpers.makePath(cfg.znode_hcm_state, 
					new HCM(backup.hcmAddress).zkNodeString(), "backupMonitors", queue.name));
		}
		// Add queue children nodes inside a single transaction
		RoQZKHelpers.createQueueZNodes(client, queuePath, monitorPath, 
				monitor.address, statMonitorPath, statMonitor.address, 
				hcmPath, hcm.address, scalingPath, 
				backupMonitorsPath, monitorBU, pathHCMMonitorToAdd, pathsHCMBuMonitorToAdd);
	}
	
	/**
	 * set a backup monitor as an active monitor
	 * @param queue on which we operate
	 * @param hcm address of the new monitor
	 * @param monitor address
	 * @param statMonitor address
	 */
	public void replaceMonitor(Queue queue, HCM hcm, Monitor monitor, StatMonitor statMonitor) {
		String queuePath = getZKPath(queue);
		// Modifications to bring to HCM State
		String HCMStateAdd = RoQZKHelpers.makePath(cfg.znode_hcm_state, 
				hcm.zkNodeString(), "masterMonitors", queue.name);
		String HCMStateRemove = RoQZKHelpers.makePath(cfg.znode_hcm_state, 
				hcm.zkNodeString(), "backupMonitors", queue.name);
		
		// Modifications to bring to the queue
		String pathQBUMonitorToRemove = RoQZKHelpers.makePath(queuePath, "monitorBU", hcm.address);
		String hcmPath = RoQZKHelpers.makePath(queuePath, "hcm");
		String statMonitorPath = RoQZKHelpers.makePath(queuePath, "stat-monitor");
		String monitorPath = RoQZKHelpers.makePath(queuePath, "monitor");
		String scalingPath = RoQZKHelpers.makePath(queuePath, "scaling");
		
		// process the request inside a transaction
		RoQZKHelpers.replaceMonitorZNodes(client, HCMStateAdd, HCMStateRemove, pathQBUMonitorToRemove, 
				hcmPath, hcm.address, statMonitorPath, statMonitor, monitorPath, monitor, scalingPath);
	}

	/**
	 * add a backup monitor for a given queue
	 * @param queue on which we operate
	 * @param hcm address of the new backupMonitor
	 * @param newBUMonitor
	 */
	public void addBackupMonitor(Queue queue, HCM hcm, BackupMonitor newBUMonitor) {
		String queuePath = getZKPath(queue);
		// Modifications to bring to HCM State
		String HCMStateAdd = RoQZKHelpers.makePath(cfg.znode_hcm_state, hcm.zkNodeString(), 
				"backupMonitors", queue.name);
		
		// Modifications to bring to the queue
		String backupMonitorsPath = RoQZKHelpers.makePath(queuePath, "monitorBU", newBUMonitor.hcmAddress);
		
		// process the request inside a transaction
		RoQZKHelpers.addBackupMonitorZNodes(client, HCMStateAdd, backupMonitorsPath, newBUMonitor);		
	}

	/**
	 * Replace a dead backup monitor by a new one
	 * @param queue on which we operate
	 * @param hcmToRemove the lost monitor hcm address
	 * @param newBUMonitor 
	 */
	public void replaceBackupMonitor(Queue queue, HCM hcmToRemove, BackupMonitor newBUMonitor) {
		String queuePath = getZKPath(queue);
		// Modifications to bring to HCM State
		String HCMStateAdd = RoQZKHelpers.makePath(cfg.znode_hcm_state, 
				new HCM(newBUMonitor.hcmAddress).zkNodeString(), "backupMonitors", queue.name);
		String HCMStateRemove = RoQZKHelpers.makePath(cfg.znode_hcm_state, 
				hcmToRemove.zkNodeString(), "backupMonitors", queue.name);
		
		// Modifications to bring to the queue
		String pathQBUMonitorToRemove = RoQZKHelpers.makePath(queuePath, "monitorBU", hcmToRemove.address);
		String backupMonitorsPath = RoQZKHelpers.makePath(queuePath, "monitorBU", newBUMonitor.hcmAddress);
		
		// process the request inside a transaction
		RoQZKHelpers.replaceBackupMonitorZNodes(client, HCMStateAdd, HCMStateRemove, 
				pathQBUMonitorToRemove, backupMonitorsPath, newBUMonitor);
	}
	
	public void removeQueue(Metadata.Queue queue) {
		log.info("");
		List<Metadata.BackupMonitor> buMonitors = this.getBackUpMonitors(queue);
		Metadata.HCM hcm = this.getHCM(queue);
		
		this.removeHCMMonitor(hcm, queue.name);
		
		for (BackupMonitor bu : buMonitors) {
			this.removeHCMBUMonitor(new HCM(bu.hcmAddress), queue.name);
		}
		
		RoQZKHelpers.deleteZNodeAndChildren(client, getZKPath(queue));
	}
	
	/**
	 * @param queue
	 * @param flag  if true, the queue is marked as running,
	 *              otherwise it is marked as stopped
	 */
	public void setRunning(Metadata.Queue queue, boolean flag) {
		log.info("");
		if (flag) {
			// create a "running" node for the selected queue
			RoQZKHelpers.createZNode(client, RoQZKHelpers.makePath(getZKPath(queue), "running"));
		} else {
			// delete the "running" node for the selected queue
			// to mark it as stopped
			RoQZKHelpers.deleteZNode(client, RoQZKHelpers.makePath(getZKPath(queue), "running"));
		}
	}

	/**
	 * @param queue
	 * @return true if the queue is marked as running, false otherwise
	 */
	public boolean isRunning(Metadata.Queue queue) {
		log.info("");
		return RoQZKHelpers.zNodeExists(client, RoQZKHelpers.makePath(getZKPath(queue), "running"));
	}
	
	public List<Metadata.Queue> getQueueList() {
		log.info("");
		
		List<Metadata.Queue> queues = new ArrayList<Metadata.Queue>();
		List<String> znodes = RoQZKHelpers.getChildren(client, cfg.znode_queues);
		
		// If something goes wrong, return an empty list.
		if (znodes == null) {
			log.debug("Helpers.getChildren() failed. Aborting getQueueList().");
			return queues;
		}
		
		for (String node : znodes) {
			queues.add(new Metadata.Queue(node));
		}
		return queues;
	}
	
	public Metadata.HCM getHCM(Metadata.Queue queue) {
		log.info("");
		String path = RoQZKHelpers.makePath(getZKPath(queue), "hcm");
		String data = RoQZKHelpers.getDataString(client, path);
		log.info("HCM address: " + data);
		if (data == null) {
			return null;
		}
		return new Metadata.HCM(data);
	}
	
	public Metadata.Monitor getMonitor(Metadata.Queue queue) {
		log.info("");
		String path = RoQZKHelpers.makePath(getZKPath(queue), "monitor");
		String data = RoQZKHelpers.getDataString(client, path);
		log.info("Monitor address: " + data);
		if (data == null) {
			return null;
		}
		return new Metadata.Monitor(data);
	}
	
	public Metadata.StatMonitor getStatMonitor(Metadata.Queue queue) {
		log.info("");
		String path = RoQZKHelpers.makePath(getZKPath(queue), "stat-monitor");
		String data = RoQZKHelpers.getDataString(client, path);
		log.info("StatMonitor address: " + data);
		if (data == null) {
			return null;
		}
		return new Metadata.StatMonitor(data);
	}
	
	public void setCloudConfig(byte[] cloudConfig) {
		log.info("");
		RoQZKHelpers.createZNodeAndParents(client, cfg.znode_cloud);
		RoQZKHelpers.setData(client, cfg.znode_cloud, cloudConfig);
	}
	
	public byte[] getCloudConfig() {
		log.info("");
		return RoQZKHelpers.getData(client, cfg.znode_cloud);
	}
	public void setNameScalingConfig(String name, Metadata.Queue queue) {
		log.info("");
		String path = RoQZKHelpers.makePath(getZKPath(queue), cfg.znode_scaling, "name");
		RoQZKHelpers.createZNodeAndParents(client, path);
		RoQZKHelpers.setData(client, path, name.getBytes());
	}
	public void setHostScalingConfig(byte[] scalingConfig, Metadata.Queue queue) {
		log.info("");
		setScalingConfig(scalingConfig, queue, "host");
	}
	public void setExchangeScalingConfig(byte[] scalingConfig, Metadata.Queue queue) {
		log.info("");
		setScalingConfig(scalingConfig, queue, "exchange");
	}
	public void setQueueScalingConfig(byte[] scalingConfig, Metadata.Queue queue) {
		log.info("");
		setScalingConfig(scalingConfig, queue, "queue");
	}
	public byte[] getNameScalingConfig(Metadata.Queue queue) {
		log.info("");
		String path = RoQZKHelpers.makePath(getZKPath(queue), cfg.znode_scaling, "name");
		return RoQZKHelpers.getData(client, path);
	}
	public byte[] getHostScalingConfig(Metadata.Queue queue) {
		return getScalingConfig(queue, "host");
	}
	public byte[] getExchangeScalingConfig(Metadata.Queue queue) {
		return getScalingConfig(queue, "exchange");
	}
	public byte[] getQueueScalingConfig(Metadata.Queue queue) {
		return getScalingConfig(queue, "queue");
	}
	
	// Private methods
	
	private String getZKPath(Metadata.Queue queue) {
		return RoQZKHelpers.makePath(cfg.znode_queues, queue.name);
	}
	private void setScalingConfig(byte[] scalingConfig, Metadata.Queue queue, String leafNode) {
		log.info("");
		String path = RoQZKHelpers.makePath(getZKPath(queue), cfg.znode_scaling, leafNode);
		
		RoQZKHelpers.createZNodeAndParents(client, path);
		RoQZKHelpers.setData(client, path, scalingConfig);
	}
	private byte[] getScalingConfig(Metadata.Queue queue, String leafNode) {
		log.info("");
		String path = RoQZKHelpers.makePath(getZKPath(queue), cfg.znode_scaling, leafNode);
		return RoQZKHelpers.getData(client, path);
	}

	public ArrayList<Metadata.Exchange> getExchanges(Queue queue) {
		List<String> addresses = RoQZKHelpers.getChildren(client,  
				RoQZKHelpers.makePath(getZKPath(queue), "exchanges"));
		ArrayList<Metadata.Exchange> exchanges = new ArrayList<Metadata.Exchange>();
		for (String address : addresses) {
			exchanges.add(new Metadata.Exchange(address));
		}
		return exchanges;
	}

	public ArrayList<Metadata.BackupMonitor> getBackUpMonitors(Queue queue) {
		log.info("");
		List<String> addresses = RoQZKHelpers.getChildren(client, 
				RoQZKHelpers.makePath(getZKPath(queue), "monitorBU"));
		ArrayList<Metadata.BackupMonitor> monitors = new ArrayList<Metadata.BackupMonitor>();
		for (String address : addresses) {
			monitors.add(new Metadata.BackupMonitor(RoQZKHelpers.getDataString(client,
					RoQZKHelpers.makePath(getZKPath(queue), "monitorBU", address))));
		}
		return monitors;
	}	

}
