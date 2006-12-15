/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.management;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;


/**
 * A JMX MBean implementation and decorator to net.sf.ehcache.CacheConfiguration
 * @author Greg Luck
 * @version $Id$
 */
public class CacheConfiguration implements CacheConfigurationMBean {

    private net.sf.ehcache.config.CacheConfiguration cacheConfiguration;

    /**
     * Constructs using a backing CacheConfiguration
     * @param cacheConfiguration
     */
    public CacheConfiguration(net.sf.ehcache.config.CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    /**
     * Accessor
     */
    public String getName() {
        return cacheConfiguration.getName();
    }

    /**
     * Accessor
     */
    public int getMaxElementsInMemory() {
        return cacheConfiguration.getMaxElementsInMemory();
    }

    /**
     * Accessor
     */
    public int getMaxElementsOnDisk() {
        return cacheConfiguration.getMaxElementsOnDisk();
    }

    /**
     * Accessor
     */
    public MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
        return cacheConfiguration.getMemoryStoreEvictionPolicy();
    }

    /**
     * Accessor
     */
    public boolean isEternal() {
        return cacheConfiguration.isEternal();
    }

    /**
     * Accessor
     */
    public long getTimeToIdleSeconds() {
        return cacheConfiguration.getTimeToIdleSeconds();
    }

    /**
     * Accessor
     */
    public long getTimeToLiveSeconds() {
        return cacheConfiguration.getTimeToLiveSeconds();
    }

    /**
     * Accessor
     */
    public boolean isOverflowToDisk() {
        return cacheConfiguration.isOverflowToDisk();
    }

    /**
     * Accessor
     */
    public boolean isDiskPersistent() {
        return cacheConfiguration.isDiskPersistent();
    }

    /**
     * Accessor
     */
    public long getDiskExpiryThreadIntervalSeconds() {
        return cacheConfiguration.getDiskExpiryThreadIntervalSeconds();
    }
}
