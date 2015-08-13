/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 *  /!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\
 *  This file comes from the STORM project, the sources are available a this
 *  address: https://github.com/apache/storm
 *  /!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\
 *
 *
 */
package org.roqmessaging.utils;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 * A simple, durable, atomic K/V database. *Very inefficient*, should only be used for occasional reads/writes.
 * Every read/write hits disk.
 */
public class LocalState {
	private Logger logger = Logger.getLogger(LocalState.class);

    private VersionedStore _vs;
    
    public LocalState(String backingDir) throws IOException {
        logger.debug("New Local State for {}: " + backingDir);
        _vs = new VersionedStore(backingDir);
    }

    public synchronized Map<Object, Object> snapshot() throws IOException {
        int attempts = 0;
        while(true) {
            try {
                return deserializeLatestVersion();
            } catch (IOException e) {
                attempts++;
                if (attempts >= 10) {
                    throw e;
                }
            }
        }
    }

    private Map<Object, Object> deserializeLatestVersion() throws IOException {
        String latestPath = _vs.mostRecentVersionPath();
        Map<Object, Object> result = new HashMap<Object, Object>();
        if (latestPath != null) {
            byte[] serialized = FileUtils.readFileToByteArray(new File(latestPath));
            if (serialized.length == 0) {
                logger.warn("LocalState file '{}' contained no data, resetting state: " + latestPath);
            } else {
                result = javaDeserialize(serialized, Map.class);
            }
        }
        return result;
    }

    public Object get(Object key) throws IOException {
        return snapshot().get(key);
    }
    
    public synchronized void put(Object key, Object val) throws IOException {
        put(key, val, true);
    }

    public synchronized void put(Object key, Object val, boolean cleanup) throws IOException {
        Map<Object, Object> curr = snapshot();
        curr.put(key, val);
        persist(curr, cleanup);
    }

    public synchronized void remove(Object key) throws IOException {
        remove(key, true);
    }

    public synchronized void remove(Object key, boolean cleanup) throws IOException {
        Map<Object, Object> curr = snapshot();
        curr.remove(key);
        persist(curr, cleanup);
    }

    public synchronized void cleanup(int keepVersions) throws IOException {
        _vs.cleanup(keepVersions);
    }
    
    private void persist(Map<Object, Object> val, boolean cleanup) throws IOException {
        byte[] toWrite = javaSerialize(val);
        String newPath = _vs.createVersion();
        File file = new File(newPath);
        FileUtils.writeByteArrayToFile(file, toWrite);
        if (toWrite.length != file.length()) {
            throw new IOException("Tried to serialize " + toWrite.length + 
                    " bytes to " + file.getCanonicalPath() + ", but " +
                    file.length() + " bytes were written.");
        }
        _vs.succeedVersion(newPath);
        if(cleanup) _vs.cleanup(4);
    }
    
    public static byte[] javaSerialize(Object obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T javaDeserialize(byte[] serialized, Class<T> clazz) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object ret = ois.readObject();
            ois.close();
            return (T) ret;
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        } catch(ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
