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

package net.sf.ehcache.constructs.blocking;


/**
 * Creates objects to populate the cache.
 * @version $Id$
 * @author Greg Luck
 */
public interface CacheEntryFactory {
    /**
     * Creates the cacheEntry for the given cache key.
     *
     * ehcache requires cache entries to be serializable.
     *
     * Note that this method must be thread safe.
     *
     * @return The entry, or null if it does not exist.
     * @throws Exception On failure creating the object.
     */
    Object createEntry(Object key) throws Exception;
}
