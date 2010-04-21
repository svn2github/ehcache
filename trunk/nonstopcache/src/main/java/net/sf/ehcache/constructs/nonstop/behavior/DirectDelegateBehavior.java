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

package net.sf.ehcache.constructs.nonstop.behavior;

import java.io.Serializable;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;

/**
 * A {@link NonStopCacheBehavior} that directly delegates to an underlying {@link Ehcache}
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class DirectDelegateBehavior implements NonStopCacheBehavior {

    private final Ehcache underlyingCache;

    /**
     * Constructor accepting the underlying cache
     * 
     * @param underlyingCache
     */
    public DirectDelegateBehavior(final Ehcache underlyingCache) {
        this.underlyingCache = underlyingCache;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Element get(final Object key) throws IllegalStateException, CacheException {
        return underlyingCache.get(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public List getKeys() throws IllegalStateException, CacheException {
        return underlyingCache.getKeys();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return underlyingCache.getKeysNoDuplicateCheck();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        return underlyingCache.getKeysWithExpiryCheck();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        return underlyingCache.getQuiet(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean isKeyInCache(final Object key) {
        return underlyingCache.isKeyInCache(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean isValueInCache(final Object value) {
        return underlyingCache.isValueInCache(value);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        underlyingCache.put(element, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.put(element);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.putQuiet(element);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.putWithWriter(element);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return underlyingCache.remove(key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean remove(final Object key) throws IllegalStateException {
        return underlyingCache.remove(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void removeAll() throws IllegalStateException, CacheException {
        underlyingCache.removeAll();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        underlyingCache.removeAll(doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return underlyingCache.calculateInMemorySize();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void evictExpiredElements() {
        underlyingCache.evictExpiredElements();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public void flush() throws IllegalStateException, CacheException {
        underlyingCache.flush();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public int getDiskStoreSize() throws IllegalStateException {
        return underlyingCache.getDiskStoreSize();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Object getInternalContext() {
        return underlyingCache.getInternalContext();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public long getMemoryStoreSize() throws IllegalStateException {
        return underlyingCache.getMemoryStoreSize();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public int getSize() throws IllegalStateException, CacheException {
        return underlyingCache.getSize();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        return underlyingCache.getSizeBasedOnAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Statistics getStatistics() throws IllegalStateException {
        return underlyingCache.getStatistics();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean isElementInMemory(Object key) {
        return underlyingCache.isElementInMemory(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean isElementOnDisk(Object key) {
        return underlyingCache.isElementOnDisk(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return underlyingCache.putIfAbsent(element);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean removeElement(Element element) throws NullPointerException {
        return underlyingCache.removeElement(element);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean removeQuiet(Object key) throws IllegalStateException {
        return underlyingCache.removeQuiet(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        return underlyingCache.removeWithWriter(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        return underlyingCache.replace(old, element);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Directly delegates to the underlying cache
     */
    public Element replace(Element element) throws NullPointerException {
        return underlyingCache.replace(element);
    }

}
