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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.writer.CacheWriterManager;

public class TestRejoinStore implements Store {

    private final Map<Object, Element> map = new HashMap<Object, Element>();
    private volatile boolean blocking = false;

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    private void checkBlocking() {
        if (blocking) {
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void addStoreListener(StoreListener listener) {
        checkBlocking();

    }

    public boolean bufferFull() {
        return false;
    }

    public boolean containsKey(Object key) {
        checkBlocking();
        return map.containsKey(key);
    }

    public boolean containsKeyInMemory(Object key) {
        checkBlocking();
        return map.containsKey(key);
    }

    public boolean containsKeyOffHeap(Object key) {
        checkBlocking();
        return false;
    }

    public boolean containsKeyOnDisk(Object key) {
        checkBlocking();
        return false;
    }

    public void dispose() {
        checkBlocking();

    }

    public Results executeQuery(StoreQuery query) {
        checkBlocking();
        return null;
    }

    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        checkBlocking();
        return null;
    }

    public void expireElements() {
        checkBlocking();

    }

    public void flush() throws IOException {
        checkBlocking();

    }

    public Element get(Object key) {
        checkBlocking();
        return map.get(key);
    }

    public Policy getInMemoryEvictionPolicy() {
        checkBlocking();
        return null;
    }

    public int getInMemorySize() {
        checkBlocking();
        return 0;
    }

    public long getInMemorySizeInBytes() {
        checkBlocking();
        return 0;
    }

    public Object getInternalContext() {
        checkBlocking();
        return null;
    }

    public List getKeys() {
        checkBlocking();
        return new ArrayList(map.keySet());
    }

    public Object getMBean() {
        checkBlocking();
        return null;
    }

    public int getOffHeapSize() {
        checkBlocking();
        return 0;
    }

    public long getOffHeapSizeInBytes() {
        checkBlocking();
        return 0;
    }

    public int getOnDiskSize() {
        checkBlocking();
        return 0;
    }

    public long getOnDiskSizeInBytes() {
        checkBlocking();
        return 0;
    }

    public Element getQuiet(Object key) {
        checkBlocking();
        return map.get(key);
    }

    public int getSize() {
        checkBlocking();
        return 0;
    }

    public Status getStatus() {
        checkBlocking();
        return null;
    }

    public int getTerracottaClusteredSize() {
        checkBlocking();
        return 0;
    }

    public boolean isCacheCoherent() {
        checkBlocking();
        return false;
    }

    public boolean isClusterCoherent() {
        checkBlocking();
        return true;
    }

    public boolean isNodeCoherent() {
        checkBlocking();
        return true;
    }

    public boolean put(Element element) throws CacheException {
        checkBlocking();
        return map.put(element.getKey(), element) == null;
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        checkBlocking();
        return map.put(element.getKey(), element);
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        checkBlocking();
        return map.put(element.getKey(), element) == null;
    }

    public Element remove(Object key) {
        checkBlocking();
        return map.remove(key);
    }

    public void removeAll() throws CacheException {
        checkBlocking();
        map.clear();
    }

    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        checkBlocking();
        return null;
    }

    public void removeStoreListener(StoreListener listener) {
        checkBlocking();

    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        checkBlocking();
        return null;
    }

    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        checkBlocking();
        return false;
    }

    public Element replace(Element element) throws NullPointerException {
        checkBlocking();
        return null;
    }

    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        checkBlocking();

    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        checkBlocking();

    }

    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        checkBlocking();

    }

    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        checkBlocking();

    }

}
