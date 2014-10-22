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

/**
 * Copies elements, either on read, write or both before using the underlying store to actually store things
 * When copying both ways, the store might not see the same types being stored
 * @param <T> the store type it wraps
 *
 * @author Alex Snaps
 */
public final class CopyingCacheStore<T extends Store> extends AbstractCopyingCacheStore<T> {

    /**
     * Creates a copying instance of store, that wraps the actual storage
     *
     * @param store                the real store
     * @param copyOnRead           whether to copy on reads
     * @param copyOnWrite          whether to copy on writes
     * @param copyStrategyInstance the copy strategy to use on every copy operation
     * @param loader               classloader of the containing cache
     */
    public CopyingCacheStore(T store, boolean copyOnRead, boolean copyOnWrite, ReadWriteCopyStrategy<Element> copyStrategyInstance, ClassLoader loader) {
        super(store, copyOnRead, copyOnWrite, copyStrategyInstance, loader);
    }


    /**
     * Wraps the Store instance passed in, should any copy occur
     * @param cacheStore the store
     * @param cacheConfiguration the cache config for that store
     * @return the wrapped Store if copying is required, or the Store instance passed in
     */
    public static Store wrapIfCopy(final Store cacheStore, final CacheConfiguration cacheConfiguration) {
        if (requiresCopy(cacheConfiguration)) {
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
    private static <T extends Store> CopyingCacheStore<T> wrap(final T cacheStore, final CacheConfiguration cacheConfiguration) {
        final ReadWriteCopyStrategy<Element> copyStrategyInstance = cacheConfiguration.getCopyStrategyConfiguration()
            .getCopyStrategyInstance(cacheConfiguration.getClassLoader());
        return new CopyingCacheStore<T>(cacheStore, cacheConfiguration.isCopyOnRead(), cacheConfiguration.isCopyOnWrite(), copyStrategyInstance, cacheConfiguration.getClassLoader());
    }

    /**
     * Checks whether configuration enables copying
     *
     * @param cacheConfiguration the cache config
     * @return true is copying is required, otherwise false
     */
    static boolean requiresCopy(final CacheConfiguration cacheConfiguration) {
        return cacheConfiguration.isCopyOnRead() || cacheConfiguration.isCopyOnWrite();
    }

    private static boolean isCopyOnReadAndCopyOnWrite(final CacheConfiguration cacheConfiguration) {
        return cacheConfiguration.isCopyOnRead() && cacheConfiguration.isCopyOnWrite();
    }

    /**
     * Wraps the given {@link net.sf.ehcache.store.ElementValueComparator} if the configuration requires copy on read
     *
     * @param comparator the comparator to wrap
     * @param cacheConfiguration the cache configuration
     * @return the comparator passed if no copy needed, a wrapped comparator otherwise
     */
    public static ElementValueComparator wrapIfCopy(final ElementValueComparator comparator, final CacheConfiguration cacheConfiguration) {
        if (isCopyOnReadAndCopyOnWrite(cacheConfiguration)) {
            final ReadWriteCopyStrategy<Element> copyStrategyInstance = cacheConfiguration.getCopyStrategyConfiguration()
                .getCopyStrategyInstance(cacheConfiguration.getClassLoader());
            CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(cacheConfiguration.isCopyOnRead(),
                    cacheConfiguration.isCopyOnWrite(),
                    copyStrategyInstance, cacheConfiguration.getClassLoader());
            return new CopyingElementValueComparator(comparator, copyStrategyHandler);
        }
        return comparator;
    }

    /**
     * An {@link net.sf.ehcache.store.ElementValueComparator} which handles copy on read
     */
    private static class CopyingElementValueComparator implements ElementValueComparator {

        private final ElementValueComparator delegate;
        private final CopyStrategyHandler copyStrategyHandler;

        public CopyingElementValueComparator(ElementValueComparator delegate, CopyStrategyHandler copyStrategyHandler) {
            this.delegate = delegate;
            this.copyStrategyHandler = copyStrategyHandler;
        }

        @Override
        public boolean equals(Element e1, Element e2) {
            return delegate.equals(copyStrategyHandler.copyElementForReadIfNeeded(e1), copyStrategyHandler.copyElementForReadIfNeeded(e2));
        }
    }
}
