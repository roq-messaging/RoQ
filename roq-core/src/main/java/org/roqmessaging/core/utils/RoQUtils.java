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
package org.roqmessaging.core.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;

/**
 * Class RoQUtils
 * <p>
 * Description: Utility class for the roq core system.
 * 
 * @author Sabri Skhiri
 */
public class RoQUtils {

	private static RoQUtils instance = null;

	/**
	 * @return The singleton instance.
	 */
	public static RoQUtils getInstance() {
		if (instance == null) {
			instance = new RoQUtils();
		}
		return instance;
	}

	/**
	 * Returns the current local IP address or an empty string in error case /
	 * when no network connection is up.
	 * <p>
	 * The current machine could have more than one local IP address so might
	 * prefer to use {@link #getAllLocalIPs() } or
	 * {@link #getAllLocalIPs(java.lang.String) }.
	 * <p>
	 * If you want just one IP, this is the right method and it tries to find
	 * out the most accurate (primary) IP address. It prefers addresses that
	 * have a meaningful dns name set for example.
	 * 
	 * @return Returns the current local IP address or an empty string in error
	 *         case.
	 * @since 0.1.0
	 */
	public String getLocalIP() {
		try {
		String ipOnly = InetAddress.getLocalHost().getHostAddress().toString();
			Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
			if (nifs == null)
				return "";
			while (nifs.hasMoreElements()) {
				NetworkInterface nif = nifs.nextElement();
				// We ignore subinterfaces - as not yet needed.

				if (!nif.isLoopback() && nif.isUp() && !nif.isVirtual()) {
					Enumeration<InetAddress> adrs = nif.getInetAddresses();
					while (adrs.hasMoreElements()) {
						InetAddress adr = adrs.nextElement();
						if (adr != null && !adr.isLoopbackAddress()
								&& (nif.isPointToPoint() || !adr.isLinkLocalAddress())) {
							String adrIP = adr.getHostAddress();
							String adrName;
							if (nif.isPointToPoint()) // Performance issues
														// getting hostname for
														// mobile internet
														// sticks
								adrName = adrIP;
							else
								adrName = InetAddress.getLocalHost().getCanonicalHostName();

							if (!adrName.equals(adrIP))
								return checkIPV6(adrIP);
							else
								ipOnly = adrIP;
						}
					}
				}
			}
			return ipOnly;
		} catch (SocketException ex) {
			return "127.0.0.1";
		} catch (UnknownHostException e) {
			return "127.0.0.1";
		}
	}
	
	/**
	 * Allow to get the IP address for a specific interface
	 * Added for issue #65
	 * @param netwInterface the name of the network interface
	 * @return the IP address of this interface
	 * @throws IllegalStateException when the network interface does not exist
	 */
	public String getLocalIP(String netwInterface) throws IllegalStateException {
		try {
			Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
			if (nifs == null)
				return "";

			while (nifs.hasMoreElements()) {
				NetworkInterface nif = nifs.nextElement();
				// We ignore subinterfaces - as not yet needed.
				if (!nif.isLoopback() && nif.isUp() && !nif.isVirtual()) {
					Enumeration<InetAddress> adrs = nif.getInetAddresses();
					if (netwInterface.equalsIgnoreCase(nif.getName())) {
						while (adrs.hasMoreElements()) {
							InetAddress adr = adrs.nextElement();
							if (adr != null && !adr.isLoopbackAddress()
									&& (nif.isPointToPoint() || !adr.isLinkLocalAddress())) {
								return checkIPV6(adr.getHostAddress());
							}
						}
					}
				}
			}
			return null;
		} catch (SocketException ex) {
			return "127.0.0.1";
		}
	}

	public  String getFileStamp() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("GMT+2:00"));
		return df.format(new Date());
	}

	/**
	 * @param targetAddress the target address 
	 * @return the same address but without the % of the IP v6 format
	 */
	public String checkIPV6(String targetAddress) {
		if(targetAddress.indexOf("%")>0){
			return targetAddress.substring(0, targetAddress.indexOf("%"));
		}
		return targetAddress;
	}
	

}
