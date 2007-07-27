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

package net.sf.ehcache.constructs.blocking;

/**
 * A <code>CacheEntryFactory</code> with one additional method, <code>updateEntryValue((Serializable key, Serializable value)</code>
 * which allows the cache entry to updated rather than replaced. This has the following
 * potential benefits:
 * <ul>
 * <li>Where only part of the value needs to be updated, it is quicker
 * <li>Memory use can be smoothed, which is useful for particularly large objects which are being
 * refreshed contrinuously
 * </ul>
 * 
 * @author Greg Luck
 * @version $Id$
 */
public interface UpdatingCacheEntryFactory extends CacheEntryFactory {
    /**
     * Perform an incremental update of data within a CacheEntry.
     * Based on identification of dirty values within a CacheEntry
     * Insert Update or Delete those entries based on the existing value.
     * <p/>
     * This method does not return a modified value, because it modifies the value passed into it, relying
     * on the pass by reference feature of Java.
     *
     * Implementations of this method must be thread safe.
     *
     * @param key the cache Key
     * @param value a value copied from the value that belonged to the Element in the cache. Value must be mutable
     * @throws Exception
     */
    void updateEntryValue(Object key, Object value) throws Exception;

}

