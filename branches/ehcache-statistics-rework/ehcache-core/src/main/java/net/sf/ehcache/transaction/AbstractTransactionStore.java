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
package net.sf.ehcache.transaction;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.AbstractStore;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import org.terracotta.context.annotations.ContextChild;
import net.sf.ehcache.writer.writebehind.WriteBehind;

/**
 * Abstract transactional store which provides implementation of all non-transactional methods
 *
 * @author Ludovic Orban
 */
public abstract class AbstractTransactionStore extends AbstractStore implements TerracottaStore {

    /**
     * The underlying store wrapped by this store
     */
    @ContextChild protected final Store underlyingStore;

    /**
     * The copy strategy for this store
     */
    protected final ReadWriteCopyStrategy<Element> copyStrategy;

    /**
     * Constructor
     * @param underlyingStore the underlying store
     */
    protected AbstractTransactionStore(Store underlyingStore, ReadWriteCopyStrategy<Element> copyStrategy) {
        this.underlyingStore = underlyingStore;
        this.copyStrategy = copyStrategy;
    }

    /**
     * Copy element for read operation
     *
     * @param element
     * @return copied element
     */
    protected Element copyElementForRead(Element element) {
        return copyStrategy.copyForRead(element);
    }

    /**
     * Copy element for write operation
     *
     * @param element
     * @return copied element
     */
    protected Element copyElementForWrite(Element element) {
        return copyStrategy.copyForWrite(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results executeQuery(StoreQuery query) {
        Results results = underlyingStore.executeQuery(query);
        if (results instanceof TxSearchResults) {
            // don't re-wrap needlessly
            return results;
        }

        return new TxSearchResults(results);
    }

    /* non-transactional methods */

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return underlyingStore.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return underlyingStore.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return underlyingStore.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return underlyingStore.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return underlyingStore.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return underlyingStore.getOnDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return underlyingStore.containsKeyOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return underlyingStore.containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return underlyingStore.containsKeyInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        underlyingStore.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return underlyingStore.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        underlyingStore.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        underlyingStore.flush();
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return underlyingStore.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return underlyingStore.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        underlyingStore.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return underlyingStore.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return underlyingStore.getMBean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeCoherent(boolean coherent) {
        if (!coherent) {
            throw new InvalidConfigurationException("a transactional cache cannot be incoherent");
        }
        underlyingStore.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isNodeCoherent()
     */
    @Override
    public boolean isNodeCoherent() {
        return underlyingStore.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isCacheCoherent()
     */
    @Override
    public boolean isCacheCoherent() {
        return underlyingStore.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isClusterCoherent()
     */
    @Override
    public boolean isClusterCoherent() {
        return underlyingStore.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     * @throws InterruptedException
     * @throws UnsupportedOperationException
     * @throws TerracottaNotRunningException
     */
    @Override
    public void waitUntilClusterCoherent() throws TerracottaNotRunningException, UnsupportedOperationException, InterruptedException {
        underlyingStore.waitUntilClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        underlyingStore.setAttributeExtractors(extractors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Attribute<T> getSearchAttribute(String attributeName) throws CacheException {
        return underlyingStore.getSearchAttribute(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAbortedSizeOf() {
        return underlyingStore.hasAbortedSizeOf();
    }

    /* TerracottaStore methods */

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore) underlyingStore).unsafeGet(key);
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore) underlyingStore).getLocalKeys();
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    /**
     * {@inheritDoc}
     */
    public CacheConfiguration.TransactionalMode getTransactionalMode() {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore) underlyingStore).getTransactionalMode();
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    /**
     * Wrap search results so that Result.getValue() can use copy strategy
     *
     * @author teck
     */
    private class TxSearchResults implements Results {

        private final Results results;

        TxSearchResults(Results results) {
            this.results = results;
        }

        public void discard() {
            results.discard();
        }

        public List<Result> all() throws SearchException {
            return new TxResultsList(results.all());
        }

        public List<Result> range(int start, int count) throws SearchException, IndexOutOfBoundsException {
            return new TxResultsList(results.range(start, count));
        }

        public int size() {
            return results.size();
        }

        public boolean hasKeys() {
            return results.hasKeys();
        }

        public boolean hasValues() {
            return results.hasValues();
        }

        public boolean hasAttributes() {
            return results.hasAttributes();
        }

        public boolean hasAggregators() {
            return results.hasAggregators();
        }
    }

    /**
     * Wrap search results so that Result.getValue() can use copy strategy
     *
     * @author teck
     */
    private class TxResultsList implements List<Result> {

        private final List<Result> results;

        TxResultsList(List<Result> results) {
            this.results = results;
        }

        public int size() {
            return results.size();
        }

        public boolean isEmpty() {
            return results.isEmpty();
        }

        public boolean contains(Object o) {
            return results.contains(unwrapIfNeeded(o));
        }

        public Iterator<Result> iterator() {
            return new TxResultsIterator(results.iterator());
        }

        public Object[] toArray() {
            return wrapResultArray(results.toArray());
        }

        public <T> T[] toArray(T[] a) {
            return wrapResultArray(results.toArray(a));
        }

        private <T> T[] wrapResultArray(T[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = (T) new TxResult((Result) array[i]);
            }
            return array;
        }

        public boolean add(Result o) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!contains(o)) {
                    return false;
                }
            }
            return true;
        }

        public boolean addAll(Collection<? extends Result> c) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(int index, Collection<? extends Result> c) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o instanceof List) {
                List other = (List) o;
                if (size() != other.size()) {
                    return false;
                }

                Iterator thisIter = results.iterator();
                Iterator otherIter = other.iterator();
                while (thisIter.hasNext()) {
                    Object otherItem = unwrapIfNeeded(otherIter.next());
                    Object thisItem = thisIter.next();
                    if (otherItem == null && thisItem == null) {
                        continue;
                    }
                    if (otherItem != null && thisItem == null) {
                        return false;
                    }
                    if (thisItem != null && otherItem == null) {
                        return false;
                    }

                    if (!thisItem.equals(otherItem)) {
                        return false;
                    }
                }

                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return results.hashCode();
        }

        public Result get(int index) {
            return new TxResult(results.get(index));
        }

        public Result set(int index, Result element) {
            throw new UnsupportedOperationException();
        }

        public void add(int index, Result element) {
            throw new UnsupportedOperationException();
        }

        public Result remove(int index) {
            throw new UnsupportedOperationException();
        }

        public int indexOf(Object o) {
            return results.indexOf(unwrapIfNeeded(o));
        }

        public int lastIndexOf(Object o) {
            return results.lastIndexOf(unwrapIfNeeded(o));
        }

        public ListIterator<Result> listIterator() {
            return new TxResultsListIterator(results.listIterator());
        }

        public ListIterator<Result> listIterator(int index) {
            return new TxResultsListIterator(results.listIterator(index));
        }

        public List<Result> subList(int fromIndex, int toIndex) {
            return new TxResultsList(results.subList(fromIndex, toIndex));
        }

        private Object unwrapIfNeeded(Object o) {
            if (o instanceof TxResult) {
                return ((TxResult) o).getUnderylingResult();
            }
            return o;
        }

    }

    /**
     * Wrap search results so that Result.getValue() can use copy strategy
     *
     * @author teck
     */
    private class TxResult implements Result {
        private final Result result;

        TxResult(Result result) {
            this.result = result;
        }

        Result getUnderylingResult() {
            return result;
        }

        public Object getKey() throws SearchException {
            return result.getKey();
        }

        public Object getValue() throws SearchException {
            return copyElementForRead(new Element(result.getKey(), result.getValue())).getObjectValue();
        }

        public <T> T getAttribute(Attribute<T> attribute) throws SearchException {
            return result.getAttribute(attribute);
        }

        public List<Object> getAggregatorResults() throws SearchException {
            return result.getAggregatorResults();
        }
    }

    /**
     * Wrap search results so that Result.getValue() can use copy strategy
     *
     * @author teck
     */
    private class TxResultsIterator implements Iterator<Result> {

        private final Iterator<Result> iterator;

        TxResultsIterator(Iterator<Result> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Result next() {
            return new TxResult(iterator.next());
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Wrap search results so that Result.getValue() can use copy strategy
     *
     * @author teck
     */
    private class TxResultsListIterator implements ListIterator<Result> {

        private final ListIterator<Result> listIterator;

        TxResultsListIterator(ListIterator<Result> listIterator) {
            this.listIterator = listIterator;
        }

        public boolean hasNext() {
            return listIterator.hasNext();
        }

        public Result next() {
            return new TxResult(listIterator.next());
        }

        public boolean hasPrevious() {
            return listIterator.hasPrevious();
        }

        public Result previous() {
            return new TxResult(listIterator.previous());
        }

        public int nextIndex() {
            return listIterator.nextIndex();
        }

        public int previousIndex() {
            return listIterator.previousIndex();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(Result o) {
            throw new UnsupportedOperationException();
        }

        public void add(Result o) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public WriteBehind createWriteBehind() {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore)underlyingStore).createWriteBehind();
        }
        throw new UnsupportedOperationException();
    }
}
