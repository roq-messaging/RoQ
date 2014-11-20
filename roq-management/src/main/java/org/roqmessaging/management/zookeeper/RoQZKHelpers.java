package org.roqmessaging.management.zookeeper;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;

// Helper functions to wrap the zookeeper operations. 
public class RoQZKHelpers {
	static boolean zNodeExists(CuratorFramework client, String path) {
		try {
			return client.checkExists().forPath(path) != null;
		} catch (Exception e) {
			return false;
		}
	}
	static void createZNode(CuratorFramework client, String path) {
		try {
			client.create().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	static void createZNode(CuratorFramework client, String path, String payload) {
		createZNode(client, path, payload.getBytes());
	}
	static void createZNode(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().forPath(path, payload);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	static void createZNodeAndParents(CuratorFramework client, String path) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	static void createZNodeAndParents(CuratorFramework client, String path, String payload) {
		createZNodeAndParents(client, path, payload.getBytes());
	}
	static void createZNodeAndParents(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path, payload);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	
	static void deleteZNode(CuratorFramework client, String path) {
		try {
			client.delete().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	static void deleteZNodeAndChildren(CuratorFramework client, String path) {
		try {
			client.delete().deletingChildrenIfNeeded().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	
	static void setData(CuratorFramework client, String path, byte[] payload) {
		try {
			client.setData().forPath(path, payload);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static void setDataString(CuratorFramework client, String path, String payload) {
		setData(client, path, payload.getBytes());
	}
	static String getDataString(CuratorFramework client, String path) {
		byte[] data = getData(client, path);
		if (data == null) {
			return null;
		}
		return new String(data);
	}
	static byte[] getData(CuratorFramework client, String path) {
		try {
			return client.getData().forPath(path);
		} catch (Exception e) {
			// e.printStackTrace();
			return null; 
		}
	}
	
	static List<String> getChildren(CuratorFramework client, String path) {
		try {
			return client.getChildren().forPath(path);
		} catch (Exception e) {
			return null;
		}
	}
	
	// Utility functions for handling paths
	static String makePath(String ...nodes) {
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
