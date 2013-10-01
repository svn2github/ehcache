/**
 *  Copyright Terracotta, Inc.
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
 * A cache entry factory that blocks until signaled.
 * <p/>
 * This is useful for writing tests.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class BlockingCacheEntryFactory implements CacheEntryFactory {

    private final Object value;
    private int count;

    /**
     * Constructs a new object
     *
     * @param value the factory always creates values equal to this value
     */
    public BlockingCacheEntryFactory(final Object value) {
        this.value = value;
    }


    /**
     * @return number of entries the factory has created.
     */
    public int getCount() {
        return count;
    }


    /**
     * Signals the factory.
     */
    public synchronized void signal(final int count) {
        this.count += count;
        notify();
    }

    /**
     * Fetches an entry.
     */
    public synchronized Object createEntry(final Object key) throws Exception {
        // Wait until signalled
        while (count == 0) {
            wait();
        }
        count--;
        return value;
    }
}
