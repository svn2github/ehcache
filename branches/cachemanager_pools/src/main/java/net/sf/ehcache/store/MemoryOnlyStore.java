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

package net.sf.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

/**
 * A memory-only store with support for all caching features.
 *
 * @author Ludovic Orban
 */
public final class MemoryOnlyStore extends FrontEndCacheTier<NullStore, MemoryStore> {

    private final boolean copyOnRead;
    private final boolean copyOnWrite;
    private final ReadWriteCopyStrategy<Element> copyStrategy;

    private MemoryOnlyStore(CacheConfiguration cacheConfiguration, NullStore cache, MemoryStore authority) {
        super(cache, authority);

        this.copyOnRead = cacheConfiguration.isCopyOnRead();
        this.copyOnWrite = cacheConfiguration.isCopyOnWrite();
        this.copyStrategy = cacheConfiguration.getCopyStrategy();
    }

    /**
     * Create an instance of MemoryStore
     * @param cache the cache
     * @param onHeapPool the on heap pool
     * @return an instance of MemoryStore
     */
    public static Store create(Ehcache cache, Pool onHeapPool) {
        final NullStore nullStore = NullStore.create();
        final MemoryStore memoryStore = MemoryStore.create(cache, onHeapPool);
        return new MemoryOnlyStore(cache.getCacheConfiguration(), nullStore, memoryStore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isAuthorityHandlingPinnedElements() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Element copyElementForReadIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForRead(element);
        } else if (copyOnRead) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Element copyElementForWriteIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element);
        } else if (copyOnWrite) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCacheFull() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readLock(Object key) {
        authority.readLock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readUnlock(Object key) {
        authority.readUnlock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLock(Object key) {
        authority.writeLock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUnlock(Object key) {
        authority.writeUnlock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readLock() {
        authority.readLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readUnlock() {
        authority.readUnlock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLock() {
        authority.writeLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUnlock() {
        authority.writeUnlock();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        //TODO this might be wrong...
        return authority.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return cache.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        cache.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return authority.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return authority.getMBean();
    }
}
