/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.store;

import java.io.IOException;
import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.writer.CacheWriterManager;

public class LegacyStoreWrapper implements Store {

    private final Store memory;
    private final Store disk;
    private final RegisteredEventListeners eventListeners;
    private final CacheConfiguration config;
    
    public LegacyStoreWrapper(Store memory, Store disk, RegisteredEventListeners eventListeners, CacheConfiguration config) {
        this.memory = memory;
        this.disk = disk;
        this.eventListeners = eventListeners;
        this.config = config;
    }

    public boolean bufferFull() {
        if (disk == null) {
            return false;
        } else {
            return disk.bufferFull();
        }
    }

    public boolean containsKey(Object key) {
        if (key instanceof Serializable && (disk !=  null)) {
            return disk.containsKey(key) || memory.containsKey(key);
        } else {
            return memory.containsKey(key);
        }
    }

    public boolean containsKeyInMemory(Object key) {
        return memory.containsKey(key);
    }

    public boolean containsKeyOnDisk(Object key) {
        if (disk != null) {
            return disk.containsKey(key);
        } else {
            return false;
        }
    }

    public void dispose() {
        memory.dispose();
        if (disk != null) {
            disk.dispose();
        }
    }

    public void expireElements() {
        Object[] keys = memory.getKeyArray();

        for (Object key : keys) {
            Element element = memory.getQuiet(key);
            if (element != null) {
                if (element.isExpired(config)) {
                    Element e = remove(key);
                    if (e != null) {
                        eventListeners.notifyElementExpiry(e, false);
                    }
                }
            }
        }

        //This is called regularly by the expiry thread, but call it here synchronously
        if (disk != null) {
            disk.expireElements();
        }
    }

    public void flush() throws IOException {
        memory.flush();
        if (disk != null) {
            disk.flush();
        }
    }

    public Element get(Object key) {
        Element e = memory.get(key);
        if (e == null && disk != null) {
            e = disk.get(key);
            if (e != null) {
                memory.put(e);
            }
        }
        return e;
    }

    public Policy getInMemoryEvictionPolicy() {
        return memory.getInMemoryEvictionPolicy();
    }

    public int getInMemorySize() {
        return memory.getSize();
    }

    public long getInMemorySizeInBytes() {
        return memory.getInMemorySizeInBytes();
    }

    public Object getInternalContext() {
        return null;
    }

    public Object[] getKeyArray() {
        if (disk == null) {
            return memory.getKeyArray();
        } else {
            Object[] m = memory.getKeyArray();
            Object[] d = disk.getKeyArray();
    
            Object[] c = new Object[m.length + d.length];
            
            System.arraycopy(m, 0, c, 0, m.length);
            System.arraycopy(d, 0, c, m.length, d.length);
            
            return c;
        }
    }

    public int getOnDiskSize() {
        if (disk != null) {
            return disk.getSize();
        } else {
            return 0;
        }
    }

    public long getOnDiskSizeInBytes() {
        if (disk != null) {
            return disk.getOnDiskSizeInBytes();
        } else {
            return 0;
        }
    }

    public Element getQuiet(Object key) {
        Element e = memory.get(key);
        if (e == null && disk != null) {
            e = disk.get(key);
        }
        return e;
    }

    public int getSize() {
        if (disk != null) {
            return memory.getSize() + disk.getSize();
        } else {
            return memory.getSize();
        }
    }

    public Status getStatus() {
        return memory.getStatus();
    }

    public int getTerracottaClusteredSize() {
        return 0;
    }

    public boolean isCacheCoherent() {
        return false;
    }

    public boolean isClusterCoherent() {
        return false;
    }

    public boolean isNodeCoherent() {
        return false;
    }

    public boolean put(Element element) throws CacheException {
        if (element == null) {
            return false;
        }
        
        boolean notOnDisk = !containsKeyOnDisk(element.getObjectKey());
        return memory.put(element) && notOnDisk;
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        boolean notOnDisk = !containsKey(element.getObjectKey());
        return memory.putWithWriter(element, writerManager) && notOnDisk;
    }

    public Element remove(Object key) {
        Element m = memory.remove(key);
        if (disk != null && key instanceof Serializable) {
            Element d = disk.remove(key);
            if (m == null) {
                return d;
            }
        }
        return m;
    }

    public void removeAll() throws CacheException {
        memory.removeAll();
        if (disk != null) {
            disk.removeAll();
        }
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        Element m = memory.removeWithWriter(key, writerManager);
        if (disk != null && key instanceof Serializable) {
            Element d = disk.removeWithWriter(key, writerManager);
            if (m == null) {
                return d;
            }
        }
        return m;
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        memory.setInMemoryEvictionPolicy(policy);
    }

    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Store getDiskStore() {
        return disk;
    }

    public Store getMemoryStore() {
        return memory;
    }
}
