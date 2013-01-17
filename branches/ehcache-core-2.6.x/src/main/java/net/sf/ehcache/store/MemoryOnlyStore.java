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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.aggregator.AggregatorInstance;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.impl.AggregateOnlyResult;
import net.sf.ehcache.search.impl.BaseResult;
import net.sf.ehcache.search.impl.GroupedResultImpl;
import net.sf.ehcache.search.impl.OrderComparator;
import net.sf.ehcache.search.impl.ResultImpl;
import net.sf.ehcache.search.impl.ResultsImpl;
import net.sf.ehcache.search.impl.SearchManager;
import net.sf.ehcache.transaction.SoftLockID;

import static net.sf.ehcache.search.expression.BaseCriteria.getExtractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * A memory-only store with support for all caching features.
 *
 * @author Ludovic Orban
 */
public class MemoryOnlyStore extends FrontEndCacheTier<NullStore, MemoryStore> {

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * Create a MemoryOnlyStore
     *
     * @param cacheConfiguration the cache configuration
     * @param authority the memory store
     */
    protected MemoryOnlyStore(CacheConfiguration cacheConfiguration, MemoryStore authority, SearchManager searchManager) {
        super(NullStore.create(), authority, cacheConfiguration.getCopyStrategy(), searchManager,
              cacheConfiguration.isCopyOnWrite(), cacheConfiguration.isCopyOnRead());
    }

    /**
     * Create an instance of MemoryOnlyStore
     * @param cache the cache
     * @param onHeapPool the on heap pool
     * @return an instance of MemoryOnlyStore
     */
    public static Store create(Ehcache cache, Pool onHeapPool) {
        final MemoryStore memoryStore = NotifyingMemoryStore.create(cache, onHeapPool);
        final BruteForceSearchManager searchManager = new BruteForceSearchManager();

        MemoryOnlyStore memoryOnlyStore = new MemoryOnlyStore(cache.getCacheConfiguration(), memoryStore, searchManager);
        searchManager.setMemoryStore(memoryOnlyStore);
        return memoryOnlyStore;
    }

    /**
     * Get the underyling memory store element set
     *
     * @return element set
     */
    Collection<Element> elementSet() {
        return authority.elementSet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element get(Object key) {
        if (key == null) {
            return null;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
        try {
            return copyElementForReadIfNeeded(authority.get(key));
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getQuiet(Object key) {
        if (key == null) {
            return null;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
        try {
            return copyElementForReadIfNeeded(authority.getQuiet(key));
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInMemoryEvictionPolicy(final Policy policy) {
        authority.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Policy getInMemoryEvictionPolicy() {
        return authority.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }


    /**
     * Brute force search implementation
     *
     * @author teck
     */
    protected static class BruteForceSearchManager implements SearchManager {

        private volatile MemoryOnlyStore memoryStore;

        /**
         * Create a BruteForceSearchManager
         */
        public BruteForceSearchManager() {
            //
        }

        /**
         * set the memory store
         *
         * @param memoryStore
         */
        public void setMemoryStore(MemoryOnlyStore memoryStore) {
            this.memoryStore = memoryStore;
        }

        @Override
        public void put(String cacheName, int segmentId, Element element, Map<String, AttributeExtractor> extractors,
                DynamicAttributesExtractor dynamicIndexer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Results executeQuery(String cacheName, StoreQuery query, Map<String, AttributeExtractor> extractors) {
            Criteria c = query.getCriteria();

            List<AggregatorInstance<?>> aggregators = query.getAggregatorInstances();

            final Set<Attribute<?>> groupByAttributes = query.groupByAttributes();
            final boolean isGroupBy = !groupByAttributes.isEmpty();
            boolean includeResults = query.requestsKeys() || query.requestsValues() || !query.requestedAttributes().isEmpty() || isGroupBy;

            boolean hasOrder = !query.getOrdering().isEmpty();

            final Map<Set<?>, BaseResult> groupByResults = new HashMap<Set<?>, BaseResult>();
            final Map<Set, List<AggregatorInstance<?>>> groupByAggregators = new HashMap<Set, List<AggregatorInstance<?>>>();
            final int maxResults = query.maxResults();

            Collection<Element> matches = new LinkedList<Element>();

            for (Element element : memoryStore.elementSet()) {
                element = memoryStore.copyElementForReadIfNeeded(element);

                if (element.getObjectValue() instanceof SoftLockID) {
                    continue;
                }

                if (c.execute(element, extractors)) {
                    if (!isGroupBy && !hasOrder && query.maxResults() >= 0 && matches.size() == query.maxResults()) {
                        break;
                    }

                    matches.add(element);
                }
            }

            Collection<BaseResult> results = isGroupBy ? groupByResults.values() : new ArrayList<BaseResult>();

            boolean anyMatches = !matches.isEmpty();
            for (Element element : matches) {
                if (includeResults) {
                    final Map<String, Object> attributes = getAttributeValues(query.requestedAttributes(), extractors, element);
                    final Object[] sortAttributes = getSortAttributes(query, extractors, element);

                    if (!isGroupBy) {
                        results.add(new ResultImpl(element.getObjectKey(), element.getObjectValue(), query, attributes, sortAttributes));
                    } else {
                        Map<String, Object> groupByValues = getAttributeValues(groupByAttributes, extractors, element);
                        Set<?> groupId = new HashSet(groupByValues.values());
                        BaseResult group = groupByResults.get(groupId);
                        if (group == null) {
                            group = new GroupedResultImpl(query, attributes, sortAttributes, Collections.EMPTY_LIST /* placeholder for now */,
                                    groupByValues);
                            groupByResults.put(groupId, group);
                        }
                        List<AggregatorInstance<?>> groupAggrs = groupByAggregators.get(groupId);
                        if (groupAggrs == null) {
                            groupAggrs = new ArrayList<AggregatorInstance<?>>(aggregators.size());
                            for (AggregatorInstance<?> aggr : aggregators) {
                                groupAggrs.add(aggr.createClone());
                            }
                            groupByAggregators.put(groupId, groupAggrs);
                        }
                        // Switch to per-record aggregators
                        aggregators = groupAggrs;
                    }
                }

                aggregate(aggregators, extractors, element);

            }

            if (hasOrder || isGroupBy) {
                if (isGroupBy) {
                    results = new ArrayList<BaseResult>(results);
                }

                if (hasOrder) {
                    Collections.sort((List<BaseResult>)results, new OrderComparator(query.getOrdering()));
                }
                // trim results to max length if necessary
                int max = query.maxResults();
                if (max >= 0 && (results.size() > max)) {
                    results = ((List<BaseResult>)results).subList(0, max);
                }
            }

            if (!aggregators.isEmpty()) {
                for (BaseResult result : results) {
                    if (isGroupBy) {
                        GroupedResultImpl group = (GroupedResultImpl)result;
                        Set<?> groupId = new HashSet(group.getGroupByValues().values());
                        aggregators = groupByAggregators.get(groupId);
                    }
                    setResultAggregators(aggregators, result);
                }
            }

            if (!isGroupBy && anyMatches && !includeResults && !aggregators.isEmpty()) {
                // add one row in the results if the only thing included was aggregators and anything matched
                BaseResult aggOnly = new AggregateOnlyResult(query);
                setResultAggregators(aggregators, aggOnly);
                results.add(aggOnly);
            }

            return new ResultsImpl((List)results, query.requestsKeys(), query.requestsValues(), !query.requestedAttributes().isEmpty(), anyMatches
                    && !aggregators.isEmpty());
        }

        private void setResultAggregators(List<AggregatorInstance<?>> aggregators, BaseResult result)
        {
            List<Object> aggregateResults = new ArrayList<Object>();
            for (AggregatorInstance<?> aggregator : aggregators) {
                aggregateResults.add(aggregator.aggregateResult());
            }

            if (!aggregateResults.isEmpty()) {
                result.setAggregateResults(aggregateResults);
            }
        }

        private Map<String, Object> getAttributeValues(Set<Attribute<?>> attributes, Map<String, AttributeExtractor> extractors, Element element) {
            final Map<String, Object> values;
            if (attributes.isEmpty()) {
                values = Collections.emptyMap();
            } else {
                values = new HashMap<String, Object>();
                for (Attribute attribute : attributes) {
                    String name = attribute.getAttributeName();
                    values.put(name, getExtractor(name, extractors).attributeFor(element, name));
                }
            }
            return values;
        }

        private void aggregate(List<AggregatorInstance<?>> aggregators, Map<String, AttributeExtractor> extractors, Element element) {
            for (AggregatorInstance<?> aggregator : aggregators) {
                Attribute<?> attribute = aggregator.getAttribute();
                if (attribute == null) {
                    aggregator.accept(null);
                } else {
                    Object val = getExtractor(attribute.getAttributeName(), extractors).attributeFor(element, attribute.getAttributeName());
                    aggregator.accept(val);
                }
            }
        }

        private Object[] getSortAttributes(StoreQuery query, Map<String, AttributeExtractor> extractors, Element element) {
            Object[] sortAttributes;
            List<StoreQuery.Ordering> orderings = query.getOrdering();
            if (orderings.isEmpty()) {
                sortAttributes = EMPTY_OBJECT_ARRAY;
            } else {
                sortAttributes = new Object[orderings.size()];
                for (int i = 0; i < sortAttributes.length; i++) {
                    String name = orderings.get(i).getAttribute().getAttributeName();
                    sortAttributes[i] = getExtractor(name, extractors).attributeFor(element, name);
                }
            }

            return sortAttributes;
        }

        @Override
        public void remove(String cacheName, Object uniqueKey, int segmentId, boolean isRemoval) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear(String cacheName, int segmentId) {
            throw new UnsupportedOperationException();
        }

    }

}
