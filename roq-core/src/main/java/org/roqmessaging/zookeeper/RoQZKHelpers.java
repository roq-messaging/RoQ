package org.roqmessaging.zookeeper;

import java.util.ArrayList;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionBridge;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZKUtil;
import org.roqmessaging.zookeeper.Metadata.BackupMonitor;
import org.roqmessaging.zookeeper.Metadata.Monitor;
import org.roqmessaging.zookeeper.Metadata.StatMonitor;

// Helper functions to wrap the zookeeper operations. 
public class RoQZKHelpers {
	public static String ZK_BASE;
	
	public static boolean zNodeExists(CuratorFramework client, String path) {
		try {
			return client.checkExists().forPath(path) != null;
		} catch (Exception e) {
			return false;
		}
	}
	public static void createZNode(CuratorFramework client, String path) {
		try {
			client.create().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Allow to create multiple znodes 
	 * in a transaction
	 * @param client
	 * @param pathsHCMBuMonitorToAdd 
	 * @param pathHCMMonitorToAdd 
	 * @param path
	 */
	public static void createQueueZNodes(CuratorFramework client, String queuePath,
			String monitorPath, String monitorPL, String statMonitorPath,
			String statMonitorPL, String hcmPath, String hcmPL, String scalingPath, String backupMonitorsPath,
			List<Metadata.BackupMonitor> backupMonitors, String pathHCMMonitorToAdd, ArrayList<String> pathsHCMBuMonitorToAdd) {
		try {
			CuratorTransactionBridge transaction = client.inTransaction().create().forPath(queuePath)
				.and().create().forPath(pathHCMMonitorToAdd)
				.and().create().forPath(monitorPath, monitorPL.getBytes())
				.and().create().forPath(statMonitorPath, statMonitorPL.getBytes())
				.and().create().forPath(hcmPath, hcmPL.getBytes())
				.and().create().forPath(scalingPath)
				.and().create().forPath(backupMonitorsPath);
			for (int i = 0; i < backupMonitors.size(); i++) {
				transaction.and().create().forPath(makePath(backupMonitorsPath, backupMonitors.get(i).zkNodeString()), 
						(backupMonitors.get(i).getData()).getBytes());
			}
			for (String pathBUToAdd : pathsHCMBuMonitorToAdd) {
				transaction.and().create().forPath(pathBUToAdd);
			}
			transaction.and().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * replace a monitor by a standby monitor inside a single
	 * transaction
	 * @param client
	 * @param hCMStateAdd
	 * @param hCMStateRemove
	 * @param pathQBUMonitorToRemove
	 * @param hcmPath
	 * @param hcmAddress
	 * @param statMonitorPath
	 * @param statMonitor
	 * @param monitorPath
	 * @param monitor
	 */
	public static void replaceMonitorZNodes(CuratorFramework client,
			String hCMStateAdd, String hCMStateRemove,
			String pathQBUMonitorToRemove, String hcmPath, String hcmAddress,
			String statMonitorPath, StatMonitor statMonitor,
			String monitorPath, Monitor monitor, String scalingPath) {
		try {
			client.inTransaction()
				.delete().forPath(pathQBUMonitorToRemove)
				.and().delete().forPath(hCMStateRemove)
				.and().create().forPath(hCMStateAdd)
				.and().delete().forPath(hcmPath)
				.and().create().forPath(hcmPath, hcmAddress.getBytes())
				.and().delete().forPath(statMonitorPath)
				.and().create().forPath(statMonitorPath, statMonitor.address.getBytes())
				.and().delete().forPath(monitorPath)
				.and().create().forPath(monitorPath, monitor.address.getBytes())
				.and().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * add a backup monitor for a given queue inside 
	 * a single transaction
	 * @param client
	 * @param hCMStateAdd
	 * @param backupMonitorsPath
	 * @param newBUMonitor
	 */
	public static void addBackupMonitorZNodes(CuratorFramework client,
			String hCMStateAdd, String backupMonitorsPath,
			BackupMonitor newBUMonitor) {
		try {
			client.inTransaction()
				.create().forPath(hCMStateAdd)
				.and().create().forPath(backupMonitorsPath, newBUMonitor.getData().getBytes())
				.and().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Replace a backup monitor by a new one inside 
	 * a single transaction
	 * @param client
	 * @param hCMStateAdd
	 * @param hCMStateRemove
	 * @param pathQBUMonitorToRemove
	 * @param backupMonitorsPath
	 * @param newBUMonitor
	 */
	public static void replaceBackupMonitorZNodes(CuratorFramework client,
			String hCMStateAdd, String hCMStateRemove,
			String pathQBUMonitorToRemove, String backupMonitorsPath,
			BackupMonitor newBUMonitor) {
		try {
			client.inTransaction()
				.delete().forPath(pathQBUMonitorToRemove)
				.and().delete().forPath(hCMStateRemove)
				.and().create().forPath(hCMStateAdd)
				.and().create().forPath(backupMonitorsPath, newBUMonitor.getData().getBytes())
				.and().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public static void createZNode(CuratorFramework client, String path, String payload) {
		createZNode(client, path, payload.getBytes());
	}
	public static void createZNode(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().forPath(path, payload);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void createEphemeralZNode(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().withMode(CreateMode.EPHEMERAL).forPath(path, payload);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	public static void createEphemeralZNode(CuratorFramework client, String path) {
		try {
			client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	public static void createZNodeAndParents(CuratorFramework client, String path) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	public static void createZNodeAndParents(CuratorFramework client, String path, String payload) {
		createZNodeAndParents(client, path, payload.getBytes());
	}
	public static void createZNodeAndParents(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path, payload);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	
	public static void deleteZNode(CuratorFramework client, String path) {
		try {
			client.delete().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	public static void deleteZNodeAndChildren(CuratorFramework client, String path) {
		try {
			ZKUtil.deleteRecursive(client.getZookeeperClient().getZooKeeper(), "/" + ZK_BASE + "/" + path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void setData(CuratorFramework client, String path, byte[] payload) {
		try {
			client.setData().forPath(path, payload);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void setDataString(CuratorFramework client, String path, String payload) {
		setData(client, path, payload.getBytes());
	}
	public static String getDataString(CuratorFramework client, String path) {
		byte[] data = getData(client, path);
		if (data == null) {
			return null;
		}
		return new String(data);
	}
	public static byte[] getData(CuratorFramework client, String path) {
		try {
			return client.getData().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
			return null; 
		}
	}
	
	public static List<String> getChildren(CuratorFramework client, String path) {
		try {
			return client.getChildren().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// Utility functions for handling paths
	public static String makePath(String ...nodes) {
		StringBuilder path = new StringBuilder();
		boolean first = true;
		for (String node : nodes) {
			// Only add a forward slash between nodes
			if (!first) {
				path.append("/");
			} else {
				first = false;
			}

			path.append(node);
		}
		return path.toString();
	}
	
}
