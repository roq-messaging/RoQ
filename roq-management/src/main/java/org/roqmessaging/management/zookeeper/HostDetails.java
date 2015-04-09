package org.roqmessaging.management.zookeeper;

public class HostDetails {
	
	private String address;
	
	public HostDetails() {
		this("");
	}
	
	public HostDetails(String address) {
		this.address = address;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
}
