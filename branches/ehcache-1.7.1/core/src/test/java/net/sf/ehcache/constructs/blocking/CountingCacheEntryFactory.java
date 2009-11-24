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

import net.sf.ehcache.Element;

import java.util.Random;

/**
 * A cache entry factory that counts the number of entries it has created.
 * <p/>
 * This is useful for writing tests.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CountingCacheEntryFactory implements UpdatingCacheEntryFactory {

    private int count;
    private final Object value;
    private Random random;

    /**
     * Creates a new instance
     *
     * @param value the factory always creates values equal to this value
     */
    public CountingCacheEntryFactory(final Object value) {
        this.value = value;
        random = new Random();
    }

    /**
     * Fetches an entry.
     */
    public Object createEntry(final Object key) {
        count++;
        if (random.nextInt(2) == 1) {
            return value;
        } else {
            return new Element(key, value);
        }
    }

    /**
     * @return number of entries the factory has created.
     */
    public int getCount() {
        return count;
    }

    /**
     * Perform an incremental update of data within a CacheEntry.
     * Based on identification of dirty values within a CacheEntry
     * Insert Update or Delete those entries based on the existing value.
     * <p/>
     * This method does not return a modified value, because it modifies the value passed into it, relying
     * on the pass by reference feature of Java.
     * <p/>
     * Implementations of this method must be thread safe.
     *
     * @param key   the cache Key
     * @param value a value copied from the value that belonged to the Element in the cache. Value must be mutable
     * @throws Exception
     */
    public void updateEntryValue(Object key, Object value) throws Exception {
        count++;
    }

}
