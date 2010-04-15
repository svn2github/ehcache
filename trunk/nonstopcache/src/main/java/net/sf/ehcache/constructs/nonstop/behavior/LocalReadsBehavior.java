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
import java.util.Arrays;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.NonStopCacheHelper;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.TerracottaStore;

public class LocalReadsBehavior implements NonStopCacheBehavior {

    private final TerracottaStore unsafeStore;

    public LocalReadsBehavior(Cache cache) {
        Store store = new NonStopCacheHelper(cache).getStore();
        if (!(store instanceof TerracottaStore)) {
            throw new IllegalArgumentException(LocalReadsBehavior.class.getName()
                    + " can be only be used with Terracotta clustered caches.");
        }
        this.unsafeStore = (TerracottaStore) store;
    }

    public Element get(Object key) throws IllegalStateException, CacheException {
        return unsafeStore.unsafeGet(key);
    }

    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    public List getKeys() throws IllegalStateException, CacheException {
        return Arrays.asList(unsafeStore.getKeyArray());
    }

    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return getKeys();
    }

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

    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        return unsafeStore.unsafeGetQuiet(key);
    }

    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    public boolean isKeyInCache(Object key) {
        if (key == null) {
            return false;
        }
        return getQuiet(key) != null;
    }

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

    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        // no-op
    }

    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        // no-op
        return false;
    }

    public boolean remove(Object key) throws IllegalStateException {
        // no-op
        return false;
    }

    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        // no-op
        return false;
    }

    public boolean remove(Serializable key) throws IllegalStateException {
        // no-op
        return false;
    }

    public void removeAll() throws IllegalStateException, CacheException {
        // no-op
    }

    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        // no-op
    }

    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return 0;
    }

    public void evictExpiredElements() {
        // no-op
    }

    public void flush() throws IllegalStateException, CacheException {
        // no-op
    }

    public int getDiskStoreSize() throws IllegalStateException {
        return 0;
    }

    public Object getInternalContext() {
        return null;
    }

    public long getMemoryStoreSize() throws IllegalStateException {
        return 0;
    }

    public int getSize() throws IllegalStateException, CacheException {
        return 0;
    }

    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        return 0;
    }

    public Statistics getStatistics() throws IllegalStateException {
        return null;
    }

    public boolean isElementInMemory(Object key) {
        if (key == null) {
            return false;
        }
        return getQuiet(key) != null;
    }

    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    public boolean isElementOnDisk(Object key) {
        return false;
    }

    public boolean isElementOnDisk(Serializable key) {
        return false;
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        return null;
    }

    public boolean removeElement(Element element) throws NullPointerException {
        return false;
    }

    public boolean removeQuiet(Object key) throws IllegalStateException {
        return false;
    }

    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return false;
    }

    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        return false;
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        return false;
    }

    public Element replace(Element element) throws NullPointerException {
        return null;
    }

}