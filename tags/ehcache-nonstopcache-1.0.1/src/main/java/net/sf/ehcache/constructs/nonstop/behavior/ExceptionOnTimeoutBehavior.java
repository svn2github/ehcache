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
 * Implementation of {@link NonStopCacheBehavior} that throws {@link NonStopCacheException} for all operations.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public final class ExceptionOnTimeoutBehavior implements NonStopCacheBehavior {

    /**
     * the singleton instance
     */
    private static final ExceptionOnTimeoutBehavior INSTANCE = new ExceptionOnTimeoutBehavior();

    /**
     * private constructor
     */
    private ExceptionOnTimeoutBehavior() {
        //
    }

    /**
     * returns the singleton instance
     * 
     */
    public static ExceptionOnTimeoutBehavior getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
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

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public List getKeys() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getKeys timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        throw new NonStopCacheException("getKeysNoDuplicateCheck timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getKeysWithExpiryCheck timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isKeyInCache(final Object key) {
        throw new NonStopCacheException("isKeyInCache timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isValueInCache(final Object value) {
        throw new NonStopCacheException("isValueInCache timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        throw new NonStopCacheException("put for element - '" + element + "', doNotNotifyCacheReplicators - '"
                + doNotNotifyCacheReplicators + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("put for element - '" + element + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("putQuiet for element - '" + element + "' timed out");

    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("putWithWriter for element - '" + element + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        throw new NonStopCacheException("remove for key - '" + key + "', doNotNotifyCacheReplicators - '" + doNotNotifyCacheReplicators
                + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean remove(final Object key) throws IllegalStateException {
        throw new NonStopCacheException("remove for key - '" + key + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeAll timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeAll with doNotNotifyCacheReplicators - '" + doNotNotifyCacheReplicators + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("calculateInMemorySize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void evictExpiredElements() {
        throw new NonStopCacheException("evictExpiredElements timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void flush() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("flush timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getDiskStoreSize() throws IllegalStateException {
        throw new NonStopCacheException("getDiskStoreSize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Object getInternalContext() {
        throw new NonStopCacheException("getInternalContext timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public long getMemoryStoreSize() throws IllegalStateException {
        throw new NonStopCacheException("getMemoryStoreSize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getSize() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getSize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("getSizeBasedOnAccuracy statisticsAccuracytimed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Statistics getStatistics() throws IllegalStateException {
        throw new NonStopCacheException("getStatistics timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isElementInMemory(Object key) {
        throw new NonStopCacheException("isElementInMemory timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isElementOnDisk(Object key) {
        throw new NonStopCacheException("isElementOnDisk timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        throw new NonStopCacheException("putIfAbsent timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean removeElement(Element element) throws NullPointerException {
        throw new NonStopCacheException("removeElement timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean removeQuiet(Object key) throws IllegalStateException {
        throw new NonStopCacheException("removeQuiet timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeWithWriter timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        throw new NonStopCacheException("replace timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element replace(Element element) throws NullPointerException {
        throw new NonStopCacheException("replace timed out");
    }
}
