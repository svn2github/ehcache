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

package net.sf.ehcache;

import net.sf.ehcache.store.Store;

/**
 * A helper class to get the internal Store from a Cache
 *
 * @author Abhishek Sanoujam
 *
 */
public class CacheStoreHelper {

    private final Cache cache;

    /**
     * Constructor accepting the cache
     *
     * @param cache
     */
    public CacheStoreHelper(final Cache cache) {
        this.cache = cache;
    }

    /**
     * Returns the internal {@link Store} of the cache
     *
     * @return the internal {@link Store} of the cache
     */
    public Store getStore() {
        return cache.getStore();
    }

}
