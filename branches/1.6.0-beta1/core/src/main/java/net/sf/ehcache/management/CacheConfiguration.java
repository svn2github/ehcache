/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import net.sf.ehcache.CacheException;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import java.io.Serializable;


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
     * @return a String representation of the policy
     */
    public String getMemoryStoreEvictionPolicy() {
        return cacheConfiguration.getMemoryStoreEvictionPolicy().toString();
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
    public int getDiskSpoolBufferSizeMB() {
        return cacheConfiguration.getDiskSpoolBufferSizeMB();
    }

    /**
     * Accessor
     */
    public long getDiskExpiryThreadIntervalSeconds() {
        return cacheConfiguration.getDiskExpiryThreadIntervalSeconds();
    }


    /**
     * @return the object name for this MBean
     */
    ObjectName getObjectName() {
        return objectName;
    }
}
