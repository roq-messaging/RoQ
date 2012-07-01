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
package org.roqmessaging.core;

import org.roqmessaging.core.interfaces.IStoppable;

/**
 * Interface RoQConstant
 * <p> Description: This interface defines the different message types that are exchanges between RoQ elements.
 * 
 * @author Sabri Skhiri
 */
public interface RoQConstant {
	
	//Constants used at monitor level to communicate with elements
	
	/**
	 * Statistic channels on which the  stat are sent
	 */
	public static int CHANNEL_STAT =0;
	
	/**
	 * OK.
	 */
	public static int OK =0;
	
	/**
	 * Communication channel between the exchange and the monitor
	 */
	public static int CHANNEL_EXCHANGE=1;
	
	/**
	 * Channel to configure the initalisation of elements
	 */
	public static int CHANNEL_INIT_PRODUCER=2;
	
	/**
	 * Channel to configure the initalisation of elements
	 */
	public static int CHANNEL_INIT_SUBSCRIBER=1;
	
	
	/**
	 * From monitor to producer
	 * Prod relocation: new exchg address
	 *  Sends“1,newExchg“	
	 */
	public static int REQUEST_RELOCATION = 1;
	
	/**
	 * From monitor to listener
	 *  Sends the newly added exchange to subscribers.
	 *  Sends“1,newExchg“	
	 */
	public static int REQUEST_UPDATE_EXCHANGE_LIST = 1;
	
	/**
	 * Send an init request code.
	 */
	public static int INIT_REQ =2;
	
	/**
	 * Recived by a publisher when its exchange is lost
	 */
	public static int EXCHANGE_LOST=2;
	
	/**
	 * Debug code
	 */
	public static int DEBUG =3;
	
	/**
	 * From  Exchange
	 * Notifies the most productive producer to re-locate.
	 *  Sends “4,reqID,reqIDcount,msgCount“
	 */
	public static int EVENT_MOST_PRODUCTIVE = 4;
	
	/**
	 *  From  Exchange
	 * Still alive signal
	 *  Sends “5,IPaddress"
	 */
	public static int EVENT_HEART_BEAT= 5;
	
	/**
	 *  From  Exchange
	 * This event is sent by the Exchange when it stops.
	 * Sends “6,IPaddress”
	 */
	public static int EVENT_EXCHANGE_SHUT_DONW = 6;
	
	/**
	 * Event sent from subscriber to monitor.
	 * “31,totalreceived”
	 */
	public static int STAT_TOTAL_RCVD= 31;
	
	/**
	 * Event sent from producer every minute.
	 * "12, s_ID , sent
	 */
	public static int STAT_PUB_MIN = 12;
	
	/**
	 * Unique way to identify the Stat that send the stat.
	 */
	public static int STAT_EXCHANGE_ID= 20;
	
	/**
	 * Stat event sent every minute from exchanges to monitor.
	 * “21,minute,totalProcessed,processed,totalthroughput,throughput,nbProd”
	 */
	public static int STAT_EXCHANGE_MIN= 21;
	
	/**
	 * Stat event sent every minute from exchanges to monitor.
	 * “22,cpu, memory”
	 */
	public static int STAT_EXCHANGE_OS_MIN= 22;
	
	/**
	 * Stat event sent every minute to describe the queue state.
	 * “23,QName, number of exchange registered, number of total producer, total througput on Q”
	 */
	public static int STAT_Q= 23;
	
	/**
	 * Stat sent from producer to monitor.
	 * “11,totalSent”
	 */
	public static int STAT_TOTAL_SENT= 11;
	
	/**
	 * GLobal Configuration request to create a queue.
	 */
	public static int CONFIG_CREATE_QUEUE = 1002;
	
	/**
	 * GLobal Configuration request to create a queue 
	 */
	public static int CONFIG_CREATE_QUEUE_OK = 1003;
	
	/**
	 * GLobal Configuration request to create a queue 
	 */
	public static int CONFIG_CREATE_QUEUE_FAIL = 1004;

	/**
	 * Global configuration request to create a new host manager configuration entry
	 */
	public static int CONFIG_ADD_HOST = 1005;
	
	/**
	 * Global configuration request to remove a host manager configuration entry
	 */
	public static int CONFIG_REMOVE_HOST = 1006;
	
	/**
	 * Global configuration request to remove a host manager configuration entry
	 */
	public static int CONFIG_GET_HOST_BY_QNAME = 1007;

	/**
	 * Remove Queue request
	 */
	public static int CONFIG_REMOVE_QUEUE = 1008;
	
	
	/**
	 * create Exchange request
	 */
	public static int CONFIG_CREATE_EXCHANGE = 1009;
	
	/**
	 * Create Exchange request
	 */
	public static int CONFIG_REMOVE_EXCHANGE = 1010;
	
	
	/**
	 * Send to shut donw a {@linkplain IStoppable} element
	 */
	public static int SHUTDOWN_REQUEST = 1100;
	
	/**
	 * Failing constant
	 */
	public static int FAIL = 1101;
	
	/*Operations for configuration management*/
	
	/**
	 * Sent by the Global config manager to the Mngt server.
	 * Infocode
	 * in a second message of the same envelope :  map(Q Name, host)
	 */
	public static int MNGT_UPDATE_CONFIG = 1500;


	/**
	 *  Request command that can be sent to the Global ConfigManager to get the Monitor and
	 *   Stat Monitor.
	 *   Request: "2000, QName"
	 *   Answer:"monitor address, Statistic monitor host on the subscribing port" while the
	 *    CONFIG_GET_HOST_BY_QNAME  return the stat monitor port on the publioshing port
	 */
	public static int BSON_CONFIG_GET_HOST_BY_QNAME = 2000;
	
	/**
	 *  Request command that can be sent to the Configuration server
	 *   Request: "2001, QName"
	 *   Answer: 
	 *   RESULT, OK
	 *   COMMENT, "The reason why it fails". This comment is not present in case of success.
	 *   This request will definitively remove the queue from the global configuration manager if it runs
	 *   and from the Management server DB.
	 */
	public static int BSON_CONFIG_REMOVE_QUEUE = 2001;
	
	/**
	 *  Request command that can be sent to the Configuration server
	 *   Request: "2002, QName"
	 *   Answer: 
	 *   RESULT, OK
	 *   COMMENT, "The reason why it fails". This comment is not present in case of success.
	 *   This request will stop the queue, i.e. removing the queue at the Global configuration server only.
	 */
	public static int BSON_CONFIG_STOP_QUEUE = 2002;
	
	/**
	 *  Request command that can be sent to the Configuration server
	 *   Request: "2003, QName, Host"
	 *   Answer: 
	 *   RESULT, OK
	 *   COMMENT, "The reason why it fails". This comment is not present in case of success.
	 *   This request will start the queue, i.e. creating the queue at the Global configuration server only.
	 */
	public static int BSON_CONFIG_START_QUEUE = 2003;
	
	/**
	 *  Request command that can be sent to the Configuration server
	 *   Request: "2004, QName, Host"
	 *   Answer: 
	 *   RESULT, OK
	 *   COMMENT, "The reason why it fails". This comment is not present in case of success.
	 *   This request will start an exchange on the specified host for the specified Q..
	 */
	public static int BSON_CONFIG_CREATE_XCHANGE = 2004;
	
	/**
	 * Used by the management server to broadcast configuration.
	 */
	public static String BSON_QUEUES = "Queues";
	

	/**
	 * Used by the management server to broadcast configuration.
	 */
	public static String BSON_HOSTS = "Hosts";
	
	/**
	 * Used by the Global  management  to answer the get host by QName request.
	 */
	public static String BSON_MONITOR_HOST = "Monitor_host";
	
	/**
* Used by the Global  management  to answer the get host by QName request.
	 */
	public static String BSON_STAT_MONITOR_HOST = "Stat_Monitor_host";

	/**
	 * ID of the host manager address array maintained in the GCM state
	 */
	public static String CACHE_HOST_MNGER_ADDR="host.manager.address";
	
	/**
	 * ID of the queue location  map maintained in the GCM state
	 */
	public static String CACHE_HOST_MNGER_MAP="host.manager.map";
	
	/**
	 * ID of the host manager map maintained in the GCM state
	 */
	public static String CACHE_Q_HOST_LOCATION="queue.host.location";
	
	/**
	 * ID of the queue  monitor map maintained in the GCM state
	 */
	public static String CACHE_Q_MONITOR_MAP="queue.monitor.map";
	
	/**
	 * ID of the  queue  Statistic monitor map maintained in the GCM state
	 */
	public static String CACHE_Q_MONITOR_STAT_MAP="queue.monitor.stat.map";
	
}
