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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.TerracottaStore;

/**
 * A {@link NonStopCacheBehavior} implementation that returns the local value in the VM, if present, for get operations and no-op for put,
 * remove operations
 *
 * @author Abhishek Sanoujam
 *
 */
public class LocalReadsBehavior implements NonStopCacheBehavior {

    private final TerracottaStore unsafeStore;

    /**
     * Constructor accepting the underlying {@link Cache}
     *
     * @param cache
     */
    public LocalReadsBehavior(Cache cache) {
        Store store = new CacheStoreHelper(cache).getStore();
        if (!(store instanceof TerracottaStore)) {
            throw new IllegalArgumentException(LocalReadsBehavior.class.getName()
                    + " can be only be used with Terracotta clustered caches.");
        }
        this.unsafeStore = (TerracottaStore) store;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public Element get(Object key) throws IllegalStateException, CacheException {
        return unsafeStore.unsafeGet(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public List getKeys() throws IllegalStateException, CacheException {
        return Collections.unmodifiableList(new ArrayList(unsafeStore.getLocalKeys()));
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return getKeys();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        List allKeyList = getKeys();
        ArrayList<Object> nonExpiredKeys = new ArrayList<Object>(allKeyList.size());
        int allKeyListSize = allKeyList.size();
        for (Object key : allKeyList) {
            Element element = getQuiet(key);
            if (element != null) {
                nonExpiredKeys.add(key);
            }
        }
        nonExpiredKeys.trimToSize();
        return nonExpiredKeys;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        return unsafeStore.unsafeGetQuiet(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public boolean isKeyInCache(Object key) {
        if (key == null) {
            return false;
        }
        return getQuiet(key) != null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public boolean isValueInCache(Object value) {
        for (Object key : getKeys()) {
            Element element = get(key);
            if (element != null) {
                Object elementValue = element.getValue();
                if (elementValue == null) {
                    if (value == null) {
                        return true;
                    }
                } else {
                    if (elementValue.equals(value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean remove(Object key) throws IllegalStateException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void removeAll() throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void evictExpiredElements() {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void flush() throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op and always returns zero
     */
    public int getDiskStoreSize() throws IllegalStateException {
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op and always returns null
     */
    public Object getInternalContext() {
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op and always returns zero
     */
    public long getMemoryStoreSize() throws IllegalStateException {
        return getKeys().size();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op and always returns zero
     */
    public int getSize() throws IllegalStateException, CacheException {
        return getKeys().size();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        return getKeys().size();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Statistics getStatistics() throws IllegalStateException {
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean isElementInMemory(Object key) {
        if (key == null) {
            return false;
        }
        return getQuiet(key) != null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean isElementOnDisk(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean isElementOnDisk(Serializable key) {
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean removeElement(Element element) throws NullPointerException {
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean removeQuiet(Object key) throws IllegalStateException {
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Element replace(Element element) throws NullPointerException {
        return null;
    }

}
