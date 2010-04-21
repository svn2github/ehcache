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
import java.util.Collections;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;

/**
 * Implementation of {@link NonStopCacheBehavior} which returns null for all get
 * operations and does nothing for puts and removes. (Null Object pattern)
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class NoOpOnTimeoutBehavior implements NonStopCacheBehavior {

    /**
     * the singleton instance
     */
    private static final NoOpOnTimeoutBehavior INSTANCE = new NoOpOnTimeoutBehavior();

    /**
     * Returns the singleton instance
     * 
     * @return the singleton instance
     */
    public static NoOpOnTimeoutBehavior getInstance() {
        return INSTANCE;
    }

    /**
     * private constructor
     */
    private NoOpOnTimeoutBehavior() {
        //
    }

    /**
     * {@inheritDoc}.
     * No-op operation and returns null
     */
    public Element get(final Object key) throws IllegalStateException, CacheException {
        return null;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        return null;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object) key);
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public List getKeys() throws IllegalStateException, CacheException {
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean isKeyInCache(final Object key) {
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean isValueInCache(final Object value) {
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean remove(final Object key) throws IllegalStateException {
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void removeAll() throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void evictExpiredElements() {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public void flush() throws IllegalStateException, CacheException {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public int getDiskStoreSize() throws IllegalStateException {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public Object getInternalContext() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public long getMemoryStoreSize() throws IllegalStateException {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public int getSize() throws IllegalStateException, CacheException {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public Statistics getStatistics() throws IllegalStateException {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean isElementInMemory(Object key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean isElementInMemory(Serializable key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean isElementOnDisk(Object key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean isElementOnDisk(Serializable key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean removeElement(Element element) throws NullPointerException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean removeQuiet(Object key) throws IllegalStateException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * No-op operation and returns null
     */
    public Element replace(Element element) throws NullPointerException {
        // no-op
        return null;
    }

}