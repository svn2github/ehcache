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

package net.sf.ehcache.store.compound.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.aggregator.AggregatorInstance;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeType;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.store.ElementAttributeValues;
import net.sf.ehcache.store.FifoPolicy;
import net.sf.ehcache.store.LfuPolicy;
import net.sf.ehcache.store.LruPolicy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.StoreQuery.Ordering;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.factories.CapacityLimitedInMemoryFactory;

/**
 * Implements a memory only store.
 *
 * @author Chris Dennis
 */
public final class MemoryOnlyStore extends CompoundStore implements CacheConfigurationListener {

    private final Map<String, AttributeExtractor> attributeExtractors = new ConcurrentHashMap<String, AttributeExtractor>();

    private final CapacityLimitedInMemoryFactory memoryFactory;

    private final CacheConfiguration config;

    private MemoryOnlyStore(CapacityLimitedInMemoryFactory memory, CacheConfiguration config) {
        super(memory, config.isCopyOnRead(), config.isCopyOnWrite(), config.getCopyStrategy());
        this.memoryFactory = memory;
        this.config = config;
    }

    /**
     * Constructs an in-memory store for the given cache, using the given disk path.
     *
     * @param cache cache that fronts this store
     * @param diskStorePath disk path to store data in
     * @return a fully initialized store
     */
    public static MemoryOnlyStore create(Cache cache, String diskStorePath) {
        CacheConfiguration config = cache.getCacheConfiguration();
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(null, config.getMaxElementsInMemory(),
                determineEvictionPolicy(config), cache.getCacheEventNotificationService());
        MemoryOnlyStore store = new MemoryOnlyStore(memory, config);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    /**
     * Chooses the Policy from the cache configuration
     */
    private static final Policy determineEvictionPolicy(CacheConfiguration config) {
        MemoryStoreEvictionPolicy policySelection = config.getMemoryStoreEvictionPolicy();

        if (policySelection.equals(MemoryStoreEvictionPolicy.LRU)) {
            return new LruPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.FIFO)) {
            return new FifoPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.LFU)) {
            return new LfuPolicy();
        }

        throw new IllegalArgumentException(policySelection + " isn't a valid eviction policy");
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
    public boolean containsKeyInMemory(Object key) {
        return containsKey(key);
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
    public boolean containsKeyOnDisk(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        memoryFactory.expireElements();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This store is not persistent, so this simply clears the in-memory store if clear-on-flush is set for this cache.
     */
    public void flush() throws IOException {
        if (config.isClearOnFlush()) {
            removeAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return memoryFactory.getEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return memoryFactory.getSizeInBytes();
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
    public long getOnDiskSizeInBytes() {
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
    public long getOffHeapSizeInBytes() {
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
    public void setInMemoryEvictionPolicy(Policy policy) {
        memoryFactory.setEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A NO-OP
     */
    public void deregistered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A NO-OP
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A NO-OP
     */
    public void loggingChanged(boolean oldValue, boolean newValue) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        memoryFactory.setCapacity(newCapacity);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A NO-OP
     */
    public void registered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A NO-OP
     */
    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
        // no-op
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A NO-OP
     */
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        // no-op
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
    @Override
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        this.attributeExtractors.putAll(extractors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results executeQuery(StoreQuery query) {
        Criteria c = query.getCriteria();

        List<AggregatorInstance<?>> aggregators = query.getAggregatorInstances();


        boolean includeResults = query.requestsKeys() || !query.requestedAttributes().isEmpty();

        ArrayList<Result> results = new ArrayList<Result>();

        boolean hasOrder = !query.getOrdering().isEmpty();

        for (Element element : elementSet()) {
            if (!hasOrder && query.maxResults() >= 0 && results.size() == query.maxResults()) {
                break;
            }

            ElementAttributeValues elementAttributeValues = new ElementAttributeValuesImpl(element, attributeExtractors);

            boolean match = c.execute(element, elementAttributeValues);

            if (match) {
                if (includeResults) {
                    results.add(new ResultImpl(element, query, elementAttributeValues));
                }

                for (AggregatorInstance<?> aggregator : aggregators) {
                    Attribute<?> attribute = aggregator.getAttribute();
                    if (attribute == null) {
                        aggregator.accept(null);
                    } else {
                        Object val = elementAttributeValues.getAttributeValue(attribute.getAttributeName());
                        aggregator.accept(val);
                    }
                }
            }
        }

        if (hasOrder) {
            Collections.sort(results, new OrderComparator(query.getOrdering()));

            // trim results to max length if necessary
            int max = query.maxResults();
            if (max >= 0 && (results.size() > max)) {
                int trim = results.size() - max;
                for (int i = 0; i < trim; i++) {
                    results.remove(results.size() - 1);
                }
                results.trimToSize();
            }
        }

        return new ResultsImpl(results, query.requestsKeys(), aggregators, !query.requestedAttributes().isEmpty());
    }

    /**
     * Implementation for {@link ElementAttributeValues}. Caches repeated reads and type lookups
     */
    private static class ElementAttributeValuesImpl implements ElementAttributeValues {

        private static final Object NULL = new Object();

        private final Map<String, TypedValue> cache = new HashMap<String, TypedValue>();
        private final Element element;
        private final Map<String, AttributeExtractor> attributeExtractors;

        public ElementAttributeValuesImpl(Element element, Map<String, AttributeExtractor> attributeExtractors) {
            this.element = element;
            this.attributeExtractors = attributeExtractors;
        }

        /**
         * {@inheritDoc}
         */
        public Object getAttributeValue(String attributeName) throws SearchException {
            return getAttributeValue(attributeName, null, false);
        }

        /**
         * {@inheritDoc}
         */
        public Object getAttributeValue(String attributeName, AttributeType expectedType) throws SearchException {
            return getAttributeValue(attributeName, expectedType, true);
        }

        private Object getAttributeValue(String attributeName, AttributeType expectedType, boolean checkType) throws SearchException {
            TypedValue cachedValue = cache.get(attributeName);
            if (cachedValue != null) {
                if (checkType) {
                    return cachedValue.getValue(expectedType);
                } else {
                    return cachedValue.getValue();
                }
            }

            AttributeExtractor extractor = attributeExtractors.get(attributeName);
            if (extractor == null) {
                throw new SearchException("No such search attribute named [" + attributeName + "]");
            }

            Object value = extractor.attributeFor(element);

            if (value == null) {
                cache.put(attributeName, new TypedValue(attributeName, NULL, null));
            } else {
                AttributeType actualType = AttributeType.typeFor(attributeName, value);

                if (checkType) {
                    if (actualType != expectedType) {
                        throw new SearchException("Expecting attribute of type " + expectedType.name() + " but was " + actualType.name());
                    }
                }

                cache.put(attributeName, new TypedValue(attributeName, value, actualType));
            }
            return value;
        }

        /**
         * A cached attribute value and type lookup
         */
        private static class TypedValue {
            private final AttributeType type;
            private final Object value;
            private final String attributeName;

            TypedValue(String attributeName, Object value, AttributeType type) {
                this.attributeName = attributeName;
                this.value = value;
                this.type = type;
            }

            public Object getValue() {
                if (value == NULL) {
                    return null;
                }

                return value;
            }

            public Object getValue(AttributeType expectedType) {
                if (value == NULL) {
                    return null;
                }

                if (type != expectedType) {
                    throw new SearchException("Expecting value of type (" + expectedType + ") for attribute [" + attributeName
                            + "] but was (" + type + ")");
                }

                return value;
            }
        }

    }

    /**
     * Result implementation
     */
    private static class ResultImpl implements Result {

        private final Object key;
        private final StoreQuery query;
        private final Map<String, Object> attributes;
        private final Object[] sortAttributes;

        ResultImpl(Element element, StoreQuery query, ElementAttributeValues elementAttributeValues) {
            this.query = query;
            this.key = element.getObjectKey();

            if (query.requestedAttributes().isEmpty()) {
                attributes = Collections.EMPTY_MAP;
            } else {
                attributes = new HashMap<String, Object>();
                for (Attribute attribute : query.requestedAttributes()) {
                    String name = attribute.getAttributeName();
                    attributes.put(name, elementAttributeValues.getAttributeValue(name));
                }
            }

            List<Ordering> orderings = query.getOrdering();
            if (orderings.isEmpty()) {
                sortAttributes = null;
            } else {
                sortAttributes = new Object[orderings.size()];
                for (int i = 0; i < sortAttributes.length; i++) {
                    String name = orderings.get(i).getAttribute().getAttributeName();
                    sortAttributes[i] = elementAttributeValues.getAttributeValue(name);
                }
            }
        }

        Object getSortAttribute(int pos) {
            return sortAttributes[pos];
        }

        /**
         * @{inheritDoc
         */
        public Object getKey() {
            if (query.requestsKeys()) {
                return key;
            }

            throw new SearchException("keys not included in query. Use includeKeys() to add keys to results.");
        }

        /**
         * @{inheritDoc
         */
        public <T> T getAttribute(Attribute<T> attribute) {
            String name = attribute.getAttributeName();
            Object value = attributes.get(name);
            if (value == null) {
                throw new SearchException("Attribute [" + name + "] not included in query");
            }
            return (T) value;
        }

        @Override
        public String toString() {
            return "ResultImpl [attributes=" + attributes + ", key=" + key + ", query=" + query + ", sortAttributes="
                    + Arrays.toString(sortAttributes) + "]";
        }

    }

    /**
     * Results implementation
     */
    private static class ResultsImpl implements Results {

        private final List<Result> results;
        private final List<Object> aggregateResults;
        private final boolean hasKeys;
        private final boolean hasAttributes;

        ResultsImpl(List<Result> results, boolean hasKeys, List<AggregatorInstance<?>> aggregators, boolean hasAttributes) {
            this.hasKeys = hasKeys;
            this.hasAttributes = hasAttributes;
            this.results = Collections.unmodifiableList(results);

            if (aggregators.isEmpty()) {
                this.aggregateResults = null;
            } else {
                List<Object> tmp = new ArrayList<Object>();
                for (AggregatorInstance<?> aggregator : aggregators) {
                    tmp.add(aggregator.aggregateResult());
                }
                this.aggregateResults = Collections.unmodifiableList(tmp);
            }
        }

        /**
         * @{inheritDoc
         */
        public void discard() {
            // no-op
        }

        /**
         * @{inheritDoc
         */
        public List<Result> all() throws SearchException {
            return results;
        }

        /**
         * @{inheritDoc
         */
        public List<Result> range(int start, int length) throws SearchException {
            if (start < 0) {
                throw new IllegalArgumentException("start: " + start);
            }

            if (length < 0) {
                throw new IllegalArgumentException("length: " + length);
            }

            int size = results.size();

            if (start > size - 1 || length == 0) {
                return Collections.EMPTY_LIST;
            }

            int end = start + length;

            if (end > size) {
                end = size;
            }

            return results.subList(start, end);
        }

        /**
         * @{inheritDoc
         */
        public int size() {
            return results.size();
        }

        /**
         * @{inheritDoc
         */
        public boolean hasKeys() {
            return hasKeys;
        }

        /**
         * @{inheritDoc
         */
        public boolean hasAttributes() {
            return hasAttributes;
        }

        /**
         * @{inheritDoc
         */
        public boolean hasAggregators() {
            return aggregateResults != null;
        }

        public List<Object> getAggregatorResults() throws SearchException {
            if (!hasAggregators()) {
                throw new SearchException("No aggregators present in query");
            }
            return this.aggregateResults;
        }

    }

    /**
     * A compound comparator to implements query ordering
     */
    private static class OrderComparator implements Comparator<Result> {

        private final List<Comparator<Result>> comparators;

        OrderComparator(List<Ordering> orderings) {
            comparators = new ArrayList<Comparator<Result>>();
            int pos = 0;
            for (Ordering ordering : orderings) {
                switch (ordering.getDirection()) {
                    case ASCENDING: {
                        comparators.add(new AscendingComparator(pos));
                        break;
                    }
                    case DESCENDING: {
                        comparators.add(new DescendingComparator(pos));
                        break;
                    }
                    default: {
                        throw new AssertionError(ordering.getDirection());
                    }
                }

                pos++;
            }
        }

        public int compare(Result o1, Result o2) {
            for (Comparator<Result> c : comparators) {
                int cmp = c.compare(o1, o2);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        }

        /**
         * Simple ascending comparator
         */
        private static class AscendingComparator implements Comparator<Result> {

            private final int pos;

            AscendingComparator(int pos) {
                this.pos = pos;
            }

            public int compare(Result o1, Result o2) {
                Object attr1 = ((ResultImpl) o1).getSortAttribute(pos);
                Object attr2 = ((ResultImpl) o2).getSortAttribute(pos);

                if ((attr1 == null) && (attr2 == null)) {
                    return 0;
                }

                if (attr1 == null) {
                    return -1;
                }

                if (attr2 == null) {
                    return 1;
                }

                return ((Comparable) attr1).compareTo(attr2);
            }
        }

        /**
         * Simple descending comparator
         */
        private static class DescendingComparator implements Comparator<Result> {

            private final int pos;

            DescendingComparator(int pos) {
                this.pos = pos;
            }

            public int compare(Result o1, Result o2) {
                Object attr1 = ((ResultImpl) o1).getSortAttribute(pos);
                Object attr2 = ((ResultImpl) o2).getSortAttribute(pos);

                if ((attr1 == null) && (attr2 == null)) {
                    return 0;
                }

                if (attr1 == null) {
                    return 1;
                }

                if (attr2 == null) {
                    return -1;
                }

                return ((Comparable) attr2).compareTo(attr1);
            }
        }
    }

}
