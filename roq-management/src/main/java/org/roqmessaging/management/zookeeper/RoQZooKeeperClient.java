package org.roqmessaging.management.zookeeper;

import java.io.EOFException;
import java.util.List;
import java.util.ArrayList;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.log4j.Logger;
import org.roqmessaging.core.utils.RoQUtils;
import org.roqmessaging.zookeeper.RoQZKHelpers;
import org.roqmessaging.zookeeper.RoQZooKeeper;
import org.roqmessaging.management.GlobalConfigLeaderListener;
import org.roqmessaging.management.zookeeper.RoQZooKeeperConfig;

public class RoQZooKeeperClient extends RoQZooKeeper {
	private final Logger log = Logger.getLogger(getClass());
	private LeaderLatch leaderLatch;
	private RoQZooKeeperConfig cfg;
	
	public RoQZooKeeperClient(RoQZooKeeperConfig config) {
		super(config);
		log.info("");
		
		cfg = config;	
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void initZkClusterNodes() {
		String path = RoQZKHelpers.makePath(cfg.znode_queues);
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

	public boolean isLeader() {
		log.info("");
		return leaderLatch.hasLeadership();
	}
	
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
	public void addHCM(Metadata.HCM hcm) {
		log.info("");
		RoQZKHelpers.createZNodeAndParents(client, getZKPath(hcm));
	}
	
	// Remove the znode if it exists.
	public void removeHCM(Metadata.HCM hcm) {
		log.info("");
		RoQZKHelpers.deleteZNode(client, getZKPath(hcm));
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

	public List<Metadata.HCM> getHCMList() {
		log.info("");
		
		List<Metadata.HCM> hcms = new ArrayList<Metadata.HCM>();
		List<String> znodes = RoQZKHelpers.getChildren(client, cfg.znode_hcm);
		
		// If something goes wrong, return an empty list.
		if (znodes == null) {
			log.debug("Helpers.getChildren() failed. Aborting getHCMList().");
			return hcms;
		}
		
		for (String node : znodes) {
			hcms.add(new Metadata.HCM(node));
		}
		return hcms;
	}
	
	public boolean queueExists(Metadata.Queue queue) {
		log.info("");
		return RoQZKHelpers.zNodeExists(client, getZKPath(queue));
	}
	
	public void createQueue(Metadata.Queue queue, Metadata.HCM hcm, Metadata.Monitor monitor, Metadata.StatMonitor statMonitor) {
		log.info("");
		
		String queuePath = getZKPath(queue);
		String monitorPath = RoQZKHelpers.makePath(queuePath, "monitor");
		String statMonitorPath = RoQZKHelpers.makePath(queuePath, "stat-monitor");
		String hcmPath = RoQZKHelpers.makePath(queuePath, "hcm");
		// RoQZKHelpers.createZNodeAndParents(client, queuePath);
		// Add queue children nodes inside a single transaction
		RoQZKHelpers.createQueueZNodes(client, queuePath, monitorPath, 
				monitor.address, statMonitorPath, statMonitor.address, 
				hcmPath, hcm.address);
	}
	
	public void removeQueue(Metadata.Queue queue) {
		log.info("");
		
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
	private String getZKPath(Metadata.HCM hcm) {
		return RoQZKHelpers.makePath(cfg.znode_hcm, hcm.address);
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
}
