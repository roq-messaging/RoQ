package org.roqmessaging.zookeeper;

import java.util.ArrayList;
import java.util.List;

public class RoQZooKeeperMonitorClient extends RoQZooKeeper {

	public RoQZooKeeperMonitorClient(RoQZKSimpleConfig config) {
		super(config);
	}
	
	public RoQZooKeeperMonitorClient(String zkAdrress) {
		super(zkAdrress);
	}
	
	/**
	 * Create the parent node that contains the list 
	 * of exchanges for a monitor
	 * @param monitor
	 */
	public void createQueueExchanges(String queue) {
		String path = RoQZKHelpers.makePath(cfg.znode_queues_exchanges, queue);
		RoQZKHelpers.createZNode(client, path);
	}
	
	/**
	 * Add an exchange in the list
	 * @param monitor
	 */
	public void addXchangeInQueueExchanges(String queue, String exchangeRepAddress) {
		String path = RoQZKHelpers.makePath(cfg.znode_queues_exchanges, queue, 
				new Metadata.Exchange(exchangeRepAddress).zkNodeString());
		RoQZKHelpers.createZNode(client, path, exchangeRepAddress);
	}
	
	/**
	 * Add an exchange in the list
	 * @param monitor
	 */
	public void deleteXchangeInQueueExchanges(String queue, String exchangeRepAddress) {
		String path = RoQZKHelpers.makePath(cfg.znode_queues_exchanges, queue, 
				new Metadata.Exchange(exchangeRepAddress).zkNodeString());
		RoQZKHelpers.deleteZNode(client, path);
	}	
		
	/**
	 * Get the addresses of all the exchanges
	 * @param monitor
	 * @return 
	 */
	public List<String> getXchangesListInQueueExchanges(String queue) {
		String path = RoQZKHelpers.makePath(cfg.znode_queues_exchanges, queue);
		List<String> exchangesList = RoQZKHelpers.getChildren(client, path);
		List<String> exchangesAddressList = new ArrayList<String>();
		if (exchangesList != null) {
			for (String exchange : exchangesList) {
				path = RoQZKHelpers.makePath(cfg.znode_queues_exchanges, queue, exchange);
				exchangesAddressList.add(RoQZKHelpers.getDataString(client, path));
			}
		}
		return exchangesAddressList;
	}

}
