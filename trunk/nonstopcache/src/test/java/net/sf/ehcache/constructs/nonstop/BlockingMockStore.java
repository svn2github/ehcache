/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.constructs.nonstop;

import java.io.IOException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * All operations in this Store never return
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class BlockingMockStore implements Store {

    public boolean bufferFull() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public boolean containsKey(Object key) {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public boolean containsKeyInMemory(Object key) {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public boolean containsKeyOnDisk(Object key) {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public void dispose() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

    public void expireElements() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

    public void flush() throws IOException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

    public Element get(Object key) {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public Policy getInMemoryEvictionPolicy() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public int getInMemorySize() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return 0;
    }

    public long getInMemorySizeInBytes() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return 0;
    }

    public Object getInternalContext() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public Object[] getKeyArray() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public int getOnDiskSize() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return 0;
    }

    public long getOnDiskSizeInBytes() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return 0;
    }

    public Element getQuiet(Object key) {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public int getSize() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return 0;
    }

    public Status getStatus() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public int getTerracottaClusteredSize() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return 0;
    }

    public boolean isCacheCoherent() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public boolean isClusterCoherent() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public boolean isNodeCoherent() {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public boolean put(Element element) throws CacheException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public Element remove(Object key) {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public void removeAll() throws CacheException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

    public Element removeElement(Element element) throws NullPointerException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return false;
    }

    public Element replace(Element element) throws NullPointerException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }
        return null;
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

}
