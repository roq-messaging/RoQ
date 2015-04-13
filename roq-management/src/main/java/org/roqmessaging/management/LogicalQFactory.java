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
package org.roqmessaging.management;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory;
import org.roqmessaging.core.RoQConstant;
import org.zeromq.ZMQ;

/**
 * Class LogicalQueueFactory
 * <p>
 * Description: Responsible of the logical queue Life cycle management. Binds
 * config server on port 5000 and config server on 5100
 * 
 * @author sskhiri, bvanmelle
 */
public class LogicalQFactory implements IRoQLogicalQueueFactory {
	// ZMQ elements
	private ZMQ.Socket globalConfigReq = null;
	private ZMQ.Context context;
	// Logger
	private Logger logger = Logger.getLogger(LogicalQFactory.class);
	// Config
	private String configServer = null;
	private boolean initialized = false;
	// Config to hold
	private GlobalConfigurationStateClient configurationState = null;
	// Lock
	private Lock lockCreateQ = new ReentrantLock();
	private Lock lockRemoveQ = new ReentrantLock();

	/**
	 * Initialise the socket to the config server.
	 */
	public LogicalQFactory(String configServer, int hcmTIMEOUT) {
		this.configServer = configServer;
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://" + this.configServer + ":5000");
		this.configurationState = new GlobalConfigurationStateClient(this.configServer, hcmTIMEOUT);
	}
	
	/**
	 * Initialise the socket to the config server.
	 */
	public LogicalQFactory(String configServer) {
		this.configServer = configServer;
		context = ZMQ.context(1);
		globalConfigReq = context.socket(ZMQ.REQ);
		globalConfigReq.connect("tcp://" + this.configServer + ":5000");
		this.configurationState = new GlobalConfigurationStateClient(this.configServer);
	}

	@Override
	/**
	 * 1. Check if the name already exist in the topology <br>
	 * 2. Create the entry in the global config <br>
	 * 3. Sends the create event to the hostConfig manager thread 4. If the
	 * answer is not confirmed, we remove back the entry in the global config
	 * and throw an exception
	 * 
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#createQueue(java.lang.String,
	 *      java.lang.String)
	 */
	public int createQueue(String queueName, String targetAddress, ArrayList<String> monitorsBU, boolean recoveryMod) 
			throws IllegalStateException {
		logger.info("Queue creation process for queue: " + queueName + " on: " + targetAddress + " with " + monitorsBU.size() + " backup monitor(s)");
		try {
			this.lockCreateQ.lock();
			this.refreshTopology();
			if(queueAlreadyExists(queueName) && !recoveryMod) return -1 ; // The queue exists and was well created
			if(!hostExists(targetAddress)) return -2; // HCM not exists	
			//2. Sends the create event to the hostConfig manager thread
			ZMQ.Socket hostSocket = this.configurationState.getHostManagerMap().get(targetAddress);
			hostSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + queueName).getBytes(), 0);
			byte[] response = hostSocket.recv(0);
			if (response == null) {
				logger.info("HCM: " + targetAddress + " has timeout");
				return -2; // HCM has timeout
			}			
			String[] resultHost = new String(response).split(",");
			if (Integer.parseInt(resultHost[0]) != RoQConstant.CONFIG_REQUEST_OK) {
				logger.error("The queue creation for  " + queueName
						+ " failed on: " + targetAddress);
				return -3; // Failure at the HCM
			} else {
				// Create the backup hosts
				String monitorsBUString = "";
				for (String buHost : monitorsBU) {
					logger.info("Create a backup on: " + buHost);
					if(!hostExists(buHost)) {
						logger.info("HCM: " + buHost + " has been removed");
						// return -5; // BACKUP HCM not exists
					}
					hostSocket = this.configurationState.getHostManagerMap().get(buHost);
					hostSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_STBY_MONITOR) + "," + queueName).getBytes(), 0);
					response = hostSocket.recv(0);
					if (response == null) {
						logger.info("HCM: " + buHost + " has timeout");
						// return -6; // BACKUP HCM has timeout
					} else {
						String[] resultHost2 = new String(response).split(",");
						if (Integer.parseInt(resultHost[0]) != RoQConstant.CONFIG_REQUEST_OK) {
							logger.error("The backup monitor creation process for  " + queueName
									+ " failed on: " + buHost);
							// return -7; // Failure at the HCM
						} else {
							monitorsBUString += ",";
							monitorsBUString += buHost + "," + resultHost2[1] + "," + resultHost2[2];
						}
					}
				}
				
				logger.info("Created queue " + queueName + " @ " + resultHost[1]);
				// 3.B. Create the entry in the global config
				globalConfigReq.send(
						(Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + queueName + "," +resultHost[1]  
								+ "," +resultHost[2]+","+targetAddress + monitorsBUString)
								.getBytes(), 0);
				String result = new String(globalConfigReq.recv(0));
				if (Integer.parseInt(result) != RoQConstant.CONFIG_REQUEST_OK) {
					logger.error("The queue creation for  " + queueName
							+ " failed on the global configuration server");
					return -4; //Failed to register the queue at GCM
				}else{
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						logger.warn(e);
					}
					return 0;
				}
			}
		} finally {
			this.lockCreateQ.unlock();
		}
	}

	@Override
	/**
	 * @param queueName the logical queue
	 * @return true if the check is OK
	 */
	public boolean queueAlreadyExists(String queueName) {
		if (!initialized)
			this.configurationState.refreshConfiguration();
		// 1. Check if the name already exist in the topology
		if (this.configurationState.getQueueMonitorMap().containsKey(queueName)) {
			// the queue already exist
			logger.error(new IllegalStateException("The queue name " + queueName + " already exists"));
			return true;
		} else {
			return false;
		}
	}
		
	/**
	 * @param targetAddress the target address
	 * @return true if the check is OK
	 */
	private boolean hostExists(String targetAddress) {		
		if (!this.configurationState.getHostManagerMap().containsKey(targetAddress)) {
			// the target address is not registered as node of the cluster
			// (no Host manager running)
			logger.error(new IllegalStateException("the target address " + targetAddress + " is not registered as a "
					+ "node of the cluster (no Host manager running)"));
			return false;
		}
		return true;
	}
	
	/**
	 * @param queueName the logical queue
	 * @param targetAddress the target address
	 * @return true if the check is OK
	 */
	private boolean checkForUpdateQ(String queueName, String targetAddress) {
		// 1. Check if the name already exist in the topology
		if (!this.configurationState.getQueueMonitorMap().containsKey(queueName)) {
			logger.error(new IllegalStateException("The queue name " + queueName + " is not registrated in the cache"));
			return false;
		}
		// 2. Check if the monitor stat is registrated
		if (!this.configurationState.getQueueMonitorStatMap().containsKey(queueName)) {
			logger.error(new IllegalStateException("The monitor stat of queue name " + queueName + " is not registrated in the cache"));
			return false;
		}
		
		if (!this.configurationState.getHostManagerMap().containsKey(targetAddress)) {
			// the target address is not registered as node of the cluster
			// (no Host manager running)
			logger.error(new IllegalStateException("the target address " + targetAddress + " is not registered as a "
					+ "node of the cluster (no Host manager running)"));
			return false;
		}
		return true;
	}

	/**
	 * Remove the configuration from the configuration: <br>
	 * 1. Remove from local cache - clean socket<br>
	 * 2. Remove the configuration from global configuration
	 * 
	 * @param queueName
	 *            the name of the queue to remove.
	 * @return the monitor host to send the remove request
	 */
	private String removeQFromGlobalConfig(String queueName) {
		logger.debug("Removing "+ queueName+" from the global config");
		// 1. Clean local cache
		String monitorHost = this.configurationState.getQueueMonitorMap().remove(queueName);
		// 2. Ask the global configuration to remove the queue
		globalConfigReq.send((Integer.toString(RoQConstant.CONFIG_REMOVE_QUEUE) + "," + queueName).getBytes(), 0);
		String result = new String(globalConfigReq.recv(0));
		if (Integer.parseInt(result) != RoQConstant.OK) {
			logger.error("Error when removing the queue " + queueName + " from global config",
					new IllegalStateException("The queue creation for  " + queueName
							+ " failed on the global configuration server"));
		}
		return monitorHost;
	}

	@Override
	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#removeQueue(java.lang.String)
	 */
	public boolean removeQueue(String queueName) {
		try {
			this.lockRemoveQ.lock();
			this.refreshTopology();
			// 1. Get the monitor address & Remove the entry in the global
			// configuration
			logger.info("Removing Q " + queueName);
			// We attack directly the host config manager so, the first things
			// to do it to get the host manager
			// for this logical queue
			// 1. get the host location of the Q
			String host = this.configurationState.getQueueHostLocation().get(queueName);
			if (host == null) {
				logger.error("The queue name is not registred in the local cache", new IllegalStateException(
						"The Q name is not registred in the local cache"));
				return false;
			}
	
			// 2. Get the socket of the host local manager
			ZMQ.Socket hostManagerSocket = this.configurationState.getHostManagerMap().get(host);
			if (hostManagerSocket == null) {
				logger.error("The host manager socket is not registrated in the local cache",
						new IllegalStateException("The Q host manager socke is not registred in the local cache"));
				return false;
			}
			// 3. send the shutdown request
			logger.debug("Sending remove queue request to host manager at " + host +" "+ hostManagerSocket.toString());
			hostManagerSocket.send((Integer.toString(RoQConstant.CONFIG_REMOVE_QUEUE) + "," + queueName).getBytes(), 0);
			logger.debug("Answer from the host config manager "+  new String(hostManagerSocket.recv(0)));

			// 4. Remove standby monitors
			List<String> buHost = this.configurationState.getQueueBUMonitorHostMap().get(queueName);
			for (String backupHost : buHost) {
				hostManagerSocket = this.configurationState.getHostManagerMap().get(backupHost);
				if (hostManagerSocket == null) {
					logger.error("The host manager socket is not registrated in the local cache for: " + backupHost,
							new IllegalStateException("The Q host manager socke is not registred in the local cache"));
					return false;
				}
				// 3. send the shutdown request
				logger.debug("Sending remove queue request to host manager at " + host +" "+ hostManagerSocket.toString());
				hostManagerSocket.send((Integer.toString(RoQConstant.CONFIG_REMOVE_STBY_MONITOR) + "," + queueName).getBytes(), 0);
				logger.debug("Answer from the host config manager "+  new String(hostManagerSocket.recv(0)));
			}
			
			// 5. Remove Metadata
			removeQFromGlobalConfig(queueName);
		} finally {
			this.lockRemoveQ.unlock();
		}
		return true;
	}
	
	@Override
	public boolean startQueue(String queueName, String targetAddress) {
		try {
			this.lockCreateQ.lock();
			if (!this.initialized)
				this.refreshTopology();
			if(queueAlreadyExists(queueName) || !hostExists(targetAddress)) return false ;
			// The name does not exist yet
			//2. Sends the create event to the hostConfig manager thread
			ZMQ.Socket hostSocket = this.configurationState.getHostManagerMap().get(targetAddress);
			hostSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_QUEUE) + "," + queueName).getBytes(), 0);
			String[] resultHost = new String(hostSocket.recv(0)).split(",");
			
			if (Integer.parseInt(resultHost[0]) != RoQConstant.CONFIG_REQUEST_OK) {
				logger.error("The queue creation for  " + queueName
						+ " failed on the local host server");
				return false;
			} else {
				logger.info("Created queue " + queueName + " @ " + resultHost[1]);
				// 3.B. Create the entry in the global config
				globalConfigReq.send(
						(Integer.toString(RoQConstant.CONFIG_START_QUEUE) + "," + queueName)
								.getBytes(), 0);
				String result = new String(globalConfigReq.recv(0));
				if (Integer.parseInt(result) != RoQConstant.OK) {
					logger.error("The queue start for  " + queueName
							+ " failed on the global configuration server");
					return false;
				}else{
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						logger.warn(e);
					}
					return true;
				}
			}
		} finally {
			this.lockCreateQ.unlock();
		}
	}
	
	@Override
	public boolean stopQueue(String queueName) {
		try {
			this.lockRemoveQ.lock();
			this.refreshTopology();
			// 1. Get the monitor address & Remove the entry in the global
			// configuration
			logger.info("Stopping Q " + queueName);
			
			// 1. Clean local cache
			configurationState.getQueueMonitorMap().remove(queueName);
			// 2. Ask the global configuration to remove the queue
			globalConfigReq.send((Integer.toString(RoQConstant.CONFIG_STOP_QUEUE) + "," + queueName).getBytes(), 0);
			String result = new String(globalConfigReq.recv(0));
			if (Integer.parseInt(result) != RoQConstant.OK) {
				logger.error("Error when removing the queue " + queueName + " from global config",
						new IllegalStateException("The queue creation for  " + queueName
								+ " failed on the global configuration server"));
				return false;
			}
			
		} finally {
			this.lockRemoveQ.unlock();
		}
		return true;
	}
	
	@Override
	/**
	 * Reload cache from the global configuration server
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#refreshTopology()
	 */
	public void refreshTopology() {
		this.configurationState.refreshConfiguration();
		this.initialized = true;
	}

	@Override
	/**
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#clean()
	 */
	public void clean() {
		try {
			this.lockRemoveQ.lock();
			logger.info("Cleaning the local cache");
			for (String host : this.configurationState.getHostManagerMap().keySet()) {
				ZMQ.Socket socket = this.configurationState.getHostManagerMap().get(host);
				socket.setLinger(0);
				socket.close();
			}
			this.configurationState.getHostManagerMap().clear();
			this.configurationState.getQueueMonitorMap().clear();
		} finally {
			this.lockRemoveQ.unlock();
		}
	}

	@Override
	/**
	 * Creates an exchange on the target address
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#createExchange(java.lang.String, java.lang.String)
	 */
	public boolean createExchange(String queueName, String targetAddress, String exchangeId) {
		this.refreshTopology();
		//1. Check
		if(!checkForUpdateQ(queueName, targetAddress))return false;
		
		//2. Get the information to send to the host manager
		//Need to send, the info code, the queue name and the monitor and stat address as the monitor can be on
		 // another machine.
		String monitorAddress = this.configurationState.getQueueMonitorMap().get(queueName);
		String monitorStatAddress = this.configurationState.getQueueMonitorStatMap().get(queueName);
		ZMQ.Socket hostMngSocket = this.configurationState.getHostManagerMap().get(targetAddress);
		
		//3. Sends a create exchange to the host manager
		logger.debug("Sending create Xchange request to host manager at " + targetAddress +" "+ hostMngSocket.toString());
		String arguments =Integer.toString(RoQConstant.CONFIG_CREATE_EXCHANGE) + "," + queueName + ","+exchangeId+","+monitorAddress+","+monitorStatAddress;
		logger.debug("Sending "+ arguments);
		hostMngSocket.send((arguments).getBytes(), 0);
		String codeBackStr = new String(hostMngSocket.recv(0));
		int codeBack = Integer.parseInt(codeBackStr);
		if(codeBack== RoQConstant.FAIL) {
			logger.error("The create exchange failed");
			return false;
		}
		
		//3. Sends to the global config Manager - > no needs.
		return true;
	}
	
	@Override
	/**
	 * Creates a backup monitor
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#createBackupMonitor(String queue, String hcmAddress, String hcmRemoved)
	 */
	public boolean createBackupMonitor(String queue, String hcmAddress, String hcmRemoved) {
		logger.info("Create a backup on: " + hcmAddress);
		try {
			this.lockCreateQ.lock();
			ZMQ.Socket hostSocket = this.configurationState.getHostManagerMap().get(hcmAddress);
			hostSocket.send((Integer.toString(RoQConstant.CONFIG_CREATE_STBY_MONITOR) + "," + queue).getBytes(), 0);
			byte[] response = hostSocket.recv(0);
			if (response == null) {
				logger.info("HCM: " + hcmAddress + " has timeout");
				// return -6; // BACKUP HCM has timeout
			} else {
				String[] resultHost =new String(response).split(",");
				if (Integer.parseInt(resultHost[0]) != RoQConstant.CONFIG_REQUEST_OK) {
					logger.error("The backup monitor creation process for  " + queue
							+ " failed on: " + hcmAddress);
				} else {
					// Update the metadata
					if (hcmRemoved != null) {
						globalConfigReq.send(
								(Integer.toString(RoQConstant.CONFIG_REPLACE_QUEUE_BACKUP_MONITOR) + 
										"," + queue + "," + hcmAddress + "," + resultHost[1] + "," + resultHost[2] + "," + hcmRemoved)
										.getBytes(), 0);
						String GCMresult = new String(globalConfigReq.recv(0));
						if (Integer.parseInt(GCMresult) != RoQConstant.OK) {
							logger.error("The metadata update failed");
							return false;
						} else {
							return true;
						}
					} else {
						globalConfigReq.send(
								(Integer.toString(RoQConstant.CONFIG_ADD_QUEUE_BACKUP_MONITOR) + 
										"," + queue + "," + hcmAddress + "," + resultHost[1] + "," + resultHost[2]).getBytes(), 0);
						String GCMresult = new String(globalConfigReq.recv(0));
						if (Integer.parseInt(GCMresult) != RoQConstant.OK) {
							logger.error("The metadata update failed");
							return false;
						} else {
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Failover failed: " + e);
		} finally {
			this.lockCreateQ.unlock();
		}
		return false;
	}

	@Override
	/**
	 * activate a standby monitor
	 * @see org.roqmessaging.clientlib.factory.IRoQLogicalQueueFactory#failoverOnBackupMonitor(String queue, String hcmAddress)
	 */
	public boolean failoverOnBackupMonitor(String queue, String hcmAddress) {
		logger.info("failover on: " + hcmAddress);
		try {
			this.lockCreateQ.lock();
			// The name does not exist yet
			//2. Sends the create event to the hostConfig manager thread
			ZMQ.Socket hostSocket = this.configurationState.getHostManagerMap().get(hcmAddress);
			hostSocket.send((Integer.toString(RoQConstant.CONFIG_START_STBY_MONITOR) + "," + queue).getBytes(), 0);
			byte[] result = hostSocket.recv(0);
			if (result != null) {
				String[] resultHost = new String(result).split(",");
				if (Integer.parseInt(resultHost[0]) != RoQConstant.CONFIG_REQUEST_OK) {
					logger.error("The STBY monitor startup for  " + queue
							+ " failed on the local host server");
					return false;
				} else {
					logger.info("Started standby monitor for " + queue + " @ " + hcmAddress);
					// Update the metadata
					globalConfigReq.send(
							(Integer.toString(RoQConstant.CONFIG_REPLACE_QUEUE_MONITOR) + 
									"," + queue + "," + hcmAddress + "," + resultHost[1] + "," + resultHost[2])
									.getBytes(), 0);
					String GCMresult = new String(globalConfigReq.recv(0));
					if (Integer.parseInt(GCMresult) != RoQConstant.OK) {
						logger.error("The metadata update failed");
						return false;
					} else {
						return true;
					}
				}
			} else {
				logger.error("The hcm didn't answered");
				return false;
			}
		} catch (Exception e) {
			logger.error("Failover failed: " + e);
		} finally {
			this.lockCreateQ.unlock();
		}
		return false;
	}

	/**
	 * @return the configurationState
	 */
	public GlobalConfigurationStateClient getConfigurationState() {
		return configurationState;
	}

}
