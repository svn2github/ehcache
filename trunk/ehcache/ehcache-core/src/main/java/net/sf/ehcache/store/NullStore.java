/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.writer.CacheWriterManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * A store implementation which does not store anything.
 *
 * @author Ludovic Orban
 */
public final class NullStore extends AbstractStore implements TierableStore {

    private NullStore() {
    }

    /**
     * Create a new NullStore instance.
     * @return a NullStore instance
     */
    public static NullStore create() {
        return new NullStore();
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void removeNoReturn(Object key) {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTierPinned() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPersistent() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void fill(Element e) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeIfNotPinned(final Object key) {
        return remove(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    public void recalculateSize(Object key) {
        // no-op
    }
}
