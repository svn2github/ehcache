/**
 *  Copyright Terracotta, Inc.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.writer.CacheWriterManager;
import org.terracotta.context.annotations.ContextChild;

/**
 * A wrapper to convert a legacy pair of stores into a new style compound store.
 *
 * @author Chris Dennis
 */
public class LegacyStoreWrapper extends AbstractStore {

    private static final int SYNC_STRIPES = 64;

    @ContextChild private final Store memory;
    @ContextChild private final Store disk;
    private final RegisteredEventListeners eventListeners;
    private final CacheConfiguration config;

    private final StripedReadWriteLockSync sync = new StripedReadWriteLockSync(SYNC_STRIPES);

    /**
     * Create a correctly locked store wrapper around the supplied in-memory and on disk stores.
     *
     * @param memory in-memory store
     * @param disk on disk store
     * @param eventListeners event listener to fire on
     * @param config cache configuration
     */
    public LegacyStoreWrapper(Store memory, Store disk, RegisteredEventListeners eventListeners, CacheConfiguration config) {
        this.memory = memory;
        this.disk = disk;
        this.eventListeners = eventListeners;
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        if (disk == null) {
            return false;
        } else {
            return disk.bufferFull();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        Sync s = sync.getSyncForKey(key);
        s.lock(LockType.READ);
        try {
            if (key instanceof Serializable && (disk !=  null)) {
                return disk.containsKey(key) || memory.containsKey(key);
            } else {
                return memory.containsKey(key);
            }
        } finally {
            s.unlock(LockType.READ);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        Sync s = sync.getSyncForKey(key);
        s.lock(LockType.READ);
        try {
            return memory.containsKey(key);
        } finally {
            s.unlock(LockType.READ);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        Sync s = sync.getSyncForKey(key);
        s.lock(LockType.READ);
        try {
            if (disk != null) {
                return disk.containsKey(key);
            } else {
                return false;
            }
        } finally {
            s.unlock(LockType.READ);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        memory.dispose();
        if (disk != null) {
            disk.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {

        for (Object key : memory.getKeys()) {
            Sync s = sync.getSyncForKey(key);
            s.lock(LockType.WRITE);
            try {
                Element element = memory.getQuiet(key);
                if (element != null) {
                    if (element.isExpired(config)) {
                        Element e = remove(key);
                        if (e != null) {
                            eventListeners.notifyElementExpiry(e, false);
                        }
                    }
                }
            } finally {
                s.unlock(LockType.WRITE);
            }
        }

        //This is called regularly by the expiry thread, but call it here synchronously
        if (disk != null) {
            disk.expireElements();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        memory.flush();
        if (disk != null) {
            disk.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        Sync s = sync.getSyncForKey(key);
        s.lock(LockType.READ);
        try {
            Element e = memory.get(key);
            if (e == null && disk != null) {
                e = disk.get(key);
                if (e != null) {
                    memory.put(e);
                }
            }
            return e;
        } finally {
            s.unlock(LockType.READ);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return memory.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return memory.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return memory.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return sync;
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        if (disk == null) {
            return memory.getKeys();
        } else {
            HashSet<Object> keys = new HashSet<Object>();
            keys.addAll(memory.getKeys());
            keys.addAll(disk.getKeys());
            return new ArrayList(keys);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        if (disk == null) {
            return memory.getOffHeapSize();
        } else {
            return memory.getOffHeapSize() + disk.getOffHeapSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        if (disk == null) {
            return memory.getOffHeapSizeInBytes();
        } else {
            return memory.getOffHeapSizeInBytes() + disk.getOffHeapSizeInBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        if (disk != null) {
            return disk.getSize();
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        if (disk != null) {
            return disk.getOnDiskSizeInBytes();
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        Sync s = sync.getSyncForKey(key);
        s.lock(LockType.READ);
        try {
            Element e = memory.getQuiet(key);
            if (e == null && disk != null) {
                e = disk.getQuiet(key);
                if (e != null) {
                    memory.put(e);
                }
            }
            return e;
        } finally {
            s.unlock(LockType.READ);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The size is the number of {@link Element}s in the memory store
     * plus the number of {@link Element}s in the disk store.
     */
    public int getSize() {
        if (disk != null) {
            HashSet<Object> keys = new HashSet<Object>();
            keys.addAll(memory.getKeys());
            keys.addAll(disk.getKeys());
            return keys.size();
        } else {
            return memory.getSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return memory.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        if (element == null) {
            return false;
        }

        Sync s = sync.getSyncForKey(element.getObjectKey());
        s.lock(LockType.WRITE);
        try {
            boolean notOnDisk = !containsKeyOnDisk(element.getObjectKey());
            return memory.put(element) && notOnDisk;
        } finally {
            s.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        if (element == null) {
            return false;
        }

        Sync s = sync.getSyncForKey(element.getObjectKey());
        s.lock(LockType.WRITE);
        try {
            boolean notOnDisk = !containsKey(element.getObjectKey());
            return memory.putWithWriter(element, writerManager) && notOnDisk;
        } finally {
            s.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        Sync s = sync.getSyncForKey(key);
        s.lock(LockType.WRITE);
        try {
            Element m = memory.remove(key);
            if (disk != null && key instanceof Serializable) {
                Element d = disk.remove(key);
                if (m == null) {
                    return d;
                }
            }
            return m;
        } finally {
            s.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        memory.removeAll();
        if (disk != null) {
            disk.removeAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        Sync s = sync.getSyncForKey(key);
        s.lock(LockType.WRITE);
        try {
            Element m = memory.removeWithWriter(key, writerManager);
            if (disk != null && key instanceof Serializable) {
                Element d = disk.removeWithWriter(key, writerManager);
                if (m == null) {
                    return d;
                }
            }
            return m;
        } finally {
            s.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        memory.setInMemoryEvictionPolicy(policy);
    }

    /**
     * Returns the underlying memory store for this legacy wrapper.
     */
    public Store getMemoryStore() {
        return memory;
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        Sync lock = sync.getSyncForKey(element.getObjectKey());

        lock.lock(LockType.WRITE);
        try {
            Element e = getQuiet(element.getObjectKey());
            if (e == null) {
                put(element);
            }
            return e;
        } finally {
            lock.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        Sync lock = sync.getSyncForKey(element.getObjectKey());

        lock.lock(LockType.WRITE);
        try {
            Element current = getQuiet(element.getObjectKey());
            if (comparator.equals(element, current)) {
                return remove(current.getObjectKey());
            } else {
                return null;
            }
        } finally {
            lock.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator)
            throws NullPointerException, IllegalArgumentException {
        Sync lock = sync.getSyncForKey(old.getObjectKey());

        lock.lock(LockType.WRITE);
        try {
            Element current = getQuiet(old.getObjectKey());
            if (comparator.equals(old, current)) {
                put(element);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        Sync lock = sync.getSyncForKey(element.getObjectKey());

        lock.lock(LockType.WRITE);
        try {
            Element current = getQuiet(element.getObjectKey());
            if (current != null) {
                put(element);
            }
            return current;
        } finally {
            lock.unlock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }
}
