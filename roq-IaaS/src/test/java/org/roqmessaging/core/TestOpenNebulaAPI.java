package org.roqmessaging.core;

import org.junit.Test;

public class TestOpenNebulaAPI {
	private OpenNebulaAPI nebulaAPI =new OpenNebulaAPI();

 @Test
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

}
