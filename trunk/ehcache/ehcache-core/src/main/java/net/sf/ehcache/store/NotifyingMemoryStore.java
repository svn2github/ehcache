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

package net.sf.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.pool.Pool;

/**
 * @author Alex Snaps
 */
public final class NotifyingMemoryStore extends MemoryStore {

    private final Ehcache cache;

    /**
     * Constructs things that all MemoryStores have in common.
     *
     * @param cache the cache
     * @param pool  the pool tracking the on-heap usage
     */
    private NotifyingMemoryStore(final Ehcache cache, Pool pool) {
        super(cache, pool, true);
        this.cache = cache;
    }

    /**
     * A factory method to create a MemoryStore.
     *
     * @param cache the cache
     * @param pool the pool tracking the on-heap usage
     * @return an instance of a NotifyingMemoryStore, configured with the appropriate eviction policy
     */
    public static NotifyingMemoryStore create(final Ehcache cache, Pool pool) {
        NotifyingMemoryStore store = new NotifyingMemoryStore(cache, pool);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    /**
     * {@inheritDoc}
     * and notifies listeners
     */
    @Override
    protected boolean evict(final Element element) {
        Element remove = remove(element.getObjectKey());
        if (remove != null) {
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
        }
        return remove != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void notifyDirectEviction(final Element element) {
        cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void expireElements() {
        for (Object key : keySet()) {
            final Element element = expireElement(key);
            if (element != null) {
                cache.getCacheEventNotificationService().notifyElementExpiry(element, false);
            }
        }
    }
}
