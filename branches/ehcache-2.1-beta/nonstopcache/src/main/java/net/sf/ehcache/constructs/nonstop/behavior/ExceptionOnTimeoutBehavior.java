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
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

/**
 * Implementation of {@link NonStopCacheBehavior} which throws {@link NonStopCacheException} for all operations.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class ExceptionOnTimeoutBehavior implements NonStopCacheBehavior {

    /**
     * the singleton instance
     */
    private static final ExceptionOnTimeoutBehavior INSTANCE = new ExceptionOnTimeoutBehavior();

    /**
     * throw news the singleton instance
     * 
     * @throw new the singleton instance
     */
    public static ExceptionOnTimeoutBehavior getInstance() {
        return INSTANCE;
    }

    /**
     * private constructor
     */
    private ExceptionOnTimeoutBehavior() {
        //
    }

    /**
     * {@inheritDoc}
     */
    public Element get(final Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("get for key - '" + key + "'  timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getQuite for key - '" + key + "'  timed out");
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

    /**
     * {@inheritDoc}
     */
    public List getKeys() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getKeys timed out");
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        throw new NonStopCacheException("getKeysNoDuplicateCheck timed out");
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getKeysWithExpiryCheck timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyInCache(final Object key) {
        throw new NonStopCacheException("isKeyInCache timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValueInCache(final Object value) {
        throw new NonStopCacheException("isValueInCache timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        throw new NonStopCacheException("put for element - '" + element + "', doNotNotifyCacheReplicators - '"
                + doNotNotifyCacheReplicators + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("put for element - '" + element + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("putQuiet for element - '" + element + "' timed out");

    }

    /**
     * {@inheritDoc}
     */
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("putWithWriter for element - '" + element + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        throw new NonStopCacheException("remove for key - '" + key + "', doNotNotifyCacheReplicators - '" + doNotNotifyCacheReplicators
                + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object key) throws IllegalStateException {
        throw new NonStopCacheException("remove for key - '" + key + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeAll timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeAll with doNotNotifyCacheReplicators - '" + doNotNotifyCacheReplicators + "' timed out");
    }

    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("calculateInMemorySize timed out");
    }

    public void evictExpiredElements() {
        throw new NonStopCacheException("evictExpiredElements timed out");
    }

    public void flush() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("flush timed out");
    }

    public int getDiskStoreSize() throws IllegalStateException {
        throw new NonStopCacheException("getDiskStoreSize timed out");
    }

    public Object getInternalContext() {
        throw new NonStopCacheException("getInternalContext timed out");
    }

    public long getMemoryStoreSize() throws IllegalStateException {
        throw new NonStopCacheException("getMemoryStoreSize timed out");
    }

    public int getSize() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getSize timed out");
    }

    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("getSizeBasedOnAccuracy statisticsAccuracytimed out");
    }

    public Statistics getStatistics() throws IllegalStateException {
        throw new NonStopCacheException("getStatistics timed out");
    }

    public boolean isElementInMemory(Object key) {
        throw new NonStopCacheException("isElementInMemory timed out");
    }

    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    public boolean isElementOnDisk(Object key) {
        throw new NonStopCacheException("isElementOnDisk timed out");
    }

    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        throw new NonStopCacheException("putIfAbsent timed out");
    }

    public boolean removeElement(Element element) throws NullPointerException {
        throw new NonStopCacheException("removeElement timed out");
    }

    public boolean removeQuiet(Object key) throws IllegalStateException {
        throw new NonStopCacheException("removeQuiet timed out");
    }

    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeWithWriter timed out");
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        throw new NonStopCacheException("replace timed out");
    }

    public Element replace(Element element) throws NullPointerException {
        throw new NonStopCacheException("replace timed out");
    }
}
