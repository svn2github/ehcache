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

public class DirectDelegateBehavior implements NonStopCacheBehavior {

    private final Ehcache underlyingCache;

    public DirectDelegateBehavior(final Ehcache underlyingCache) {
        this.underlyingCache = underlyingCache;
    }

    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

    public boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object) key);
    }

    public Element get(final Object key) throws IllegalStateException, CacheException {
        return underlyingCache.get(key);
    }

    public List getKeys() throws IllegalStateException, CacheException {
        return underlyingCache.getKeys();
    }

    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return underlyingCache.getKeysNoDuplicateCheck();
    }

    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        return underlyingCache.getKeysWithExpiryCheck();
    }

    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        return underlyingCache.getQuiet(key);
    }

    public boolean isKeyInCache(final Object key) {
        return underlyingCache.isKeyInCache(key);
    }

    public boolean isValueInCache(final Object value) {
        return underlyingCache.isValueInCache(value);
    }

    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        underlyingCache.put(element, doNotNotifyCacheReplicators);
    }

    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.put(element);
    }

    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.putQuiet(element);
    }

    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.putWithWriter(element);
    }

    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return underlyingCache.remove(key, doNotNotifyCacheReplicators);
    }

    public boolean remove(final Object key) throws IllegalStateException {
        return underlyingCache.remove(key);
    }

    public void removeAll() throws IllegalStateException, CacheException {
        underlyingCache.removeAll();
    }

    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        underlyingCache.removeAll(doNotNotifyCacheReplicators);
    }

    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return underlyingCache.calculateInMemorySize();
    }

    public void evictExpiredElements() {
        underlyingCache.evictExpiredElements();
    }

    public void flush() throws IllegalStateException, CacheException {
        underlyingCache.flush();
    }

    public int getDiskStoreSize() throws IllegalStateException {
        return underlyingCache.getDiskStoreSize();
    }

    public Object getInternalContext() {
        return underlyingCache.getInternalContext();
    }

    public long getMemoryStoreSize() throws IllegalStateException {
        return underlyingCache.getMemoryStoreSize();
    }

    public int getSize() throws IllegalStateException, CacheException {
        return underlyingCache.getSize();
    }

    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        return underlyingCache.getSizeBasedOnAccuracy(statisticsAccuracy);
    }

    public Statistics getStatistics() throws IllegalStateException {
        return underlyingCache.getStatistics();
    }

    public boolean isElementInMemory(Object key) {
        return underlyingCache.isElementInMemory(key);
    }

    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    public boolean isElementOnDisk(Object key) {
        return underlyingCache.isElementOnDisk(key);
    }

    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        return underlyingCache.putIfAbsent(element);
    }

    public boolean removeElement(Element element) throws NullPointerException {
        return underlyingCache.removeElement(element);
    }

    public boolean removeQuiet(Object key) throws IllegalStateException {
        return underlyingCache.removeQuiet(key);
    }

    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        return underlyingCache.removeWithWriter(key);
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        return underlyingCache.replace(old, element);
    }

    public Element replace(Element element) throws NullPointerException {
        return underlyingCache.replace(element);
    }

}
