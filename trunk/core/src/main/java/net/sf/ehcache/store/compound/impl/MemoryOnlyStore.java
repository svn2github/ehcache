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

/**
 * Implements a memory only store.
 * 
 * @author Chris Dennis
 */
public final class MemoryOnlyStore extends CompoundStore implements CacheConfigurationListener {

    private final CapacityLimitedInMemoryFactory memoryFactory;
    
    private final CacheConfiguration config;
    
    private MemoryOnlyStore(CapacityLimitedInMemoryFactory memory, CacheConfiguration config) {
        super(memory, config.isCopyOnRead(), config.isCopyOnWrite(), config.getCopyStrategy());
        this.memoryFactory = memory;
        this.config = config;
    }
    
    /**
     * Constructs an in-memory store for the given cache, using the given disk path.
     * 
     * @param cache cache that fronts this store
     * @param diskStorePath disk path to store data in
     * @return a fully initialized store
     */
    public static MemoryOnlyStore create(Cache cache, String diskStorePath) {
        CacheConfiguration config = cache.getCacheConfiguration();
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(null, config.getMaxElementsInMemory(),
                determineEvictionPolicy(config), cache.getCacheEventNotificationService());
        MemoryOnlyStore store = new MemoryOnlyStore(memory, config);
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
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return containsKey(key);
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
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        memoryFactory.expireElements();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This store is not persistent, so this simply clears the in-memory store if 
     * clear-on-flush is set for this cache.
     */
    public void flush() throws IOException {
        if (config.isClearOnFlush()) {
            removeAll();
        }
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
    public int getInMemorySize() {
        return getSize();
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
    public int getOffHeapSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return 0;
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
    public void setInMemoryEvictionPolicy(Policy policy) {
        memoryFactory.setEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void deregistered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void loggingChanged(boolean oldValue, boolean newValue) {
        // no-op
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
    public void registered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        // no-op
    }

}
