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

package net.sf.ehcache.search;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.AggregatorException;
import net.sf.ehcache.search.expression.Criteria;

/**
 * Creates queries for performing cache searches.
 * 
 * Queries are created using a fluent builder style.
 * 
 * A query can be executed and then modified and re-executed. If {@link #end} is called
 * the query is made immutable.
 * 
 * Both Element keys and attributes of Element can be queried. Attributes must be pre-defined
 * for a cache. They are populated by extraction from an Element's value using an {@link net.sf.ehcache.search.attribute.AttributeExtractor}
 * .
 * 
 * Search results can either be Element keys (the default), values, or the result of an {@link Aggregator} function.
 * 
 * A {@link Query} instance can
 * be used by multiple threads
 * 
 * @author teck
 */
public class Query {

    private volatile boolean frozen;

    /**
     * Construct a new/empty builder for the given Cache.
     * Only one cache can be searched. Only what is present in a cache is searched.
     * CacheLoaders will not be consulted.
     * 
     * In indexed implementations, the indexes are continuously updated as cache
     * operations occur, so that the
     * 
     * @param cache
     *            the {@link Cache} instance that will be queried
     */
    public Query(Cache cache) {
        //
    }

    /**
     * Request that the key object be present in the results. A query that only
     * selects attributes need not select keys.
     * 
     * @return this
     */
    public Query includeKeys() {
        return this;
    }

    /**
     * Hint that cache values will be accessed in the result set (distributed
     * caches might use this hint to pre-emptively fetch data)
     * 
     * @return this
     */
    public Query includeValues() {
        return this;
    }

    /**
     * Request that the given attribute(s) should be present in the result for
     * this query. This call can be made multiple times to add to the set of
     * selected attributes
     * 
     * @param attribute
     *            The query attribute to select @return this
     */
    public Query includeAttribute(Attribute<?>... attribute) {
        return this;
    }

    /**
     * Request this query to aggregate the results by the given Aggregator
     * 
     * This method can only be called once.
     * 
     * If an aggregator is specified, then neither {@link #includeKeys()} or {@link #includeValues()} can
     * be used.
     * 
     * Ehcache standalone supports user-defined aggregators. Terracotta clustered, which executes on the
     * Terracotta server only supports predefined aggregators in the {@link net.sf.ehcache.search.aggregator} package.
     * 
     * @throws SearchException
     *             if more than one column of results was specified.
     * @throws AggregatorException
     *             if the result type is not supported by the aggregator
     * @return this
     */
    public Query includeAggregator(Aggregator aggregator, Attribute<?> attribute) throws SearchException, AggregatorException {
        // we should check the aggregator for attributes, keys and values
        if (!aggregator.supports(attribute.getClass())) {
            throw new AggregatorException("Attributes of type " + attribute.getClass().getName() + " is not supported");
        }
        return this;
    }

    /**
     * Request result set ordering by the given attribute and direction. This
     * call can be made multiple times to specify second level. third level, etc
     * orderings
     * 
     * @param attribute
     *            The attribute to order the results by
     * @param direction
     *            Ascending or descending
     * @return this
     */
    public Query addOrder(Attribute<?> attribute, Direction direction) {
        return this;
    }

    /**
     * Restrict the number of results returned from the search.
     * 
     * @param maxResults
     *            the maximum number of results to return
     * @return this
     */
    public Query maxResults(int maxResults) {
        return this;
    }

    /**
     * Execute this query. Every call to this method will re-execute the query
     * and return a distinct results object
     * 
     * @return query results
     * @throws SearchException
     */
    public Results execute() throws SearchException {
        return (Results) new Object();
    }

    /**
     * Adds a criteria to the query
     */
    public Query add(Criteria criteria) {
        if (frozen) {
            throw new SearchException("The Query cannot be modified.");
        }
        //
        return this;
    }

    /**
     * Optional method for terminating query creation. If called the query becomes
     * immutable, so that calling any other fluent
     */
    public Query end() {
        frozen = true;
        return this;
    }

}
