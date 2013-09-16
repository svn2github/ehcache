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

package net.sf.ehcache.constructs.eventual;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.store.ElementValueComparator;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * StronglyConsistentCacheAccessor is a decorator that accepts distributed caches configured with eventual consistency.
 * <p/>
 * The accessor will then lock for all operations:
 * <ul>
 *   <li>using a read lock for read operations</li>
 *   <li>using a write lock for write operations</li>
 *   <li>using a write lock and compound operations for the CAS operations</li>
 * </ul>
 *
 * @author Louis Jacomet
 */
public class StronglyConsistentCacheAccessor extends EhcacheDecoratorAdapter {

    private final ElementValueComparator elementComparator;

    /**
     * Constructor accepting the cache to be decorated.
     *
     * @param underlyingCache a clustered cache configured with eventual consistency
     *
     * @throws IllegalArgumentException if the underlying cache is not clustered and has not
     * eventual consistency.
     */
    public StronglyConsistentCacheAccessor(Ehcache underlyingCache) throws IllegalArgumentException {
        super(underlyingCache);
        TerracottaConfiguration terracottaConfiguration = underlyingCache.getCacheConfiguration().getTerracottaConfiguration();
        if (terracottaConfiguration == null || terracottaConfiguration.getConsistency() != TerracottaConfiguration.Consistency.EVENTUAL) {
            throw new IllegalArgumentException("This decorator only accepts clustered cache with eventual consistency. " +
                                               underlyingCache.getName() + " is not such a cache.");
        }

        elementComparator = underlyingCache.getCacheConfiguration()
                .getElementValueComparatorConfiguration()
                .createElementComparatorInstance(underlyingCache.getCacheConfiguration());
    }

    @Override
    public Element putIfAbsent(Element element, boolean doNotNotifyCacheReplicators) throws NullPointerException {
        Object objectKey = element.getObjectKey();
        if (objectKey == null) {
            throw new NullPointerException();
        }

        acquireWriteLockOnKey(objectKey);
        try {
            Element current = getQuiet(objectKey);
            if (current == null) {
                super.put(element, doNotNotifyCacheReplicators);
            }
            return current;
        } finally {
            releaseWriteLockOnKey(objectKey);
        }
    }

    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        return putIfAbsent(element, false);
    }

    @Override
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        if (old.getObjectKey() == null || element.getObjectKey() == null) {
            throw new NullPointerException();
        }
        if (!old.getObjectKey().equals(element.getObjectKey())) {
            throw new IllegalArgumentException("The keys for the element arguments to replace must be equal");
        }

        Object objectKey = element.getObjectKey();

        acquireWriteLockOnKey(objectKey);
        try {
            if (elementComparator.equals(getQuiet(objectKey), old)) {
                super.put(element);
                return true;
            }
        } finally {
            releaseWriteLockOnKey(objectKey);
        }
        return false;
    }

    @Override
    public Element replace(Element element) throws NullPointerException {
        Object objectKey = element.getObjectKey();
        if (objectKey == null) {
            throw new NullPointerException();
        }
        acquireWriteLockOnKey(objectKey);
        try {
            Element current = getQuiet(objectKey);
            if (current != null) {
                super.put(element);
            }
            return current;
        } finally {
            releaseWriteLockOnKey(objectKey);
        }
    }

    @Override
    public boolean removeElement(Element element) throws NullPointerException {
        Object objectKey = element.getObjectKey();
        if (objectKey == null) {
            throw new NullPointerException();
        }

        acquireWriteLockOnKey(objectKey);
        try {
            Element current = getQuiet(objectKey);
            if (elementComparator.equals(current, element)) {
                return super.remove(objectKey);
            }
        } finally {
            releaseWriteLockOnKey(objectKey);
        }
        return false;
    }

    @Override
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (element == null) {
            // TODO Method javadoc says IllegalArgumentException on null element. However Cache.putInternal happily ignores null elements
            return;
        }

        Object objectKey = element.getObjectKey();

        acquireWriteLockOnKey(objectKey);
        try {
            super.put(element, doNotNotifyCacheReplicators);
        } finally {
            releaseWriteLockOnKey(objectKey);
        }
    }

    @Override
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        put(element, false);
    }

    @Override
    public void putAll(Collection<Element> elements) throws IllegalArgumentException, IllegalStateException, CacheException {
        for (Element element : elements) {
            put(element);
        }
    }

    @Override
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (element == null) {
            return;
        }

        Object objectKey = element.getObjectKey();
        acquireWriteLockOnKey(objectKey);
        try {
            super.putQuiet(element);
        } finally {
            releaseWriteLockOnKey(objectKey);
        }
    }

    @Override
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (element == null) {
            return;
        }

        Object objectKey = element.getObjectKey();
        acquireWriteLockOnKey(objectKey);
        try {
            super.putWithWriter(element);
        } finally {
            releaseWriteLockOnKey(objectKey);
        }
    }

    @Override
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        acquireWriteLockOnKey(key);
        try {
            return super.remove(key, doNotNotifyCacheReplicators);
        } finally {
            releaseWriteLockOnKey(key);
        }
    }

    @Override
    public void removeAll(Collection<?> keys) throws IllegalStateException {
        removeAll(keys, false);
    }

    @Override
    public boolean remove(Object key) throws IllegalStateException {
        return remove(key, false);
    }

    @Override
    public void removeAll(Collection<?> keys, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        for (Object key : keys) {
            remove(key);
        }
    }

    @Override
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object)key, doNotNotifyCacheReplicators);
    }

    @Override
    public boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object)key);
    }

    @Override
    public boolean removeQuiet(Object key) throws IllegalStateException {
        acquireWriteLockOnKey(key);
        try {
            return super.removeQuiet(key);
        } finally {
            releaseWriteLockOnKey(key);
        }
    }

    @Override
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object)key);
    }

    @Override
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        acquireWriteLockOnKey(key);
        try {
            return super.removeWithWriter(key);
        } finally {
            releaseWriteLockOnKey(key);
        }
    }

    @Override
    public Element removeAndReturnElement(Object key) throws IllegalStateException {
        acquireWriteLockOnKey(key);
        try {
            return super.removeAndReturnElement(key);
        } finally {
            releaseWriteLockOnKey(key);
        }
    }

    @Override
    public Element get(Object key) throws IllegalStateException, CacheException {
        acquireReadLockOnKey(key);
        try {
            return super.get(key);
        } finally {
            releaseReadLockOnKey(key);
        }
    }

    @Override
    public Map<Object, Element> getAll(Collection<?> keys) throws IllegalStateException, CacheException {
        HashMap<Object, Element> result = new HashMap<Object, Element>();
        for (Object key : keys) {
            result.put(key, get(key));
        }
        return result;
    }

    @Override
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object)key);
    }

    @Override
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        acquireReadLockOnKey(key);
        try {
            return super.getQuiet(key);
        } finally {
            releaseReadLockOnKey(key);
        }
    }

    @Override
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object)key);
    }

    @Override
    public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {
        acquireReadLockOnKey(key);
        try {
            return super.getWithLoader(key, loader, loaderArgument);
        } finally {
            releaseReadLockOnKey(key);
        }
    }

    @Override
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
        HashMap<Object, Object> result = new HashMap<Object, Object>(keys.size());
        for (Object key : keys) {
            Element element = getWithLoader(key, null, loaderArgument);
            if (element != null) {
                result.put(key, element.getObjectValue());
            } else {
                result.put(key, null);
            }
        }
        return result;
    }
}
