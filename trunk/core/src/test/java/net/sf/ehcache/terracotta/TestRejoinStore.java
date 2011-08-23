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

package net.sf.ehcache.terracotta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.constructs.nonstop.NullCacheLockProvider;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.writer.CacheWriterManager;

public class TestRejoinStore implements TerracottaStore {

    public enum StoreAction {
        NONE, BLOCKING, EXCEPTION;
    }

    private final Map<Object, Element> map = new HashMap<Object, Element>();
    private volatile StoreAction storeAction = StoreAction.NONE;
    private final CacheLockProvider cacheLockProvider = new NullCacheLockProvider();
    private final List<String> calledMethods = new ArrayList<String>();

    public void setBlocking(boolean blocking) {
        if (blocking) {
            this.setStoreAction(StoreAction.BLOCKING);
        } else {
            this.setStoreAction(StoreAction.NONE);
        }
    }

    public void setStoreAction(StoreAction storeAction) {
        this.storeAction = storeAction;
    }

    public synchronized List<String> getCalledMethods() {
        return calledMethods;
    }

    public synchronized void clearCalledMethods() {
        calledMethods.clear();
    }

    private void alwaysCalledMethod() {
        StackTraceElement lastMethod = new Exception().getStackTrace()[1];
        synchronized (this) {
            calledMethods.add(lastMethod.getMethodName());
        }

        switch (storeAction) {
            case BLOCKING: {
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            case EXCEPTION: {
                throw new RuntimeException("You want exception, you get it");
            }
        }
    }

    public void addStoreListener(StoreListener listener) {
        alwaysCalledMethod();

    }

    public boolean bufferFull() {
        return false;
    }

    public boolean containsKey(Object key) {
        alwaysCalledMethod();
        return map.containsKey(key);
    }

    public boolean containsKeyInMemory(Object key) {
        alwaysCalledMethod();
        return map.containsKey(key);
    }

    public boolean containsKeyOffHeap(Object key) {
        alwaysCalledMethod();
        return false;
    }

    public boolean containsKeyOnDisk(Object key) {
        alwaysCalledMethod();
        return false;
    }

    public void dispose() {
        alwaysCalledMethod();

    }

    public Results executeQuery(StoreQuery query) {
        alwaysCalledMethod();
        return null;
    }

    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        alwaysCalledMethod();
        return null;
    }

    public void expireElements() {
        alwaysCalledMethod();

    }

    public void flush() throws IOException {
        alwaysCalledMethod();

    }

    public Element get(Object key) {
        alwaysCalledMethod();
        return map.get(key);
    }

    public Policy getInMemoryEvictionPolicy() {
        alwaysCalledMethod();
        return null;
    }

    public int getInMemorySize() {
        alwaysCalledMethod();
        return 0;
    }

    public long getInMemorySizeInBytes() {
        alwaysCalledMethod();
        return 0;
    }

    public Object getInternalContext() {
        alwaysCalledMethod();
        return cacheLockProvider;
    }

    public List getKeys() {
        alwaysCalledMethod();
        return new ArrayList(map.keySet());
    }

    public Object getMBean() {
        alwaysCalledMethod();
        return null;
    }

    public int getOffHeapSize() {
        alwaysCalledMethod();
        return 0;
    }

    public long getOffHeapSizeInBytes() {
        alwaysCalledMethod();
        return 0;
    }

    public int getOnDiskSize() {
        alwaysCalledMethod();
        return 0;
    }

    public long getOnDiskSizeInBytes() {
        alwaysCalledMethod();
        return 0;
    }

    public boolean hasAbortedSizeOf() {
        alwaysCalledMethod();
        return false;
    }

    public Element getQuiet(Object key) {
        alwaysCalledMethod();
        return map.get(key);
    }

    public Map<Object, Element> getAllQuiet(Collection<Object> keys) {
        alwaysCalledMethod();
        Map<Object, Element> rv = new HashMap<Object, Element>();
        for (Object key : keys) {
            rv.put(key, map.get(key));
        }
        return rv;
    }

    public Map<Object, Element> getAll(Collection<Object> keys) {
        return getAllQuiet(keys);
    }

    public int getSize() {
        alwaysCalledMethod();
        return 0;
    }

    public Status getStatus() {
        alwaysCalledMethod();
        return null;
    }

    public int getTerracottaClusteredSize() {
        alwaysCalledMethod();
        return 0;
    }

    public boolean isCacheCoherent() {
        alwaysCalledMethod();
        return false;
    }

    public boolean isClusterCoherent() {
        alwaysCalledMethod();
        return true;
    }

    public boolean isNodeCoherent() {
        alwaysCalledMethod();
        return true;
    }

    public boolean put(Element element) throws CacheException {
        alwaysCalledMethod();
        return map.put(element.getKey(), element) == null;
    }

    public void putAll(Collection<Element> elements) throws CacheException {
        alwaysCalledMethod();
        for (Element element : elements) {
            put(element);
        }
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        alwaysCalledMethod();
        return map.put(element.getKey(), element);
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        alwaysCalledMethod();
        return map.put(element.getKey(), element) == null;
    }

    public Element remove(Object key) {
        alwaysCalledMethod();
        return map.remove(key);
    }

    public void removeAll(Collection<Object> keys) {
        alwaysCalledMethod();
        for(Object key : keys) {
            map.remove(key);
        }
    }

    public void removeAll() throws CacheException {
        alwaysCalledMethod();
        map.clear();
    }

    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        alwaysCalledMethod();
        return null;
    }

    public void removeStoreListener(StoreListener listener) {
        alwaysCalledMethod();

    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        alwaysCalledMethod();
        return null;
    }

    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        alwaysCalledMethod();
        return false;
    }

    public Element replace(Element element) throws NullPointerException {
        alwaysCalledMethod();
        return null;
    }

    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        alwaysCalledMethod();

    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        alwaysCalledMethod();

    }

    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        alwaysCalledMethod();

    }

    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        alwaysCalledMethod();

    }

    public Set getLocalKeys() {
        alwaysCalledMethod();
        return null;
    }

    public Element unlockedGet(Object key) {
        alwaysCalledMethod();
        return null;
    }

    public Element unlockedGetQuiet(Object key) {
        alwaysCalledMethod();
        return null;
    }

    public Element unsafeGet(Object key) {
        alwaysCalledMethod();
        return null;
    }

    public Element unsafeGetQuiet(Object key) {
        alwaysCalledMethod();
        return null;
    }

}
