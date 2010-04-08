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

package net.sf.ehcache.store.compound.impl;

import java.io.File;
import java.io.IOException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.store.FifoPolicy;
import net.sf.ehcache.store.LfuPolicy;
import net.sf.ehcache.store.LruPolicy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.factories.CapacityLimitedInMemoryFactory;
import net.sf.ehcache.store.compound.factories.DiskOverflowStorageFactory;

/**
 * Implements an overflow-to-disk store.
 * <p>
 * When this store's in-memory space becomes full, it pushes Elements off to disk.
 * 
 * @author Chris Dennis
 */
public final class OverflowToDiskStore extends CompoundStore implements CacheConfigurationListener {

    private final CapacityLimitedInMemoryFactory memoryFactory;
    private final DiskOverflowStorageFactory diskFactory;
    private final CacheConfiguration config;
    
    private OverflowToDiskStore(CapacityLimitedInMemoryFactory memory, DiskOverflowStorageFactory disk, CacheConfiguration config) {
        super(memory);
        this.memoryFactory = memory;
        this.diskFactory = disk;
        this.config = config;
    }

    /**
     * Constructs an overflow-to-disk store for the given cache, using the given disk path.
     * 
     * @param cache cache that fronts this store
     * @param diskStorePath disk path to store data in
     * @return a fully initialized store
     */
    public static OverflowToDiskStore create(Cache cache, String diskStorePath) {
        CacheConfiguration config = cache.getCacheConfiguration();
        DiskOverflowStorageFactory disk = new DiskOverflowStorageFactory(cache, diskStorePath);
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(disk, config.getMaxElementsInMemory(), 
                determineEvictionPolicy(config), cache.getCacheEventNotificationService());            
        OverflowToDiskStore store = new OverflowToDiskStore(memory, disk, config);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    /**
     * Chooses the Policy from the cache configuration
     */
    private static final Policy determineEvictionPolicy(CacheConfiguration config) {
        MemoryStoreEvictionPolicy policySelection = config.getMemoryStoreEvictionPolicy();

        if (policySelection.equals(MemoryStoreEvictionPolicy.LRU)) {
            return new LruPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.FIFO)) {
            return new FifoPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.LFU)) {
            return new LfuPolicy();
        }

        throw new IllegalArgumentException(policySelection + " isn't a valid eviction policy");
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return diskFactory.bufferFull();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return memoryFactory.created(unretrievedGet(key));
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return diskFactory.created(unretrievedGet(key));
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return memoryFactory.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return memoryFactory.getSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return diskFactory.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return diskFactory.getSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return memoryFactory.getEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        memoryFactory.setEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        diskFactory.setCapacity(newCapacity);
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        memoryFactory.setCapacity(newCapacity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void loggingEnabledChanged(boolean oldValue, boolean newValue) {
        //no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void registered(CacheConfiguration config) {
        //no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void deregistered(CacheConfiguration config) {
        //no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
        //no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        memoryFactory.expireElements();
        diskFactory.expireElements();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This store is not persistent, so this simply clears the in-memory store if 
     * clear-on-flush is set for this cache.
     */
    public void flush() throws IOException {
        if (config.isClearOnFlush()) {
            for (Object key : getKeyArray()) {
                if (containsKeyInMemory(key)) {
                    remove(key);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        return 0;
    }

    /**
     * Return a reference to the file backing this store.
     */
    public File getDataFile() {
        return diskFactory.getDataFile();
    }
}
