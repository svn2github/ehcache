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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.StripedReadWriteLock;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * TODO this class requires some love... Don't think the CachingTier is always properly maintained should authority.*() throws
 *
 * @author Alex Snaps
 */
public class CacheStore implements Store {

    private static final int DEFAULT_LOCK_STRIPE_COUNT = 128;

    private final CachingTier<Object, Element> cachingTier;
    private final AuthoritativeTier authoritativeTier;

    @Deprecated
    private final StripedReadWriteLock masterLocks;

    @Deprecated
    private final CacheConfiguration cacheConfiguration;
    private volatile Status status;

    /**
     * Constructor :P
     *
     * @param cache     the cache fronting the authority
     * @param authority the authority fronted by the cache
     */
    public CacheStore(final CachingTier<Object, Element> cache, final AuthoritativeTier authority) {
        this(cache, authority, null);
    }

    /**
     * Constructor :P
     *
     * @param cache              the cache fronting the authority
     * @param authority          the authority fronted by the cache
     * @param cacheConfiguration OMFG! NOOOOOooooo.....
     */
    @Deprecated
    public CacheStore(final CachingTier<Object, Element> cache, final AuthoritativeTier authority, CacheConfiguration cacheConfiguration) {
        if (cache == null || authority == null) {
            throw new NullPointerException();
        }
        this.cachingTier = cache;
        this.cacheConfiguration = cacheConfiguration;
        this.cachingTier.addListener(new CachingTier.Listener<Object, Element>() {
            @Override
            public void evicted(final Object key, final Element value) {
                authority.flush(value);
            }
        });
        this.authoritativeTier = authority;
        if (authority instanceof StripedReadWriteLockProvider) {
            masterLocks = ((StripedReadWriteLockProvider)authority).createStripedReadWriteLock();
        } else {
            masterLocks = new StripedReadWriteLockSync(DEFAULT_LOCK_STRIPE_COUNT);
        }
        this.status = Status.STATUS_ALIVE;
    }

    @Override
    public void unpinAll() {
        authoritativeTier.unpinAll();
    }

    @Override
    public boolean isPinned(final Object key) {
        return authoritativeTier.isPinned(key);
    }

    @Override
    public void setPinned(final Object key, final boolean pinned) {
        authoritativeTier.setPinned(key, pinned);
    }

    @Override
    public void addStoreListener(final StoreListener listener) {
        authoritativeTier.addStoreListener(listener);
    }

    @Override
    public void removeStoreListener(final StoreListener listener) {
        authoritativeTier.removeStoreListener(listener);
    }


    /**
     * This method currently populates the CachingTier too, but would be racy for any CachingTier evicting Faults
     * (i.e. mapping being installed). We _know_ we don't do that...
     * We'll see later whether we delete the tests that assumes we populate the cache on put (or adapt them)
     * Or we try to actually make this impl. deal with it (could be we can assume stuff about cachingTier, or another set
     * of methods might enable this usecase)
     * <p/>
     * Current race is as following:
     * <p/>
     * Auth        Cache
     * t1(K) ->    +A     WS     -
     * t2(K) ->    +B           +B
     * t3(*) ->    +++           - (evicts K)
     * t1(K) ->     B     RS    +A
     * <p/>
     * Basically, if a newer value gets evicted from the cache, while we're scheduled out for a longer period,
     * we might install an old value in the cache, making it out of sync with the underlying authoritativeTier.
     * <p/>
     * This would be the way I'd rather have this (i.e. not populate the CachingTier on put:
     * try {
     * return authoritativeTier.put(element);
     * } finally {
     * cachingTier.remove(element.getObjectKey());
     * }
     */
    @Override
    public boolean put(final Element element) throws CacheException {
        final boolean[] hack = new boolean[1];
        cachingTier.remove(element.getObjectKey());
        if (cachingTier.get(element.getObjectKey(), new Callable<Element>() {
            @Override
            public Element call() throws Exception {
                hack[0] = authoritativeTier.putFaulted(element);
                return element;
            }
        }, false) != element) {
            cachingTier.remove(element.getObjectKey());
            return authoritativeTier.put(element);
        }
        return hack[0];
    }

    // TODO this is probably not optimum
    @Override
    public void putAll(final Collection<Element> elements) throws CacheException {
        for (Element element : elements) {
            put(element);
        }
    }

    @Override
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        try {
            return authoritativeTier.putWithWriter(element, writerManager);
        } finally {
            cachingTier.remove(element.getObjectKey());
        }
    }

    @Override
    public Element get(final Object key) {
        if (key == null) {
            return null;
        }
        return cachingTier.get(key, new Callable<Element>() {
            @Override
            public Element call() throws Exception {
                return authoritativeTier.fault(key, true);
            }
        }, true);
    }

    @Override
    public Element getQuiet(final Object key) {
        if (key == null) {
            return null;
        }
        return cachingTier.get(key, new Callable<Element>() {
            @Override
            public Element call() throws Exception {
                return authoritativeTier.fault(key, false);
            }
        }, false);
    }

    @Override
    public List getKeys() {
        return authoritativeTier.getKeys();
    }

    @Override
    public Element remove(final Object key) {
        if (key == null) {
            return null;
        }
        try {
            return authoritativeTier.remove(key);
        } finally {
            cachingTier.remove(key);
        }
    }

    // TODO this is probably not optimum
    @Override
    public void removeAll(final Collection<?> keys) {
        for (Object key : keys) {
            remove(key);
        }
    }

    @Override
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        try {
            return authoritativeTier.removeWithWriter(key, writerManager);
        } finally {
            cachingTier.remove(key);
        }
    }

    @Override
    public void removeAll() throws CacheException {
        try {
            authoritativeTier.removeAll();
        } finally {
            cachingTier.clear();
        }
    }

    @Override
    public Element putIfAbsent(final Element element) throws NullPointerException {
        final Element previous = authoritativeTier.putIfAbsent(element);
        if (previous == null) {
            // && cache.putIfAbsent(element.getObjectKey(), element) != null) { todo this, putIfAbsent would need to fault as well!
            cachingTier.remove(element.getObjectKey());
        }
        return previous;
    }

    @Override
    public Element removeElement(final Element element, final ElementValueComparator comparator) throws NullPointerException {
        final Element removedElement = authoritativeTier.removeElement(element, comparator);
        cachingTier.remove(removedElement.getObjectKey());
        return removedElement;
    }

    @Override
    public boolean replace(final Element old, final Element element, final ElementValueComparator comparator)
        throws NullPointerException, IllegalArgumentException {

        // validate that old.getObjectKey() && element.getObjectKey() match here, rather than only the authority do it ?...
        final boolean replaced = authoritativeTier.replace(old, element, comparator);
        if (replaced) {
            // && !cache.replace(element.getObjectKey(), old, element)) { todo this, replace would need to fault as well!
            cachingTier.remove(element.getObjectKey());
        }
        return replaced;
    }

    @Override
    public Element replace(final Element element) throws NullPointerException {
        final Element previous = authoritativeTier.replace(element);
        if (previous != null) {
            // && !cache.replace(element.getObjectKey(), previous, element)) { todo this, replace would need to fault as well!
            cachingTier.remove(previous.getObjectKey());
        }
        return previous;
    }

    @Override
    public synchronized void dispose() {
        if (status == Status.STATUS_SHUTDOWN) {
            return;
        }
        if (cacheConfiguration != null && cacheConfiguration.isClearOnFlush()) {
            cachingTier.clear();
        }
        authoritativeTier.dispose();
        status = Status.STATUS_SHUTDOWN;
    }

    @Override
    public int getSize() {
        return authoritativeTier.getSize();
    }

    @Override
    public int getInMemorySize() {
        return cachingTier.getInMemorySize() + authoritativeTier.getInMemorySize();
    }

    @Override
    public int getOffHeapSize() {
        return cachingTier.getOffHeapSize() + authoritativeTier.getOffHeapSize();
    }

    @Override
    public int getOnDiskSize() {
        // As of now... a cache will never hold anything on disk!
        return authoritativeTier.getOnDiskSize();
    }

    @Override
    public int getTerracottaClusteredSize() {
        throw new UnsupportedOperationException("No such thing for non clustered stores!");
    }

    @Override
    public long getInMemorySizeInBytes() {
        // TODO delegate to pool accessors here ??
        return cachingTier.getInMemorySizeInBytes() + authoritativeTier.getInMemorySizeInBytes();
    }

    @Override
    public long getOffHeapSizeInBytes() {
        // TODO delegate to pool accessors here ??
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public long getOnDiskSizeInBytes() {
        // TODO delegate to pool accessors here ??
        return cachingTier.getOnDiskSizeInBytes() + authoritativeTier.getOnDiskSizeInBytes();
    }

    @Override
    public boolean hasAbortedSizeOf() {
        // TODO delegate to pool accessors here ??
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public boolean containsKey(final Object key) {
        return authoritativeTier.containsKey(key);
    }

    @Override
    @Deprecated
    public boolean containsKeyOnDisk(final Object key) {
        return authoritativeTier.containsKeyOnDisk(key);
    }

    @Override
    @Deprecated
    public boolean containsKeyOffHeap(final Object key) {
        return authoritativeTier.containsKeyOffHeap(key);
    }

    @Override
    @Deprecated
    public boolean containsKeyInMemory(final Object key) {
        return cachingTier.contains(key);
    }

    @Override
    public void expireElements() {
        authoritativeTier.expireElements();
    }

    @Override
    public void flush() throws IOException {
        if (cacheConfiguration != null && cacheConfiguration.isClearOnFlush()) {
            cachingTier.clear();
        }
        authoritativeTier.flush();
    }

    @Override
    public boolean bufferFull() {
        return authoritativeTier.bufferFull();
    }

    @Override
    public Policy getInMemoryEvictionPolicy() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void setInMemoryEvictionPolicy(final Policy policy) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    @Deprecated
    public Object getInternalContext() {
        return masterLocks;
    }

    @Override
    public boolean isCacheCoherent() {
        throw new UnsupportedOperationException("No such thing for non clustered stores!");
    }

    @Override
    public boolean isClusterCoherent() throws TerracottaNotRunningException {
        throw new UnsupportedOperationException("No such thing for non clustered stores!");
    }

    @Override
    public boolean isNodeCoherent() throws TerracottaNotRunningException {
        throw new UnsupportedOperationException("No such thing for non clustered stores!");
    }

    @Override
    public void setNodeCoherent(final boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
        throw new UnsupportedOperationException("No such thing for non clustered stores!");
    }

    @Override
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException, InterruptedException {
        throw new UnsupportedOperationException("No such thing for non clustered stores!");
    }

    @Override
    public Object getMBean() {
        return authoritativeTier.getMBean();
    }

    @Override
    public void setAttributeExtractors(final Map<String, AttributeExtractor> extractors) {
        authoritativeTier.setAttributeExtractors(extractors);
    }

    @Override
    public Results executeQuery(final StoreQuery query) throws SearchException {
        return authoritativeTier.executeQuery(query);
    }

    @Override
    public <T> Attribute<T> getSearchAttribute(final String attributeName) {
        return authoritativeTier.getSearchAttribute(attributeName);
    }

    // TODO this is probably not optimum
    @Override
    public Map<Object, Element> getAllQuiet(final Collection<?> keys) {
        final Map<Object, Element> result = new HashMap<Object, Element>();
        for (Object key : keys) {
            result.put(key, getQuiet(key));
        }
        return result;
    }

    // TODO this is probably not optimum
    @Override
    public Map<Object, Element> getAll(final Collection<?> keys) {
        final Map<Object, Element> result = new HashMap<Object, Element>();
        for (Object key : keys) {
            result.put(key, get(key));
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void recalculateSize(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

}
