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

/**
 * A management bean for a cache
 *
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public interface CacheMBean {


     /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    void removeAll() throws IllegalStateException, CacheException;


    /**
     * Flushes all cache items from memory to the disk store, and from the DiskStore to disk.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    void flush() throws IllegalStateException, CacheException;


    /**
     * Gets the status attribute of the Cache.
     *
     * @return The status value from the Status enum class
     */
    String getStatus();


    /**
     * Gets the cache name.
     */
    String getName();


    /**
     *
     * Gets the JMX read-only CacheConfiguration
     */
    CacheConfiguration getCacheConfiguration();


    /**
     *
     * Gets the JMX cache statistics
     */
    public CacheStatistics getStatistics();



}
