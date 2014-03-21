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
import net.sf.ehcache.config.ConfigurationHelper;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.aggregator.AggregatorInstance;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeExtractorException;
import net.sf.ehcache.search.attribute.AttributeType;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.impl.AggregateOnlyResult;
import net.sf.ehcache.search.impl.BaseResult;
import net.sf.ehcache.search.impl.DynamicSearchChecker;
import net.sf.ehcache.search.impl.GroupedResultImpl;
import net.sf.ehcache.search.impl.OrderComparator;
import net.sf.ehcache.search.impl.ResultImpl;
import net.sf.ehcache.search.impl.ResultsImpl;
import net.sf.ehcache.search.impl.SearchManager;
import net.sf.ehcache.transaction.SoftLockID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static net.sf.ehcache.search.expression.BaseCriteria.getExtractor;

/**
 * Brute force search implementation
 *
 * @author teck
 */
public class BruteForceSearchManager implements SearchManager {

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * account for all search attributes
     */
    private final Set<Attribute> searchAttributes = new CopyOnWriteArraySet<Attribute>();
    private BruteForceSource bruteForceSource;

    /**
     * Create a BruteForceSearchManager
     */
    public BruteForceSearchManager() {
        //
    }

    /**
     * Concrete search result with relevant inputs to aggregate functions (if any) 
     */
    private static final class ResultHolder implements Comparable<ResultHolder> {
        private final BaseResult result;
        private final List<Object> aggregatorInputs;
        private final OrderComparator<BaseResult> comp;
        
        private ResultHolder(BaseResult res, List<Object> values, OrderComparator<BaseResult> cmp) {
            result = res;
            aggregatorInputs = values;
            comp = cmp;
        }

        @Override
        public int compareTo(ResultHolder other) {
            return comp.compare(this.result, other.result);
        }
    }
    
    @Override
    public Results executeQuery(StoreQuery query, Map<String, AttributeExtractor> extractors, DynamicAttributesExtractor
            dynIndexer) {
        Criteria c = query.getCriteria();

        List<AggregatorInstance<?>> aggregators = query.getAggregatorInstances();

        final Set<Attribute<?>> groupByAttributes = query.groupByAttributes();
        final boolean isGroupBy = !groupByAttributes.isEmpty();
        boolean includeResults = query.requestsKeys() || query.requestsValues() || !query.requestedAttributes().isEmpty() || isGroupBy;

        boolean hasOrder = !query.getOrdering().isEmpty();

        final Map<Set<?>, ResultHolder> groupByResults = new HashMap<Set<?>, ResultHolder>();
        final Map<Set, List<AggregatorInstance<?>>> groupByAggregators = new HashMap<Set, List<AggregatorInstance<?>>>();

        Collection<Element> matches = new LinkedList<Element>();
        Map<Object, Map<String, AttributeExtractor>> eltExtractors = new HashMap<Object, Map<String, AttributeExtractor>>();

        for (Element element : bruteForceSource.elements()) {

            Map<String, AttributeExtractor> extractorSuperset = getCombinedExtractors(extractors, dynIndexer, element);
            eltExtractors.put(element.getObjectKey(), extractorSuperset);

            if (c.execute(element, extractorSuperset)) {
                if (!isGroupBy && !hasOrder && query.maxResults() >= 0 && matches.size() == query.maxResults()) {
                    break;
                }

                matches.add(element);
            }
        }

        Collection<ResultHolder> results = isGroupBy ? groupByResults.values() : new ArrayList<ResultHolder>();

        boolean anyMatches = !matches.isEmpty();
        OrderComparator<BaseResult> comp = new OrderComparator<BaseResult>(query.getOrdering());
        
        for (Element element : matches) {
            Map<String, AttributeExtractor> extractorSuperset = eltExtractors.get(element.getObjectKey());

            List<Object> resultAggs = new ArrayList<Object>(aggregators.size());
            for (AggregatorInstance<?> agg: aggregators) {
                Attribute aggrAttr = agg.getAttribute();
                // placeholder input for count
                Object val = aggrAttr != null ? 
                    getExtractor(aggrAttr.getAttributeName(), extractorSuperset).attributeFor(element, aggrAttr.getAttributeName()) : null;
                resultAggs.add(val);
            }
            
            Map<String, Object> attributes = getAttributeValues(query.requestedAttributes(), extractorSuperset, element);
            Object[] sortAttributes = getSortAttributes(query, extractorSuperset, element);

            if (!isGroupBy) {
                results.add(new ResultHolder(new ResultImpl(element.getObjectKey(), element.getObjectValue(), query, attributes, sortAttributes), 
                            resultAggs, comp));
            } else {
                Map<String, Object> groupByValues = getAttributeValues(groupByAttributes, extractorSuperset, element);
                Set<?> groupId = new HashSet<Object>(groupByValues.values());
                List<AggregatorInstance<?>> groupAggrs = groupByAggregators.get(groupId);
                if (groupAggrs == null) {
                    groupAggrs = new ArrayList<AggregatorInstance<?>>(aggregators.size());
                    for (AggregatorInstance<?> aggr : aggregators) {
                        groupAggrs.add(aggr.createClone());
                    }
                    groupByAggregators.put(groupId, groupAggrs);
                }
                int i = 0;
                for (AggregatorInstance<?> inst: groupAggrs) {
                    inst.accept(resultAggs.get(i++));
                }
                ResultHolder group = groupByResults.get(groupId);
                if (group == null) {
                    group = new ResultHolder(new GroupedResultImpl(query, attributes, sortAttributes, Collections.emptyList(),
                            groupByValues), Collections.emptyList(), comp);
                    groupByResults.put(groupId, group);
                }
            }
        }

        if (hasOrder || isGroupBy) {
            if (isGroupBy) {
                results = new ArrayList<ResultHolder>(results);
            }

            if (hasOrder) {
                Collections.sort((List<ResultHolder>)results);
            }
            // trim results to max length if necessary
            int max = query.maxResults();
            if (max >= 0 && (results.size() > max)) {
                results = ((List<ResultHolder>)results).subList(0, max);
            }
        }

        if (!aggregators.isEmpty()) {
            for (ResultHolder rh : results) {
                if (isGroupBy) {
                    GroupedResultImpl group = (GroupedResultImpl)rh.result;
                    Set<?> groupId = new HashSet<Object>(group.getGroupByValues().values());
                    aggregators = groupByAggregators.get(groupId);
                    setResultAggregators(aggregators, group);
                } else {
                    int i = 0;
                    for (Object val: rh.aggregatorInputs) {
                        aggregators.get(i++).accept(val);
                    }
                }
            }
            if (includeResults && !isGroupBy) {
                // Set the same aggregate values for each result
                for (ResultHolder rh: results) {
                    setResultAggregators(aggregators, rh.result);
                }
            }
        }
        
        List<BaseResult> output;
        
        if (!isGroupBy && anyMatches && !includeResults && !aggregators.isEmpty()) {
            // add one row in the results if the only thing included was aggregators and anything matched
            BaseResult aggOnly = new AggregateOnlyResult(query);
            setResultAggregators(aggregators, aggOnly);
            output = Collections.singletonList(aggOnly);
        } else {
            output = new ArrayList<BaseResult>(results.size());
            for (ResultHolder rh: results) {
                output.add(rh.result);
            }
        }

        return new ResultsImpl(output, query.requestsKeys(), query.requestsValues(), !query.requestedAttributes().isEmpty(), anyMatches
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

    private Map<String, AttributeExtractor> getCombinedExtractors(Map<String, AttributeExtractor> configExtractors, DynamicAttributesExtractor
            dynIndexer, Element element) {
        Map<String, AttributeExtractor> combinedExtractors = new HashMap<String, AttributeExtractor>();
        combinedExtractors.putAll(configExtractors);

        if (dynIndexer != null) {
            Map<String, ?> dynamic = DynamicSearchChecker.getSearchAttributes(element, configExtractors.keySet(),
                    dynIndexer);

            for (final Map.Entry<String, ?> entry: dynamic.entrySet()) {
                AttributeExtractor old = combinedExtractors.put(entry.getKey(), new AttributeExtractor() {
                    @Override
                    public Object attributeFor(Element element, String attributeName) throws AttributeExtractorException {
                        if (!attributeName.equals(entry.getKey())) {
                            throw new AttributeExtractorException(String.format("Expected attribute name %s but got %s", entry.getKey(),
                                    attributeName));
                        }
                        return entry.getValue();
                    }
                });
                if (old != null) {
                    throw new AttributeExtractorException(String.format("Attribute name %s already used by configured extractors",
                            entry.getKey()));
                }
            }
        }
        return combinedExtractors;
    }

    private Object[] getSortAttributes(StoreQuery query, Map<String, AttributeExtractor> extractors, Element element) {
        Object[] sortAttributes;
        List<StoreQuery.Ordering> orderings = query.getOrdering();
        if (orderings.isEmpty()) {
            sortAttributes = BruteForceSearchManager.EMPTY_OBJECT_ARRAY;
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
    public void clear(String cacheName, int segmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(String cacheName, int segmentId, Element element, byte[] key, Map<String, AttributeExtractor> extractors,
            DynamicAttributesExtractor dynamicIndexer) {
        if (extractors.isEmpty() && dynamicIndexer == null) {
            return;
        }

      boolean isXa = element.getObjectValue() instanceof SoftLockID;

      if (isXa) {
        SoftLockID sl = (SoftLockID) element.getObjectValue();
        element = sl.getOldElement();

        // No previous value committed - do not index
        if (element == null) { return; }
      }
      element = bruteForceSource.transformForIndexing(element);

      // Handle dynamic attribute extractor, if any
      Map<String, ?> dynAttrs = DynamicSearchChecker.getSearchAttributes(element, extractors.keySet(),
                                                                   dynamicIndexer);
      Set<Attribute<?>> attrs = new HashSet<Attribute<?>>(dynAttrs.size());
      for (Map.Entry<String, ?> attr : dynAttrs.entrySet()) {
          if (!AttributeType.isSupportedType(attr.getValue())) {
              throw new CacheException(String.format("Unsupported attribute type specified %s for dynamically extracted attribute %s",
                      attr.getClass().getName(), attr.getKey()));
          }
          attrs.add(new Attribute(attr.getKey()));
      }

      Searchable config = bruteForceSource.getSearchable();
      if (config == null) { return; }
      for (Map.Entry<String, AttributeExtractor> entry : extractors.entrySet()) {
        String name = entry.getKey();
        SearchAttribute sa = config.getSearchAttributes().get(name);
        Class<?> c = ConfigurationHelper.getSearchAttributeType(sa);
        if (c == null) { continue; }

        AttributeExtractor extractor = entry.getValue();
        Object av = extractor.attributeFor(element, name);

        AttributeType schemaType = AttributeType.typeFor(c);
        AttributeType type = AttributeType.typeFor(name, av);

        String schemaTypeName = c.isEnum() ? c.getName() : schemaType.name();
        String typeName = AttributeType.ENUM == type ? ((Enum) av).getDeclaringClass().getName() : type.name();

        if (!typeName.equals(schemaTypeName)) { throw new SearchException(
                                                                    String
                                                                        .format("Expecting a %s value for attribute [%s] but was %s",
                                                                                schemaTypeName, name, typeName));
        }
      }

      searchAttributes.addAll(attrs);
    }

    @Override
    public void remove(String cacheName, Object key, int segmentId, boolean isRemoval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Attribute> getSearchAttributes(String cacheName) {
        return searchAttributes;
    }

    /**
     * Sets the BruteForceSource to be used for search
     *
     * @param bruteForceSource the source
     */
    public void setBruteForceSource(BruteForceSource bruteForceSource) {
        this.bruteForceSource = bruteForceSource;
    }

    /**
     * Add search attributes
     *
     * @param attributeSet the search attributes to add
     */
    void addSearchAttributes(Set<Attribute<?>> attributeSet) {
        searchAttributes.addAll(attributeSet);
    }
}
