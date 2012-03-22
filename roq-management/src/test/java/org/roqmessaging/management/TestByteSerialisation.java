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
package org.roqmessaging.management;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Class TestByteSerialisation
 * <p>
 * Description: Test the serialisation method used in the global config manager.
 * 
 * @author sskhiri
 */
public class TestByteSerialisation {
	private Logger logger = Logger.getLogger(TestByteSerialisation.class);

	@Test
	public void test() {
		ArrayList<String> array = new ArrayList<String>();
		array.add("127.0.0.1");
		array.add("127.0.0.2");
		array.add("127.0.0.3");
		byte[] serialised = serialise(array);
		ArrayList<String> arrayTest = deserializeQuotes(serialised);
		assert arrayTest.get(0).equals(array.get(0));
		assert arrayTest.get(1).equals(array.get(1));
		logger.info("position 1 " + arrayTest.get(0));
		logger.info("position 2 " + arrayTest.get(1));
	}
	
	@Test
	public void testGenerics() {
		logger.info("Test generics");
		ArrayList<String> array = new ArrayList<String>();
		array.add("127.0.0.1");
		array.add("127.0.0.2");
		array.add("127.0.0.3");
		byte[] serialised = serialiseObject(array);
		ArrayList<String> arrayTest = deserializeObject(serialised);
		assert arrayTest.get(0).equals(array.get(0));
		assert arrayTest.get(1).equals(array.get(1));
		logger.info("position 1 " + arrayTest.get(0));
		logger.info("position 2 " + arrayTest.get(1));
		
		
	}

	/**
	 * @param array
	 *            the array to serialise
	 * @return the serialized version
	 */
	private byte[] serialise(ArrayList<String> array) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(array);
			out.close();
			return bos.toByteArray();
		} catch (IOException e) {
			logger.error("Error when openning the IO", e);
		}

		return null;
	}

	public ArrayList<String> deserializeQuotes(byte[] serialised) {
		try {
			// Deserialize from a byte array
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serialised));
			@SuppressWarnings("unchecked")
			ArrayList<String> unserialised = (ArrayList<String>) in.readObject();
			in.close();
			return unserialised;
		} catch (Exception e) {
			logger.error("Error when unserialiasing the array", e);
		}
		return null;
	}
	
	/**
	 * @param array
	 *            the array to serialise
	 * @return the serialized version
	 */
	private<T> byte[] serialiseObject(T object) {
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
	 * @param serialised the array of byte
	 * @return the array list from the byte array
	 */
	public <T> T deserializeObject(byte[] serialised) {
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

}
