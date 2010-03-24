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

package net.sf.ehcache.management;

import java.io.Serializable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import net.sf.ehcache.CacheException;


/**
 * A JMX MBean implementation and decorator to net.sf.ehcache.CacheConfiguration
 *
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public class CacheConfiguration implements CacheConfigurationMBean, Serializable {

    private static final long serialVersionUID = -8944774509593267228L;

    private transient net.sf.ehcache.config.CacheConfiguration cacheConfiguration;

    private ObjectName objectName;

    /**
     * Constructs using a backing CacheConfiguration
     *
     * @param cache
     */
    public CacheConfiguration(net.sf.ehcache.Ehcache cache) {
        cacheConfiguration = cache.getCacheConfiguration();
        objectName = createObjectName(cache.getCacheManager().toString(), cache.getName());
    }

    /**
     * Creates an object name using the scheme "net.sf.ehcache:type=CacheConfiguration,CacheManager=<cacheManagerName>,name=<cacheName>"
     */
    static ObjectName createObjectName(String cacheManagerName, String cacheName) {
        ObjectName objectName;
        try {
            objectName = new ObjectName("net.sf.ehcache:type=CacheConfiguration,CacheManager="
                    + cacheManagerName + ",name=" + cacheName);
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
        return objectName;
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
    public boolean isLoggingEnabled() {
        return cacheConfiguration.isLoggingEnabled();
    }

    /**
     * setLoggingEnabled
     * 
     * @param enable
     */
    public void setLoggingEnabled(boolean enable) {
        cacheConfiguration.setLoggingEnabled(enable);
    }
    /**
     * Accessor
     */
    public int getMaxElementsInMemory() {
        return cacheConfiguration.getMaxElementsInMemory();
    }

    /**
     * setMaxElementsInMemory
     *  
     * @param maxElements
     */
    public void setMaxElementsInMemory(int maxElements) {
       cacheConfiguration.setMaxElementsInMemory(maxElements);
    }
    
    /**
     * Accessor
     */
    public int getMaxElementsOnDisk() {
       return cacheConfiguration.getMaxElementsOnDisk();
    }

    /**
     * setMaxElementsOnDisk
     *  
     * @param maxElements
     */
    public void setMaxElementsOnDisk(int maxElements) {
       cacheConfiguration.setMaxElementsInMemory(maxElements);
    }
    
    /**
     * Accessor
     * @return a String representation of the policy
     */
    public String getMemoryStoreEvictionPolicy() {
        return cacheConfiguration.getMemoryStoreEvictionPolicy().toString();
    }

    /**
     * setMemoryStoreEvictionPolicy
     *  
     * @param memoryStoreEvictionPolicy
     */
    public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        cacheConfiguration.setMemoryStoreEvictionPolicy(memoryStoreEvictionPolicy);
    }
    
    /**
     * Accessor
     */
    public boolean isEternal() {
        return cacheConfiguration.isEternal();
    }

    /**
     * setEternal
     * 
     * @param eternal
     */
    public void setEternal(boolean eternal) {
        cacheConfiguration.setEternal(eternal);
    }
    
    /**
     * Accessor
     */
    public long getTimeToIdleSeconds() {
        return cacheConfiguration.getTimeToIdleSeconds();
    }

    /**
     * setTimeToIdleSeconds
     *  
     * @param tti
     */
    public void setTimeToIdleSeconds(long tti) {
       cacheConfiguration.setTimeToIdleSeconds(tti);
    }
    
    /**
     * Accessor
     */
    public long getTimeToLiveSeconds() {
        return cacheConfiguration.getTimeToLiveSeconds();
    }

    /**
     * setTimeToLiveSeconds
     *  
     * @param ttl
     */
    public void setTimeToLiveSeconds(long ttl) {
        cacheConfiguration.setTimeToLiveSeconds(ttl);
    }
    
    /**
     * Accessor
     */
    public boolean isOverflowToDisk() {
        return cacheConfiguration.isOverflowToDisk();
    }

    /**
     * setOverflowToDisk
     * 
     * @param overflowToDisk
     */
    public void setOverflowToDisk(boolean overflowToDisk) {
        cacheConfiguration.setOverflowToDisk(overflowToDisk);
    }
    
    /**
     * Accessor
     */
    public boolean isDiskPersistent() {
        return cacheConfiguration.isDiskPersistent();
    }

    /**
     * setDiskPersistent
     * 
     * @param diskPersistent
     */
    public void setDiskPersistent(boolean diskPersistent) {
        cacheConfiguration.setDiskPersistent(diskPersistent);
    }
    
    /**
     * Accessor
     */
    public int getDiskSpoolBufferSizeMB() {
        return cacheConfiguration.getDiskSpoolBufferSizeMB();
    }

    /**
     * setDiskSpoolBufferSizeMB
     * 
     * @param diskSpoolBufferSizeMB
     */
    public void setDiskSpoolBufferSizeMB(int diskSpoolBufferSizeMB) {
        cacheConfiguration.setDiskSpoolBufferSizeMB(diskSpoolBufferSizeMB);
    }
    
    /**
     * Accessor
     */
    public long getDiskExpiryThreadIntervalSeconds() {
        return cacheConfiguration.getDiskExpiryThreadIntervalSeconds();
    }

    /**
     * setDiskExpiryThreadIntervalSeconds
     * 
     * @param diskExpiryThreadIntervalSeconds
     */
    public final void setDiskExpiryThreadIntervalSeconds(long diskExpiryThreadIntervalSeconds) {
        cacheConfiguration.setDiskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
    }
        
    /**
     * Accessor
     */
    public boolean isTerracottaClustered() {
        return cacheConfiguration.isTerracottaClustered();
    }


    /**
     * @return the object name for this MBean
     */
    ObjectName getObjectName() {
        return objectName;
    }
}
