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
package net.sf.ehcache.writer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

/**
 * A {@code CacheWriterManager} coordinates how element are written to a back-end store.
 * <p/>
 * The {@code CacheWriterManager} will in its turn call the {@code CacheWriter} that belongs to the relevant cache to perform
 * the actual write logic as it's implemented by the user.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public interface CacheWriterManager {
    /**
     * Initialize the cache writer manager.
     * <p>
     * This method is called when the cache writer manager is registered to a cache.
     *
     * @param cache the cache with which the writer manager
     * @throws CacheException when an exception occurs during the initialisation of the cache
     */
    void init(Cache cache) throws CacheException;

    /**
     * Schedule a put operation for this element in the CacheWriterManager, which will call the CacheWriter when appropriate.
     *
     * @param element the element that should be used for the operation
     * @throws CacheException when an exception occurs during the writing of the element
     */
    void put(Element element) throws CacheException;

    /**
     * Schedule a remove operation for this key in the CacheWriterManager, which will call the CacheWriter when appropriate.
     *
     * @param entry the entry that should be used for the operation
     * @throws CacheException when an exception occurs during the removal of the element
     */
    void remove(CacheEntry entry) throws CacheException;

    /**
     * Cleans up the resources of the cache writer manager.
     * <p>
     * This method is called when the manager is unregistered from a cache.
     */
    void dispose();
}