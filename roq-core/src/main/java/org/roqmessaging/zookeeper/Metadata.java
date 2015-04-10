package org.roqmessaging.zookeeper;

public class Metadata {
	public static class HCM {
		// address format: "x.y.z"
		public String address;
		public HCM(String address) {
			this.address = address;
		}
		
		public String zkNodeString() {
			return address.replace("/", "").replace(":", "");
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HCM other = (HCM) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			return true;
		}
	}
	
	public static class Queue {
		public String name;
		public Queue(String name) {
			this.name = name;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Queue other = (Queue) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	
	public static class Monitor {
		// address format: "tcp://x.y.z:port"
		public String address;
		public Monitor(String address) {
			this.address = address;
		}
		
		public String zkNodeString() {
			return address.replace("/", "").replace(":", "");
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Monitor other = (Monitor) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			return true;
		}
	}
	
	public static class Exchange {
		public String address;
		public Exchange(String address) {
			this.address = address;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Exchange other = (Exchange) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			return true;
		}
	}
	
	public static class StatMonitor {
		// address format: "tcp://x.y.z:port"
		public String address;
		public StatMonitor(String address) {
			this.address = address;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StatMonitor other = (StatMonitor) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			return true;
		}
	}
	
	// Private constructor because Metadata is just a namespace.
	private Metadata() {}
}
