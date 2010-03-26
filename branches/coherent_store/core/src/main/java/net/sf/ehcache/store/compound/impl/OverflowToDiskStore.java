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

package net.sf.ehcache.store.compound.impl;

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

    public static OverflowToDiskStore create(Cache cache, String diskStorePath) {
        CacheConfiguration config = cache.getCacheConfiguration();
        DiskOverflowStorageFactory disk = new DiskOverflowStorageFactory(cache, diskStorePath);
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(disk, config.getMaxElementsInMemory(), determineEvictionPolicy(config));            
        OverflowToDiskStore store = new OverflowToDiskStore(memory, disk, config);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    /**
     * Chooses the Policy from the cache configuration
     */
    protected static final Policy determineEvictionPolicy(CacheConfiguration config) {
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
    
    public boolean bufferFull() {
        return diskFactory.bufferFull();
    }
    
    public boolean containsKeyInMemory(Object key) {
        return memoryFactory.created(unretrievedGet(key));
    }

    public boolean containsKeyOnDisk(Object key) {
        return diskFactory.created(unretrievedGet(key));
    }

    public int getInMemorySize() {
        return memoryFactory.getSize();
    }

    public long getInMemorySizeInBytes() {
        return memoryFactory.getSizeInBytes();
    }

    public int getOnDiskSize() {
        return diskFactory.getSize();
    }

    public long getOnDiskSizeInBytes() {
        return diskFactory.getSizeInBytes();
    }

    public Policy getInMemoryEvictionPolicy() {
        return memoryFactory.getEvictionPolicy();
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        memoryFactory.setEvictionPolicy(policy);
    }

    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        diskFactory.setCapacity(newCapacity);
    }

    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        memoryFactory.setCapacity(newCapacity);
    }

    public void loggingEnabledChanged(boolean oldValue, boolean newValue) {
        //no-op
    }

    public void registered(CacheConfiguration config) {
        //no-op
    }

    public void deregistered(CacheConfiguration config) {
        //no-op
    }

    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
        //no-op
    }

    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        //no-op
    }

    public void expireElements() {
        throw new UnsupportedOperationException();
    }

    public void flush() throws IOException {
        if (config.isClearOnFlush()) {
            for (Object key : getKeyArray()) {
                if (containsKeyInMemory(key)) {
                    remove(key);
                }
            }
        }
    }

    public int getTerracottaClusteredSize() {
        return 0;
    }
}
