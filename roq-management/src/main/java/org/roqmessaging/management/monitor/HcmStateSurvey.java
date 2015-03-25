package org.roqmessaging.management.monitor;

public class HcmStateSurvey {
	
	
	/**
	 * M1.1: Get the worker heartbeats in a map
	 *   (defn read-worker-heartbeats
	 *		"Returns map from worker id to heartbeat"
	 *		[conf]
	 *		(let [ids (my-worker-ids conf)]
	 *		(into {}
	 *		(dofor [id ids]
	 *		[id (read-worker-heartbeat conf id)]))
	 *		))
	 * 
	 *  
	 */
	
	/**
	 * M1.2: Read the hb of one worker 
	 */
	
	/**
	 *  M2.0: Start a process
	 */
	 // M3.0 Recovery Mecanism
	 // 1. to kill are those in allocated that are dead or disallowed
	 // 2. kill the ones that should be dead
	 // - read pids, kill -9 and individually remove file
	 // - rmr heartbeat dir, rmdir pid dir, rmdir id dir (catch exception and log)
	 // 3. of the rest, figure out what assignments aren't yet satisfied
	 // 4. generate new worker ids, write new "approved workers" to LS
	 // 5. create local dir for worker id
	 // 5. launch new workers (give worker-id, port, and supervisor-id)
	 // 6. wait for workers launch
}
