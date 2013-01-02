package org.roqmessaging.core;

import org.junit.Ignore;
import org.junit.Test;

public class TestOpenNebulaAPI {
	private OpenNebulaAPI nebulaAPI =new OpenNebulaAPI("roq", "roq", "http://inferno.local:2633/RPC2");

	@Ignore @Test
	public void testVM() {
		int vmID = nebulaAPI.createInstance("127.0.0.1");
		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}

		deleteVM(vmID);
	}
	
	public void deleteVM(int vmID) {
		nebulaAPI.deleteInstance(vmID);
	}
	
	@Ignore @Test
	public void testCleanVM() throws Exception {
		for (int i = 0; i <4; i++) {
			nebulaAPI.createInstance("127.0.0.1");
		}
		try {
			Thread.sleep(4000);
		} catch (Exception e){}
		nebulaAPI.cleanAllInstances();
	}

}
