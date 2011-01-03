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
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.factories.DiskPersistentStorageFactory;

/**
 * Implements a persistent-to-disk store.
 * <p>
 * All new elements are automatically scheduled for writing to disk.  In addition 
 * the store will cache Elements in memory up to the in-memory capacity.
 * 
 * @author Chris Dennis
 */
public final class DiskPersistentStore extends CompoundStore implements CacheConfigurationListener {

    private final DiskPersistentStorageFactory disk;
    
    private DiskPersistentStore(DiskPersistentStorageFactory disk, CacheConfiguration config) {
        super(disk, config.isCopyOnRead(), config.isCopyOnWrite(), config.getCopyStrategy());
        this.disk = disk;
    }
    
    /**
     * Creates a persitent-to-disk store for the given cache, using the given disk path.
     * 
     * @param cache cache that fronts this store
     * @param diskStorePath disk path to store data in
     * @return a fully initialized store
     */
    public static DiskPersistentStore create(Cache cache, String diskStorePath) {
        CacheConfiguration config = cache.getCacheConfiguration();
        DiskPersistentStorageFactory disk = new DiskPersistentStorageFactory(cache, diskStorePath);
        DiskPersistentStore store = new DiskPersistentStore(disk, config);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return disk.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return disk.isInMemory(unretrievedGet(key));
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
        return disk.isOnDisk(unretrievedGet(key));
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        disk.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        disk.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return disk.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return disk.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return disk.getInMemorySizeInBytes();
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
        return disk.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return disk.getOnDiskSizeInBytes();
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
        disk.setInMemoryEvictionPolicy(policy);
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
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        disk.setOnDiskCapacity(newCapacity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A NO-OP
     */
    public void loggingChanged(boolean oldValue, boolean newValue) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        disk.setInMemoryCapacity(newCapacity);
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
     * Return a reference to the data file backing this store.
     */
    public File getDataFile() {
        return disk.getDataFile();
    }

    /**
     * Return a reference to the index file for this store.
     */
    public File getIndexFile() {
        return disk.getIndexFile();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }
}
