package org.roq.simulation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import jersey.repackaged.com.google.common.collect.ImmutableList;

import org.apache.log4j.Logger;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;

import static com.spotify.docker.client.DockerClient.LogsParameter.STDERR;
import static com.spotify.docker.client.DockerClient.LogsParameter.STDOUT;

public class RoQDockerLauncher {
	
	private DockerClient client = null;
	private static final Logger logger = Logger.getLogger(RoQDockerLauncher.class);
	private ArrayList<String> GCMList = null;
	private ArrayList<String> HCMList = null;
	private ArrayList<String> ZKList = null;
	
	private ContainerConfig configHCM = null;
	private ContainerConfig configGCM = null;
	private ContainerConfig configZK = null;
	
	private String GCMProperties =  System.getenv("ROQPATH") + "/roq-simulation/src/main/resources/GCM-docker.properties";
	private String HCMProperties =  System.getenv("ROQPATH") + "/roq-simulation/src/main/resources/HCM-docker.properties";
	
	private String GCMLog = System.getenv("ROQPATH") + "/roq-simulation/src/main/resources/GCM-docker.log";
	private String HCMLog = System.getenv("ROQPATH") + "/roq-simulation/src/main/resources/HCM-docker.log";
	
	/**
	 * Starts:<br>
	 * 1. The global configuration manager<br>
	 * 2. The local host configuration manager for this host <br>
	 * 3. Adding the local host to global host configuration manager
	 * @param formatDB defined whether the DB must be cleaned.
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
		logger.info("setUp containers");
		
		GCMList = new ArrayList<String>();
		HCMList = new ArrayList<String>();
		ZKList = new ArrayList<String>();
		
		// 1. Start the configuration
		this.client = DefaultDockerClient.builder().uri("http://localhost:80").build();
		
		// Create a docker configuration for GCM, HCM & ZK
				
		// ROQZK
		configZK = ContainerConfig.builder().image("debian:zookeeper").build();
		
		// GCM
		configGCM = ContainerConfig.builder()
			    .image("ubuntu:roqdemo")
			    .attachStderr(true).attachStdout(true)
			    //.cmd("sh", "-c", "while :; do sleep 1; done")
			    .cmd(	"java",
			            "-Djava.library.path=/usr/local/lib",
			            "-cp",
			            "/lib/RoQ2/roq-management/target/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar",
			            "org.roqmessaging.management.launcher.GlobalConfigurationLauncher",
			            "/lib/RoQ2/roq-simulation/src/main/resources/GCM-docker.properties")
			    .build();
		
		
		// HCM
		configHCM = ContainerConfig.builder()
			    .image("ubuntu:roqdemo").volumes("/lib/RoQ")
			    .attachStderr(true).attachStdout(true)
			    .cmd("java",
			            "-Djava.library.path=/usr/local/lib",
			            "-cp",
			            "/lib/RoQ2/roq-management/target/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar",
			            "org.roqmessaging.management.launcher.HostConfigManagerLauncher",
			            "/lib/RoQ2/roq-simulation/src/main/resources/HCM-docker.properties")
			    .build();
		logger.info("Clear logs");
		File gcmFile = new File(GCMLog);
		FileWriter writer = new FileWriter(gcmFile);
		writer.write("");
		writer.close();
		File hcmFile = new File(HCMLog);
		writer = new FileWriter(hcmFile);
		writer.write("");
		writer.close();
		
		logger.info("Launch three containers: ZK GCM HCM");
		launchZK();
		launchGCM();
		launchHCM();
	}
	
	/**
	 * Launch a GCM container
	 * @throws Exception
	 */
	public void launchGCM() throws Exception {
		final ImmutableList.Builder<String> binds = new ImmutableList.Builder<String>();
		binds.add(System.getenv("ROQPATH") + ":/lib/RoQ2");
		
		logger.info("Starting GCM container");
		HostConfig.Builder hostConfig = HostConfig.builder().networkMode("bridge").binds(binds.build());
		ContainerCreation creation = client.createContainer(configGCM);
		client.startContainer(creation.id(), hostConfig.build());
		GCMList.add(creation.id());
		
				
		// Update HCM properties
		File configFile = new File(HCMProperties);
						
		Properties props = new Properties();
	    InputStream input = null;
	    
        input = new FileInputStream( configFile );
	 
        // Try loading properties from the file (if found)
        props.load( input );
	    props.setProperty("zk.address", getZkConnectionString());
	    FileWriter writer = new FileWriter(configFile);
	    props.store(writer, "host settings");
	    writer.close();
	}
	
	/**
	 * Launch a HCM container
	 * @throws Exception
	 */
	public void launchHCM() throws Exception {
		logger.info("Starting HCM container");
		
		final ImmutableList.Builder<String> binds = new ImmutableList.Builder<String>();
		binds.add(System.getenv("ROQPATH") + ":/lib/RoQ2");
		
		HostConfig.Builder hostConfig = HostConfig.builder().networkMode("bridge").binds(binds.build());
		ContainerCreation creation = client.createContainer(configHCM);
		client.startContainer(creation.id(), hostConfig.build());
		HCMList.add(creation.id());
	}
	
	/**
	 * Launch Zk container
	 * @throws Exception
	 */
	public void launchZK() throws Exception {
		logger.info("Starting ZK container");
		
		ContainerCreation creation = client.createContainer(configZK);
		client.startContainer(creation.id());
		
		ZKList.add(creation.id());
		logger.info(creation.id());
		
		// Update GCM properties
		File configFile = new File(GCMProperties);
		
		String zkConnectionString = this.getZkConnectionString();
		
		Properties props = new Properties();
	    InputStream input = null;
	 
        input = new FileInputStream( configFile );

	 
        // Try loading properties from the file (if found)
        props.load( input );
	    props.setProperty("zk.servers", zkConnectionString);
	    FileWriter writer = new FileWriter(configFile);
	    props.store(writer, "host settings");
	    writer.close();
	}
	
	public void pauseZookeeper(int duration) throws DockerException, InterruptedException {
		for (String id: ZKList)
			client.pauseContainer(id);
		for (String id: ZKList) {
			ContainerRestarter restarter = new ContainerRestarter(id, this, duration);
			new Thread(restarter).start();
		}
	}
	
	/**
	 * Zookeeper Restarter
	 * @author benjamin
	 *
	 */
	class ContainerRestarter implements Runnable {
		private RoQDockerLauncher launcher;
		private String id;
		private int timeToSleep;
		
		public ContainerRestarter(String id, RoQDockerLauncher launcher, int timeToSleep) {
			this.launcher = launcher;
			this.id = id;
			this.timeToSleep = timeToSleep;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(timeToSleep);
				launcher.client.unpauseContainer(id);
			} catch (DockerException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		}
	}
	
	/**
	 * Stop a GCM container
	 * @throws Exception
	 */
	public void stopGCMByID(String id) throws Exception {
		logger.info("Writing GCM container log");
		File gcmFile = new File(GCMLog);
		FileWriter writer = new FileWriter(gcmFile, true);
		ContainerInfo info;
		LogStream stream;
		info = client.inspectContainer(id);
		stream = client.logs(info.id(), STDOUT, STDERR);
		writer.append(stream.readFully());
		writer.append("\n\n\n ---------------------------------- \n\n\n");
		writer.close();
		
		logger.info("Stopping GCM container: " + id);
		// Kill container
		client.stopContainer(id, 0);
		// Remove container
		client.removeContainer(id);
		
		// Remove the GCM from the list
		Iterator<String> iter = GCMList.iterator();
		while (iter.hasNext()) {
			if (iter.next().equals(id))
				iter.remove();
		}
	}
	
	public void stopHCMByID(String id) throws IOException, DockerException, InterruptedException {
		logger.info("Writing HCM container log");
		File hcmFile = new File(HCMLog);
		FileWriter writer = new FileWriter(hcmFile, true);
		ContainerInfo info;
		LogStream stream;
		info = client.inspectContainer(id);
		stream = client.logs(info.id(), STDOUT, STDERR);
		writer.append(stream.readFully());
		writer.append("\n\n\n ---------------------------------- \n\n\n");
		writer.close();
		
		logger.info("Stopping HCM container: " + id);
		// Kill container
		client.stopContainer(id, 0);
		// Remove container
		client.removeContainer(id);
		
		// Remove the HCM from the list
		Iterator<String> iter = HCMList.iterator();
		while (iter.hasNext()) {
			if (iter.next().equals(id))
				iter.remove();
		}
	}
	
	/**
	 * Write the logs of the containers (GCM and HCM)
	 * in files
	 * @throws IOException
	 * @throws DockerException
	 * @throws InterruptedException
	 */
	public void saveLogs() throws IOException, DockerException, InterruptedException {
		logger.info("Writing containers LOG");
		File gcmFile = new File(GCMLog);
		FileWriter writer = new FileWriter(gcmFile, true);
		ContainerInfo info;
		LogStream stream;
		for (String id : GCMList) {
			info = client.inspectContainer(id);
			stream = client.logs(info.id(), STDOUT, STDERR);
			writer.append(stream.readFully());
			writer.append("\n\n\n ---------------------------------- \n\n\n");
		}
		writer.close();
		File hcmFile = new File(HCMLog);
		writer = new FileWriter(hcmFile, true);
		for (String id : HCMList) {
			info = client.inspectContainer(id);
			stream = client.logs(info.id(), STDOUT, STDERR);
			writer.append(stream.readFully());
			writer.append("\n\n\n ---------------------------------- \n\n\n");
		}
		writer.close();
	}
	
	
	/**
	 * Return the connection String for ZooKeeper
	 * @return zkConnectionString
	 * @throws Exception
	 */
	public String getZkConnectionString() throws Exception {
		String zkConnectionString = "";
		ContainerInfo info;
		int i = 0;
		for (String id : ZKList) {
			info = client.inspectContainer(id);
			if (i > 0)
				zkConnectionString += ",";
			zkConnectionString += info.networkSettings().ipAddress();
			logger.info(info);
		}
		return zkConnectionString;
	}

	/**
	 * Stops all the involved elements
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
		saveLogs();
		logger.info("Stopping containers");		
		for (String id : HCMList) {
			// Kill container
			client.stopContainer(id, 0);
			// Remove container
			client.removeContainer(id);
		}
		for (String id : GCMList) {
			// Kill container
			client.stopContainer(id, 0);
			// Remove container
			client.removeContainer(id);
		}
		for (String id : ZKList) {
			client.stopContainer(id, 0);
			client.removeContainer(id);
		}
		client.close();
	}

	/**
	 * Initialize a docker test configuration
	 * with three containers
	 * 1 GCM, 1 HCM & 1 ZooKeeper
	 */
	public static void main(String[] args) {
		RoQDockerLauncher launcher = null;
		launcher = new RoQDockerLauncher();
		ShutDownHook hook = new ShutDownHook(launcher);
		Runtime.getRuntime().addShutdownHook(hook);
		try {
			launcher.setUp();
			while (true) {
				Thread.sleep(500);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Class ShutDownHook
	 * <p>
	 * Description: provides a hook called when we stop the launcher
	 * 
	 * @author sskhiri
	 */
	private static class ShutDownHook extends Thread {
		private RoQDockerLauncher launcher = null;

		/**
		 * Set a hook to shutdown containers
		 */
		public ShutDownHook(RoQDockerLauncher launcher2) {
			this.launcher = launcher2;
		}

		public void run() {
			System.out.println("Running Clean Up...");
			try {
				this.launcher.tearDown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Return GCM Listsu
	 */
	public ArrayList<String> getGCMList() {
		return GCMList;
		
	}
	
	/**
	 * Return GCM List
	 */
	public ArrayList<String> getHCMList() {
		return HCMList;
		
	}
	
	/**
	 * Return GCM List
	 */
	public ArrayList<String> getZKList() {
		return ZKList;
		
	}

	public ArrayList<String> getHCMAddressList() 
			throws DockerException, InterruptedException {
		ArrayList<String> list = new ArrayList<String>();
		ContainerInfo info;
		for (String id : HCMList) {
			info = client.inspectContainer(id);
			list.add(info.networkSettings().ipAddress());
			logger.info(info);
		}
		return list;
	}
	
	public ArrayList<String> getGCMAddressList() 
			throws DockerException, InterruptedException {
		ArrayList<String> list = new ArrayList<String>();
		ContainerInfo info;
		for (String id : GCMList) {
			info = client.inspectContainer(id);
			list.add(info.networkSettings().ipAddress());
			logger.info(info);
		}
		return list;
	}

}
