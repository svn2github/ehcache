/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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
import net.sf.ehcache.Ehcache;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

/**
 * A JMX MBean implementation for Cache
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public class Cache implements CacheMBean {

    /**
     * An Ehcache backing instance
     */
    private Ehcache cache;
    private ObjectName objectName;


    /**
     * A constructor for JCache.
     *
     * JCache is an adaptor for an Ehcache, and therefore requires an Ehcace in its constructor.
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Ehcache} object
     * into {@link net.sf.ehcache.CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param cache An ehcache
     * @throws net.sf.ehcache.CacheException
     */
    public Cache(Ehcache cache) throws CacheException {
        this.cache = cache;
        objectName = createObjectName(cache.getCacheManager().toString(), cache.getName());
    }

    /**
     * Creates an object name using the scheme "net.sf.ehcache:type=Cache,CacheManager=<cacheManagerName>,name=<cacheName>"
     */
    static ObjectName createObjectName(String cacheManagerName, String cacheName) {
        ObjectName objectName;
        try {
            objectName = new ObjectName("net.sf.ehcache:type=Cache,CacheManager="
                    + cacheManagerName + ",name=" + cacheName);
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
        return objectName;
    }


    /**
     * Removes all cached items.
    *
    * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
    */
    public void removeAll() throws IllegalStateException, CacheException {
        cache.removeAll();
    }

    /**
     * Flushes all cache items from memory to the disk store, and from the DiskStore to disk.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void flush() throws IllegalStateException, CacheException {
        cache.flush();
    }

    /**
     * Gets the status attribute of the Cache.
     *
     * @return The status value as a String from the Status enum class
     */
    public String getStatus() {
        return cache.getStatus().toString();
    }

    /**
     * Gets the cache name.
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * Gets the JMX read-only CacheConfiguration
     */
    public CacheConfiguration getCacheConfiguration() {
        return new CacheConfiguration(cache);
    }

    /**
     * Gets the JMX cache statistics
     */
    public CacheStatistics getStatistics() {
        return new CacheStatistics(cache);
    }


    /**
     * @return the object name for this MBean
     */
    ObjectName getObjectName() {
        return objectName;
    }
}
