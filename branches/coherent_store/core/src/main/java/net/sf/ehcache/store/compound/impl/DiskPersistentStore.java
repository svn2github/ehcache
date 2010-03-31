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

import java.io.File;
import java.io.IOException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.factories.DiskPersistentStorageFactory;

public final class DiskPersistentStore extends CompoundStore implements CacheConfigurationListener {

    private final DiskPersistentStorageFactory disk;
    
    private DiskPersistentStore(DiskPersistentStorageFactory disk) {
        super(disk);
        this.disk = disk;
    }
    
    public static DiskPersistentStore create(Cache cache, String diskStorePath) {
        CacheConfiguration config = cache.getCacheConfiguration();
        DiskPersistentStorageFactory disk = new DiskPersistentStorageFactory(cache, diskStorePath);
        DiskPersistentStore store = new DiskPersistentStore(disk);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    public boolean bufferFull() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean containsKeyInMemory(Object key) {
        return disk.isInMemory(unretrievedGet(key));
    }

    public boolean containsKeyOnDisk(Object key) {
        return disk.isOnDisk(unretrievedGet(key));
    }

    public void expireElements() {
        // TODO Auto-generated method stub

    }

    public void flush() throws IOException {
        // TODO Auto-generated method stub

    }

    public Policy getInMemoryEvictionPolicy() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getInMemorySize() {
        return disk.getInMemorySize();
    }

    public long getInMemorySizeInBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getOnDiskSize() {
        return disk.getOnDiskSize();
    }

    public long getOnDiskSizeInBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getTerracottaClusteredSize() {
        return 0;
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        // TODO Auto-generated method stub
    }

    public void deregistered(CacheConfiguration config) {
        //no-op
    }

    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        // TODO Auto-generated method stub
        
    }

    public void loggingEnabledChanged(boolean oldValue, boolean newValue) {
        //no-op
    }

    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        // TODO Auto-generated method stub
        
    }

    public void registered(CacheConfiguration config) {
        //no-op
    }

    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
        //no-op
    }

    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        //no-op
    }

    public File getDataFile() {
        return disk.getDataFile();
    }
}
