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

package net.sf.ehcache.constructs.nonstop;

import java.io.IOException;
import java.util.Collections;
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

public class MockStoreWithFlag implements Store {

    private boolean accessFlag = false;
    private String lastMethodInvoked;

    public void markAccessFlag() {
        accessFlag = true;
        Exception exception = new Exception();
        exception.fillInStackTrace();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StackTraceElement lastStackTraceElement = stackTrace[1];
        lastMethodInvoked = lastStackTraceElement.getMethodName();
    }

    public String getLastMethodInvoked() {
        return lastMethodInvoked;
    }

    public boolean isAccessFlagMarked() {
        return accessFlag;
    }

    public void clearAccessFlag() {
        this.accessFlag = false;
        this.lastMethodInvoked = "";
    }

    public boolean bufferFull() {
        markAccessFlag();
        return false;
    }

    public boolean containsKey(Object key) {
        markAccessFlag();
        return false;
    }

    public boolean containsKeyInMemory(Object key) {
        markAccessFlag();
        return false;
    }

    public boolean containsKeyOnDisk(Object key) {
        markAccessFlag();
        return false;
    }

    public void dispose() {
        markAccessFlag();

    }

    public void expireElements() {
        markAccessFlag();

    }

    public void flush() throws IOException {
        markAccessFlag();

    }

    public Element get(Object key) {
        markAccessFlag();
        return null;
    }

    public Policy getInMemoryEvictionPolicy() {
        markAccessFlag();
        return null;
    }

    public int getInMemorySize() {
        markAccessFlag();
        return 0;
    }

    public long getInMemorySizeInBytes() {
        markAccessFlag();
        return 0;
    }

    public Object getInternalContext() {
        markAccessFlag();
        return null;
    }

    public List getKeys() {
        markAccessFlag();
        return Collections.EMPTY_LIST;
    }

    public int getOnDiskSize() {
        markAccessFlag();
        return 0;
    }

    public long getOnDiskSizeInBytes() {
        markAccessFlag();
        return 0;
    }

    public Element getQuiet(Object key) {
        markAccessFlag();
        return null;
    }

    public int getSize() {
        markAccessFlag();
        return 0;
    }

    public Status getStatus() {
        markAccessFlag();
        return null;
    }

    public int getTerracottaClusteredSize() {
        markAccessFlag();
        return 0;
    }

    public boolean isCacheCoherent() {
        markAccessFlag();
        return false;
    }

    public boolean isClusterCoherent() {
        markAccessFlag();
        return false;
    }

    public boolean isNodeCoherent() {
        markAccessFlag();
        return false;
    }

    public boolean put(Element element) throws CacheException {
        markAccessFlag();
        return false;
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        markAccessFlag();
        return null;
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        markAccessFlag();
        return false;
    }

    public Element remove(Object key) {
        markAccessFlag();
        return null;
    }

    public void removeAll() throws CacheException {
        markAccessFlag();

    }

    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        markAccessFlag();
        return null;
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        markAccessFlag();
        return null;
    }

    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        markAccessFlag();
        return false;
    }

    public Element replace(Element element) throws NullPointerException {
        markAccessFlag();
        return null;
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        markAccessFlag();

    }

    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        markAccessFlag();

    }

    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        markAccessFlag();
    }

    public void addStoreListener(StoreListener listener) {
        markAccessFlag();
    }

    public void removeStoreListener(StoreListener listener) {
        markAccessFlag();
    }

    public int getOffHeapSize() {
        markAccessFlag();
        return 0;
    }

    public long getOffHeapSizeInBytes() {
        markAccessFlag();
        return 0;
    }

    public boolean containsKeyOffHeap(Object key) {
        markAccessFlag();
        return false;
    }

    public Object getMBean() {
        return null;
    }

    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        // no-op

    }

    public Results executeQuery(StoreQuery query) {
        throw new UnsupportedOperationException();
    }

    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        return new Attribute(attributeName);
    }

}
