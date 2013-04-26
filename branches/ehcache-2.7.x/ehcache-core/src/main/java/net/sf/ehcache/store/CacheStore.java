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
import net.sf.ehcache.store.disk.DiskStore;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.terracotta.context.annotations.ContextChild;

/**
 * The one store to rule them all!
 *
 * @author Alex Snaps
 */
public class CacheStore implements Store {

    private static final int DEFAULT_LOCK_STRIPE_COUNT = 128;

    @ContextChild
    private final CachingTier<Object, Element> cachingTier;
    @ContextChild
    private final AuthoritativeTier authoritativeTier;

    @Deprecated
    private final StripedReadWriteLock masterLocks;

    @Deprecated
    private final CacheConfiguration cacheConfiguration;
    private volatile Status status;

    private final ReadWriteLock daLock = new ReentrantReadWriteLock();

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
    public void addStoreListener(final StoreListener listener) {
        authoritativeTier.addStoreListener(listener);
    }

    @Override
    public void removeStoreListener(final StoreListener listener) {
        authoritativeTier.removeStoreListener(listener);
    }

    @Override
    public boolean put(final Element element) throws CacheException {
        if (cachingTier.remove(element.getObjectKey()) != null || cachingTier.loadOnPut()) {
            try {
                final boolean[] hack = new boolean[1];
                if (cachingTier.get(element.getObjectKey(), new Callable<Element>() {
                    @Override
                    public Element call() throws Exception {
                        final Lock lock = daLock.readLock();
                        lock.lock();
                        try {
                            hack[0] = authoritativeTier.putFaulted(element);
                            return element;
                        } finally {
                            lock.unlock();
                        }
                    }
                }, false) == element) {
                    return hack[0];
                }
            } catch (Throwable e) {
                cachingTier.remove(element.getObjectKey());
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                }
                throw new CacheException(e);
            }
        }
        
        try {
            return authoritativeTier.put(element);
        } finally {
            cachingTier.remove(element.getObjectKey());
        }
    }

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
                final Lock lock = daLock.readLock();
                lock.lock();
                try {
                    return authoritativeTier.fault(key, true);
                } finally {
                    lock.unlock();
                }
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
                final Lock lock = daLock.readLock();
                lock.lock();
                try {
                    return authoritativeTier.fault(key, false);
                } finally {
                    lock.unlock();
                }
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
        final Lock lock = daLock.writeLock();
        lock.lock();
        try {
            authoritativeTier.removeAll();
        } finally {
            cachingTier.clear();
            lock.unlock();
        }
    }

    @Override
    public Element putIfAbsent(final Element element) throws NullPointerException {
        Element previous = null;
        try {
            previous = authoritativeTier.putIfAbsent(element);
        } finally {
            if (previous == null) {
                cachingTier.remove(element.getObjectKey());
            }
        }
        return previous;
    }

    @Override
    public Element removeElement(final Element element, final ElementValueComparator comparator) throws NullPointerException {
        final Element removedElement;
        try {
            removedElement = authoritativeTier.removeElement(element, comparator);
        } finally {
            cachingTier.remove(element.getObjectKey());
        }
        return removedElement;
    }

    @Override
    public boolean replace(final Element old, final Element element, final ElementValueComparator comparator)
        throws NullPointerException, IllegalArgumentException {

        boolean replaced = true;
        try {
            replaced = authoritativeTier.replace(old, element, comparator);
        } finally {
            if (replaced) {
                cachingTier.remove(element.getObjectKey());
            }
        }
        return replaced;
    }

    @Override
    public Element replace(final Element element) throws NullPointerException {
        Element previous = null;
        try {
            previous = authoritativeTier.replace(element);
        } catch (Throwable e) {
            cachingTier.remove(previous.getObjectKey());
            throwUp(e);
        } finally {
            if (previous != null) {
                cachingTier.remove(previous.getObjectKey());
            }
        }
        return previous;
    }

    private void throwUp(final Throwable e) {
        if (e instanceof RuntimeException) {
            throw ((RuntimeException)e);
        } else if (e instanceof Error) {
            throw ((Error)e);
        }
        throw new CacheException(e);
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
        return cachingTier.getOffHeapSizeInBytes() + authoritativeTier.getOffHeapSizeInBytes();
    }

    @Override
    public long getOnDiskSizeInBytes() {
        // TODO delegate to pool accessors here ??
        return cachingTier.getOnDiskSizeInBytes() + authoritativeTier.getOnDiskSizeInBytes();
    }

    @Override
    public boolean hasAbortedSizeOf() {
        // TODO delegate to pool accessors here ??
        return authoritativeTier.hasAbortedSizeOf();
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
        if (authoritativeTier instanceof DiskStore && cacheConfiguration != null && cacheConfiguration.isClearOnFlush()) {
            final Lock lock = daLock.writeLock();
            lock.lock();
            try {
                cachingTier.clear();
                ((DiskStore)authoritativeTier).clearFaultedBit();
            } finally {
                lock.unlock();
            }
        } else {
            authoritativeTier.flush();
        }
    }

    @Override
    public boolean bufferFull() {
        return authoritativeTier.bufferFull();
    }

    @Override
    public Policy getInMemoryEvictionPolicy() {
        return cachingTier.getEvictionPolicy();
    }

    @Override
    public void setInMemoryEvictionPolicy(final Policy policy) {
        cachingTier.setEvictionPolicy(policy);
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
        return result;
    }

    @Override
    public void recalculateSize(final Object key) {
        cachingTier.recalculateSize(key);
    }

}
