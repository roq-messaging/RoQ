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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.log4j.Logger;

/**
 * Class RoQSerializationUtils
 * <p>
 * Description: Set of utility methods that must be used in an object that the
 * developer must instanciate.
 * 
 * @author sskhiri
 */
public class RoQSerializationUtils {
	private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

	/**
	 * @param array
	 *            the array to serialise
	 * @return the serialized version
	 */
	public synchronized <T> byte[] serialiseObject(T object) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(object);
			out.close();
			return bos.toByteArray();
		} catch (IOException e) {
			logger.error("Error when openning the IO", e);
		}

		return null;
	}

	/**
	 * @param serialised
	 *            the array of byte
	 * @return the array list from the byte array
	 */
	public synchronized <T> T deserializeObject(byte[] serialised) {
		try {
			// Deserialize from a byte array
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serialised));
			@SuppressWarnings("unchecked")
			T unserialised = (T) in.readObject();
			in.close();
			return unserialised;
		} catch (Exception e) {
			logger.error("Error when unserialiasing the array", e);
		}
		return null;
	}

	/**
	 * @param monitor
	 *            the host address: tcp://ip:port
	 * @return the port as an int
	 */
	public static  int extractBasePort(String monitor) {
		String segment = monitor.substring("tcp://".length());
		return Integer.parseInt(segment.substring(segment.indexOf(":") + 1));
	}

	/**
	 * @param str
	 *            the string to encode
	 * @return the corresponding encoded byte array
	 */
	public static byte[] stringToBytesUTFCustom(String str) {
		byte[] b = new byte[str.length() << 1];
		for (int i = 0; i < str.length(); i++) {
			char strChar = str.charAt(i);
			int bpos = i << 1;
			b[bpos] = (byte) ((strChar & 0xFF00) >> 8);
			b[bpos + 1] = (byte) (strChar & 0x00FF);
		}
		return b;
	}

	/**
	 * @param bytes
	 *            the encoded byte array
	 * @return the decoded string
	 */
	public static String bytesToStringUTFCustom(byte[] bytes) {
		char[] buffer = new char[bytes.length >> 1];
		for (int i = 0; i < buffer.length; i++) {
			int bpos = i << 1;
			char c = (char) (((bytes[bpos] & 0x00FF) << 8) + (bytes[bpos + 1] & 0x00FF));
			buffer[i] = c;
		}
		return new String(buffer);
	}

}
