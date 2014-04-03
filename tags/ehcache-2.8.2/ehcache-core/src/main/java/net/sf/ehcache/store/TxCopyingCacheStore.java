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

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.transaction.AbstractTransactionStore;
import net.sf.ehcache.transaction.SoftLockID;

/**
 * Copies elements, either on read, write or both before using the underlying store to actually store things.
 * When copying both ways, the store might not see the same types being stored.
 * Also exposes a method to get the old value from an entry currently mutated in a transaction.
 *
 * @param <T> the store type it wraps
 *
 * @author ljacomet
 */
public final class TxCopyingCacheStore<T extends Store> extends AbstractCopyingCacheStore<T> {

    /**
     * Creates a copying instance of store, that wraps the actual storage
     *
     * @param store                the real store
     * @param copyOnRead           whether to copy on reads
     * @param copyOnWrite          whether to copy on writes
     * @param copyStrategyInstance the copy strategy to use on every copy operation
     */
    public TxCopyingCacheStore(T store, boolean copyOnRead, boolean copyOnWrite, ReadWriteCopyStrategy<Element> copyStrategyInstance) {
        super(store, copyOnRead, copyOnWrite, copyStrategyInstance);
        if (!(store instanceof AbstractTransactionStore)) {
            throw new IllegalArgumentException("TxCopyingCacheStore can only wrap a transactional store");
        }
    }

    /**
     * Gets an element from the store, choosing the old value in case the element is currently mutated inside a transaction.
     *
     * @param key the key to look for
     * @return the matching element, null if not found
     */
    public Element getOldElement(Object key) {
        return getCopyStrategyHandler().copyElementForReadIfNeeded(((AbstractTransactionStore)getUnderlyingStore()).getOldElement(key));
    }


    /**
     * Wraps the Store instance passed in, should any copy occur
     * @param cacheStore the store
     * @param cacheConfiguration the cache config for that store
     * @return the wrapped Store if copying is required, or the Store instance passed in
     */
    public static Store wrapTxStore(final AbstractTransactionStore cacheStore, final CacheConfiguration cacheConfiguration) {
        if (CopyingCacheStore.requiresCopy(cacheConfiguration)) {
            return wrap(cacheStore, cacheConfiguration);
        }
        return cacheStore;
    }

    /**
     * Wraps (always) with the proper configured CopyingCacheStore
     * @param cacheStore the store to wrap
     * @param cacheConfiguration the cache config backed by this store
     * @param <T> the Store type
     * @return the wrapped store
     */
    private static <T extends Store> TxCopyingCacheStore<T> wrap(final T cacheStore, final CacheConfiguration cacheConfiguration) {
        final ReadWriteCopyStrategy<Element> copyStrategyInstance = cacheConfiguration.getCopyStrategyConfiguration()
            .getCopyStrategyInstance();
        return new TxCopyingCacheStore<T>(cacheStore, cacheConfiguration.isCopyOnRead(), cacheConfiguration.isCopyOnWrite(), copyStrategyInstance);
    }

    /**
     * Wraps the given {@link net.sf.ehcache.store.ElementValueComparator} if the configuration requires copy on read
     *
     * @param comparator the comparator to wrap
     * @param cacheConfiguration the cache configuration
     * @return the comparator passed if no copy needed, a wrapped comparator otherwise
     */
    public static ElementValueComparator wrap(final ElementValueComparator comparator, final CacheConfiguration cacheConfiguration) {
        final ReadWriteCopyStrategy<Element> copyStrategyInstance = cacheConfiguration.getCopyStrategyConfiguration()
                .getCopyStrategyInstance();
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(cacheConfiguration.isCopyOnRead(),
                cacheConfiguration.isCopyOnWrite(),
                copyStrategyInstance);
        return new TxCopyingElementValueComparator(comparator, copyStrategyHandler);
    }

    /**
     * An {@link net.sf.ehcache.store.ElementValueComparator} which handles copy on read and {@link SoftLockID}
     */
    private static class TxCopyingElementValueComparator implements ElementValueComparator {

        private final ElementValueComparator delegate;
        private final CopyStrategyHandler copyStrategyHandler;

        public TxCopyingElementValueComparator(ElementValueComparator delegate, CopyStrategyHandler copyStrategyHandler) {
            this.delegate = delegate;
            this.copyStrategyHandler = copyStrategyHandler;
        }

        @Override
        public boolean equals(Element e1, Element e2) {
            if (!(e1.getObjectValue() instanceof SoftLockID)) {
                e1 = copyStrategyHandler.copyElementForReadIfNeeded(e1);
            }
            if (!(e2.getObjectValue() instanceof SoftLockID)) {
                e2 = copyStrategyHandler.copyElementForReadIfNeeded(e2);
            }
            return delegate.equals(e1, e2);
        }
    }
}
