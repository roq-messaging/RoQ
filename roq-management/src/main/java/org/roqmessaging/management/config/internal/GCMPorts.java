package org.roqmessaging.management.config.internal;

import java.util.HashMap;

public class GCMPorts {
	
	private HashMap<String, Integer> ports = new HashMap<String, Integer>();
	
	public GCMPorts() {
	}
	public GCMPorts(int basePort) {
		fill(basePort);
	}
	private void clear() {
		ports.clear();
	}
	private void fill(int basePort) {
		ports.put("GlobalConfigurationManager.interface", basePort);
		ports.put("GlobalConfigurationManager.shutDown",  basePort+1);
		ports.put("GlobalConfigTimer.pub",                basePort+2);
		ports.put("MngtController.interface",             basePort+3);
		ports.put("MngtController.shutDown",              basePort+4);
		ports.put("MngtControllerTimer.pub",              basePort+5);
	}
	public void setBasePort(int basePort) {
		clear();
		fill(basePort);
	}
	public void set(String name, int port) {
		ports.put(name, port);
	}
	public int get(String name) {
		return ports.get(name);
	}
}
