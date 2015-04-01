package org.roqmessaging.zookeeper;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZKUtil;

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
			// e.printStackTrace();
		}
	}
	/**
	 * Allow to create multiple znodes 
	 * in an atomic fashion
	 * @param client
	 * @param path
	 */
	public static void createQueueZNodes(CuratorFramework client, String queuePath,
			String monitorPath, String monitorPL, String statMonitorPath, 
			String statMonitorPL, String hcmPath, String hcmPL, String ExchPath, String scalingPath) {
		try {
			client.inTransaction().create().forPath(queuePath)
				.and().create().forPath(monitorPath, monitorPL.getBytes())
				.and().create().forPath(statMonitorPath, statMonitorPL.getBytes())
				.and().create().forPath(hcmPath, hcmPL.getBytes())
				.and().create().forPath(scalingPath)
				.and().commit();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	
	public static void createZNode(CuratorFramework client, String path, String payload) {
		createZNode(client, path, payload.getBytes());
	}
	public static void createZNode(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().forPath(path, payload);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	public static void createEphemeralZNode(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().withMode(CreateMode.EPHEMERAL).forPath(path, payload);
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
