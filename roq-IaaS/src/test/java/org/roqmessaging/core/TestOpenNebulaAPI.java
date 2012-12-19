package org.roqmessaging.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestOpenNebulaAPI {

	@Test
	public void testCreate() {
		OpenNebulaAPI nebulaAPI=new OpenNebulaAPI();
		nebulaAPI.createInstance("127.0.0.1");
	}

}
