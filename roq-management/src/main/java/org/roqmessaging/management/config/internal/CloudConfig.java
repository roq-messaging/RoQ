package org.roqmessaging.management.config.internal;

import java.io.Serializable;

public class CloudConfig implements Serializable {
	
	private static final long serialVersionUID = 1L;
	//Define whether RoQ should use a cloud or not
	public boolean inUse = false;
	//The user  for the cloud server
	public String  user;
	//PASSWD for the cloud server
	public String  password;
	//The gateway for identifying the subnetwork gateway
	public String  gateway;
	//API end point for the cloud server
	public String  endpoint;
	
	// Auto-generated code below
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((endpoint == null) ? 0 : endpoint.hashCode());
		result = prime * result + ((gateway == null) ? 0 : gateway.hashCode());
		result = prime * result + (inUse ? 1231 : 1237);
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CloudConfig other = (CloudConfig) obj;
		if (endpoint == null) {
			if (other.endpoint != null)
				return false;
		} else if (!endpoint.equals(other.endpoint))
			return false;
		if (gateway == null) {
			if (other.gateway != null)
				return false;
		} else if (!gateway.equals(other.gateway))
			return false;
		if (inUse != other.inUse)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}
}
