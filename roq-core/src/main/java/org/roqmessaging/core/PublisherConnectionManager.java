// (c) 2011 Tran Nam-Luc - Euranova nv/sa

package org.roqmessaging.core;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.roqmessaging.core.utils.RoQSerializationUtils;
import org.roqmessaging.state.PublisherConfigState;
import org.zeromq.ZMQ;

/**
 * Class PublisherConnectionManager
 * <p> Description: responsible for managing the connection between the publisher and the exchange. This manager can be notified by the 
 * monitor by any topology change.
 * 
 * @author sskhiri
 * @author Nam-Luc tran
 * @author bvanmelle
 */
public class PublisherConnectionManager implements Runnable {
	private Logger logger = Logger.getLogger(PublisherConnectionManager.class);

	private ZMQ.Context context;
	private String s_ID;
	private HashMap<String, ZMQ.Socket> monitorsSub = new HashMap<String, ZMQ.Socket>();
	private String s_currentExchange;

	private HashMap<String, ZMQ.Socket> initReqSockets = new HashMap<String, ZMQ.Socket>();
	private ZMQ.Socket tstmpReq;

	// Mesage to send
	byte[] key, msg;
	//Volatile to force a flush in main memory
	private volatile boolean running;
	//The publisher state DAO for handling the conf
	private PublisherConfigState configState = null;
	//define whether we relocate
	private volatile boolean relocating = false;
	

	public boolean isRelocating() {
		return relocating;
	}

	public void setRelocating(boolean relocating) {
		this.relocating = relocating;
	}

	/**
	 * @param monitor the monitor host address" tcp://<ip>:<port>"
	 * @param tstmp true if using a time stamp server
	 */
	public PublisherConnectionManager(List<String> monitors, boolean tstmp) {
		this.context = ZMQ.context(1);
		for (String monitor : monitors) {
			this.monitorsSub.put(monitor, context.socket(ZMQ.SUB));
			//As we received the base port we need to increment the base port to get the sub
			//and init request port
			int basePort = extractBasePort(monitor);
			String portOff = monitor.substring(0, monitor.length()-"xxxx".length());
			this.monitorsSub.get(monitor).connect(portOff+(basePort+2));
			this.s_ID = UUID.randomUUID().toString();
			this.monitorsSub.get(monitor).subscribe("".getBytes());
			this.initReqSockets.put(monitor, context.socket(ZMQ.REQ));
			this.initReqSockets.get(monitor).connect(portOff+(basePort+1));
			this.initReqSockets.get(monitor).setReceiveTimeOut(3000);
			//Init the config state
			this.configState = new PublisherConfigState();
			this.configState.setMonitor(monitor);
			this.configState.setTimeStampServer(tstmp);
			this.configState.setPublisherID(this.s_ID);
			logger.info("Publisher client thread " + s_ID+" Connected to monitor :tcp://" + monitor + ":"+(basePort+1));
	
			if (tstmp) {
				// Init of timestamp socket. Only for benchmarking purposes
				this.tstmpReq = context.socket(ZMQ.REQ);
				this.tstmpReq.connect("tcp://" + monitor + ":5900");
				this.logger.debug("using time stamp server: "+ tstmp);
			}
			this.running = true;
			logger.info(" Publisher " + this.s_ID + " is running");
		}
	}

	/**
	 * @param monitor the host address: tcp://ip:port
	 * @return the port as an int
	 */
	private int extractBasePort(String monitor) {
		String segment = monitor.substring("tcp://".length());
		return Integer.parseInt(segment.substring(segment.indexOf(":")+1));
	}

	/**
	 * Initialize the connection to the exchange the publisher must send the message. It as asks the monitoring
	 * the list of available exchanges.
	 * @param code the code that must sent to the monitor 2 for the first connection and 3 in panic mode
	 * @return 1 if the list of exchanges received is empty, 1 otherwise
	 */
	private int initReq(int code) {
		logger.info("Asking for a new exchange connection to monitor  code "+ code+"...");
		int result = 1;
		for (String initReqKey : initReqSockets.keySet()) {
			initReqSockets.get(initReqKey).send((Integer.toString(code) + "," + s_ID).getBytes(), 0);
			byte[] resp = initReqSockets.get(initReqKey).recv(0);
			if(resp != null) {
				// Code must be 2(first connection) or 3(panic procedure)!
				
				//The answer must be the concatenated list of exchange
				String exchg = new String(resp);
				logger.info("Recieving "+ exchg + " to connect ...");
				if (!exchg.equals("")) {
					try {
						createPublisherSocket(exchg);
					}finally{
					}
					logger.info("Connected to Exchange " + exchg);
					this.s_currentExchange = exchg;
					result = 0;
					break;
				} else {
					logger.info("no exchange available");
					result = 1;
				}
			}
		}
		return result;
	}
	
	/**
	 * Create a connection socket to this exchange address
	 * @param exchange the exchange to connect
	 */
	private void createPublisherSocket(String exchange) {
		this.configState.setExchPub(this.context.socket(ZMQ.XPUB));
		this.configState.getExchPub().setSndHWM(100000);
		this.configState.getExchPub().connect("tcp://" + exchange);
		//Bug #133 add a connect to exchange address for information channel
		this.configState.setExchReq(this.context.socket(ZMQ.REQ));
		this.configState.getExchReq().setSendTimeOut(3000);
		this.configState.getExchReq().setReceiveTimeOut(3000);
		this.configState.getExchReq().connect(getExchangeReqAddress("tcp://" + exchange));
		this.configState.setValid(true);
	}

	/**
	 * @return the exchange address to bind for the request channel.
	 */
	private String getExchangeReqAddress(String exchangeFrontAddress) {
		if(exchangeFrontAddress.contains(":")){
			int basePort =  RoQSerializationUtils.extractBasePort(exchangeFrontAddress);
			String portOff = exchangeFrontAddress.substring(0, exchangeFrontAddress.length() - "xxxx".length());
			String result = portOff + (basePort + 3);
			logger.info("The Request exchange address is: " + result);
			return  result;
		}else{
			throw new IllegalStateException("The address to bind does not contain any :port !");
		}
		
	}

	public void run() {
		logger.debug("Starting the publisher "+this.s_ID + " Thread");
		//0 means that the list of exchange has been received 
		while (initReq(2) != 0) {
			try {
				logger.info("Retrying connection...");
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Register in Pollin 0 position the monitor
		// TODO size depend of the config (replication factor; 3 for the moment)
		ZMQ.Poller items = new ZMQ.Poller(5);
		HashMap<Integer, ZMQ.Socket> HostPolVal = new HashMap<Integer, ZMQ.Socket>();
		int pollval = 0;
		for (ZMQ.Socket monitorSub: monitorsSub.values()) {
			pollval = items.register(monitorSub);
			HostPolVal.put(pollval, monitorSub);
		}			
		
		logger.info("Producer online");
		while (running) {
			items.poll(100);
			for (int i = 0; i < 3; i++) {
				if (items.pollin(i)) { // Info from Monitor i
					String info[] = new String(HostPolVal.get(i).recv(0)).split(",");
					int infoCode = Integer.parseInt(info[0]);
	
					switch (infoCode) {
					case RoQConstant.REQUEST_RELOCATION:
						// Relocation notice
						// Because the message is broadcasting to all publishers we need to filter on ID
						if (info[1].equals(s_ID)) {
							rellocateExchange(info[2]);
						}
						break;
					case RoQConstant.EXCHANGE_LOST:
						// Panic
						if (info[1].equals(s_currentExchange)) {
							logger.warn("Panic, my exchange is lost! " + info[1]);
							closeConnection();
							//Try to reconnect to new exchange - asking to monitor for reallocation
							while (initReq(3) != 0) {
								logger.warn("Exchange lost. Waiting for reallocation...");
								try {
									Thread.sleep(1500);
								} catch (InterruptedException e) {
									logger.error("Error when thread sleeping (re-allocation phase", e);
								}
							}
						}
						break;
					}
				}
			}
		}
		logger.debug("Shutting down the publisher connection");
		for (ZMQ.Socket monitorSub : monitorsSub.values()) {
			monitorSub.close();
		}
		for (ZMQ.Socket initReq : initReqSockets.values()) {
			initReq.close();
		}
	}

	/**
	 * Closes the connection to the current exchange
	 */
	private void closeConnection() {
		this.sendDeconnectionEvent();
		try {
			this.logger.debug("Closing publisher sockets ...");
			this.configState.getExchPub().setLinger(0);
			this.configState.getLock().lock();
			this.configState.getExchPub().close();
			this.configState.setValid(false);
		} finally {
			this.configState.getLock().unlock();
		}
		
	}

	/**
	 * Rellocate the publisher configuration to another exchange
	 * @param exchange the new exchange to relocate the publisher configuration.
	 */
	private void rellocateExchange(String exchange) {
		try{
			this.relocating = true;
			this.sendDeconnectionEvent();
			this.logger.debug("Closing sockets when re-locate the exchange on "+exchange);
			this.configState.getLock().lock();
			this.configState.getExchPub().setLinger(0);
			this.configState.getExchPub().close();
			
			createPublisherSocket(exchange);
			s_currentExchange = exchange;
			this.relocating=false;
			logger.info("Re-allocation order -  Moving to " + exchange);
		}finally{
			this.configState.getLock().unlock();
		}
	}

	/**
	 * Notifies the exchange that this producer is not connected to this exchange.
	 * Then the exchange can update his producer statistic state.
	 * This situation happens when a producer is re-located or just close the connection.
	 */
	private void sendDeconnectionEvent() {
		logger.info("Sending a de-connect event to exchange");
		//TODO Bug #133 replace the initReq in this code by the new exchange Req socket address.
		this.configState.getExchReq().send((Integer.toString(RoQConstant.EVENT_PROD_DECONNECT) + "," + s_ID).getBytes(), 0);
		//The answer must be the concatenated list of exchange
		byte[] bresult =this.configState.getExchReq().recv(0);
		String result="?";
		if(bresult!=null) result = new String(bresult);
		else result = new String("1101");
		if (Integer.parseInt(result) == RoQConstant.OK){
			logger.info(s_ID +" Succesfully disconnected from exchange.");
		}else
			logger.error("Error when disconnecting "+ s_ID +" from exchange, check exchange logs.");
	}

	/**
	 * @return the current configuration from which the {@linkplain PublisherClient} will connect the exchange.
	 */
	public PublisherConfigState getConfiguration() {
		return this.configState;
	}
	
	/**
	 * Stop the connection manager.
	 */
	public void shutDown(){
		this.running=false;
		this.closeConnection();
	}

}