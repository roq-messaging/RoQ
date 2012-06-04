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
package org.roqmessaging.management.server;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BSONObject;
import org.roqmessaging.core.RoQConstant;
import org.roqmessaging.core.ShutDownMonitor;
import org.roqmessaging.core.interfaces.IStoppable;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.management.GlobalConfigurationManager;
import org.roqmessaging.management.serializer.IRoQSerializer;
import org.roqmessaging.management.serializer.RoQBSONSerializer;
import org.zeromq.ZMQ;

/**
 * Class MngtController
 * <p>
 * Description: Controller that loads/receive data from the
 * {@linkplain GlobalConfigurationManager} and refresh the stored data. Global
 * config manager: 5000 <br>
 * Global config manager pub sub port 5001<br>
 * Shutdown monitor port 5002<br>
 * Management contoller on port 5003<br>
 * Management timer port 5004
 * 
 * 
 * @author sskhiri
 */
public class MngtController implements Runnable, IStoppable {
	private Logger logger = Logger.getLogger(MngtController.class);
	// ZMQ
	private ZMQ.Context context = null;
	private ZMQ.Socket mngtSubSocket = null;
	private ZMQ.Socket mngtRepSocket = null;
	// running
	private volatile boolean active = true;
	private ShutDownMonitor shutDownMonitor = null;
	// utils for serialization
	private RoQSerializationUtils serializationUtils = null;
	// Management infra
	private MngtServerStorage storage = null;
	// The DB file
	private String dbName = "Management.db";
	//The publication period of the configuration
	private int period = 60000;

	/**
	 * Constructor.
	 * 
	 * @param globalConfigAddress
	 *            the address on which the global config server runs.
	 */
	public MngtController(String globalConfigAddress, String dbName, int period) {
		try {
			this.period = period;
			this.dbName = dbName;
			init(globalConfigAddress, 5004);
		} catch (SQLException e) {
			logger.error("Error while initiating the SQL connection", e);
		} catch (ClassNotFoundException e) {
			logger.error("Error while initiating the SQL connection", e);
		}
	}


	/**
	 * We start start the global conifg at 5000 + 5001 for its shutdown port, 5002 for the configuration timer + 5003 for its shutdown.
	 * @param globalConfigAddress
	 *            the global configuration server
	 * @param shuttDownPort
	 *            the port on which the shut down thread listens
	 * @throws SQLException
	 * @throws ClassNotFoundException whenthe JDBC driver has not been found
	 */
	private void init(String globalConfigAddress, int shuttDownPort) throws SQLException, ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		// Init ZMQ subscriber API for configuration
		context = ZMQ.context(1);
		mngtSubSocket = context.socket(ZMQ.SUB);
		mngtSubSocket.connect("tcp://" + globalConfigAddress + ":5002");
		mngtSubSocket.subscribe("".getBytes());
		// Init ZMQ subscriber API for configuration
		mngtRepSocket = context.socket(ZMQ.REP);
		mngtRepSocket.bind("tcp://" + globalConfigAddress + ":5003");
		// init variable
		this.serializationUtils = new RoQSerializationUtils();
		this.storage = new MngtServerStorage(DriverManager.getConnection("jdbc:sqlite:" + this.dbName));
		// Shutdown thread configuration
		this.shutDownMonitor = new ShutDownMonitor(shuttDownPort, this);
		new Thread(this.shutDownMonitor).start();
	}

	/**
	 * Runs in a while loop and wait for updated information from the
	 * {@linkplain GlobalConfigurationManager}
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		logger.debug("Starting "+ getName());
		this.active = true;
		
		//Launching the timer 
		MngtControllerTimer controllerTimer = new MngtControllerTimer(this.period, this, 5004);
		Timer publicationTimer = new Timer();
		publicationTimer.schedule(controllerTimer,  period, period);
		
		// ZMQ init of the subscriber socket
		ZMQ.Poller poller = context.poller(2);
		poller.register(mngtSubSocket);// 0
		poller.register(mngtRepSocket);
		// Init variables
		int infoCode = 0;

		// Start the running loop
		while (this.active && !Thread.currentThread().isInterrupted()) {
			// Set the poll time out, it returns either when something arrive or
			// when it time out
			poller.poll(2000);
			if (poller.pollin(0)) {
				logger.debug("Recieving Message in the update broadcast update channel");
				// An event arrives start analysing code
				String info= new String(mngtSubSocket.recv(0));
				// Check if exchanges are present: this happens when the queue
				// is shutting down a client is asking for a
				// connection
				infoCode = Integer.parseInt(info);
				switch (infoCode) {
				case RoQConstant.MNGT_UPDATE_CONFIG:
					// Infocode, map(Q Name, host)
					logger.debug("Recieving update configuration message");
					if (mngtSubSocket.hasReceiveMore()) {
						HashMap<String, String> newConfig = this.serializationUtils.deserializeObject(mngtSubSocket
								.recv(0));
						try {
							storage.updateConfiguration(newConfig);
						} catch (SQLException e) {
							logger.error("Error while updating the configuration", e);
						}
					} else {
						// Problem expected map here
						logger.error("Error, expected map of (Qname, host", new IllegalStateException(
								"Expected a second message" + " in the envelope"));
					}
					break;
				default:
					break;
				}
			}
			//Pollin 1 = Management facade interface in BSON
			if (poller.pollin(1)) {
				IRoQSerializer serializer = new RoQBSONSerializer();
				// 1. Checking the command ID
				BSONObject request = BSON.decode(mngtRepSocket.recv(0));
				if (!request.containsField("CMD")) {
					mngtRepSocket.send(serializer.serialiazeConfigAnswer(RoQConstant.FAIL,
							"The command does not contain a CMD field"), 0);
				} else {
					// 2. Getting the command ID
					switch ((Integer) request.get("CMD")) {
					case RoQConstant.BSON_CONFIG_REMOVE_QUEUE:

						break;

					case RoQConstant.BSON_CONFIG_START_QUEUE:

						break;

					case RoQConstant.BSON_CONFIG_STOP_QUEUE:

						break;

					default:
						break;
					}
				}
			}
		}//END OF THE LOOP
		
		logger.info("Stopping " + this.getClass().getName() + " cleaning sockets");
		this.mngtSubSocket.close();
		controllerTimer.cancel();
	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#shutDown()
	 */
	public void shutDown() {
		this.active = false;

	}

	/**
	 * @see org.roqmessaging.core.interfaces.IStoppable#getName()
	 */
	public String getName() {
		return this.getClass().getName() + " Server";
	}

	/**
	 * @return the shutDownMonitor
	 */
	public ShutDownMonitor getShutDownMonitor() {
		return shutDownMonitor;
	}

	/**
	 * @return the storage
	 */
	public MngtServerStorage getStorage() {
		return storage;
	}

	/**
	 * @param storage the storage to set
	 */
	public void setStorage(MngtServerStorage storage) {
		this.storage = storage;
	}

}
