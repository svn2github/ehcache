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

package net.sf.ehcache.search;

import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.AggregatorException;
import net.sf.ehcache.search.expression.Criteria;

/**
 * Creates queries for performing cache searches.
 * <p/>
 * Queries are created using our search DSL implemented using Java.
 * <p/>
 * A fluent interface provides a compact and yet easy-to-read representation. Fluent interfaces are implemented using method chaining.
 * Static factory methods and imports are a great aid in creating a compact, yet readable DSL.
 * <p/>
 * Out API has the following features:
 * <p/>
 * Method Chaining - we return <code>this</code>.
 * <p/>
 * <p/>
 * See http://www.infoq.com/articles/internal-dsls-java for a description of these conventions.
 * <p/>
 * A query can be executed and then modified and re-executed. If {@link #end} is called the query is made immutable.
 * <p/>
 * Both Element keys and attributes of Element can be queried. Attributes must be pre-defined for a cache. They are populated by extraction
 * from an Element's value using an {@link net.sf.ehcache.search.attribute.AttributeExtractor} .
 * <p/>
 * Search results can either be Element keys (the default), values, or the result of an {@link Aggregator} function.
 * <p/>
 * A {@link Query} instance can be used by multiple threads
 *
 * @author teck
 * @author Greg Luck
 */
public interface Query {

    /**
     * The search attribute for a cache element's key.
     *
     * This will exist as a search attribute at runtime if the key is of a supported {@link net.sf.ehcache.search.attribute.AttributeType}
     */
    public static final Attribute KEY = new Attribute("key");

    /**
     * The search attribute for a cache element's value.
     *
     * This will exist as a search attribute at runtime if the value is of a supported {@link net.sf.ehcache.search.attribute.AttributeType}
     */
    public static final Attribute VALUE = new Attribute("value");

    /**
     * Request that the key object be present in the results.
     *
     * @return this
     */
    public Query includeKeys();

    /**
     * Request that the value object be present in the results.
     *
     * @return this
     */
    public Query includeValues();

    /**
     * Request that the given attribute(s) should be present in the result for
     * this query. This call can be made multiple times to add to the set of
     * selected attributes.
     * <p/>
     * Note that in a distributed cache attributes may need to come over the network. To prevent very large network transfers, consider
     * limiting the results size with {@link #maxResults(int)} or by using {@link Results#range} rathern than {@link Results#all()}
     *
     * @param attributes the query attributes to select
     * @return this
     */
    public Query includeAttribute(Attribute<?>... attributes);

    /**
     * Request this query to aggregate the results by the given Aggregator(s)
     * <p/>
     * This method may be called multiple times to request multiple aggregations
     *
     * @param aggregators
     * @return this
     * @throws SearchException
     * @throws net.sf.ehcache.search.aggregator.AggregatorException
     */
    public Query includeAggregator(Aggregator... aggregators) throws SearchException, AggregatorException;

    /**
     * Request result set ordering by the given attribute and direction. This
     * call can be made multiple times to specify second level. third level, etc
     * orderings
     *
     * @param attribute The attribute to order the results by
     * @param direction Ascending or descending
     * @return this
     */
    public Query addOrderBy(Attribute<?> attribute, Direction direction);

    /**
     * Group result set by unique value(s) of specified attribute(s).
     * Rows with duplicate values for these attributes will be removed. This method may also be chained to achieve the same effect.
     * @param attribute
     * @return
     * @since 2.6
     */
    public Query addGroupBy(Attribute<?>... attribute);

    /**
     * Restrict the number of results returned from the search.
     * <p/>
     * By default an unlimited number of results can be returned. This could cause an OutOfMemoryError to be thrown. It is therefore
     * recommended to add an <code>maxResults</code> clause to your query to limit the size.
     * <p/>
     * Negative values are ignored and result in the default behaviour: unlimited number of results.
     *
     * @param maxResults the maximum number of results to return
     * @return this
     */
    public Query maxResults(int maxResults);

    /**
     * Adds a criteria to the query
     */
    public Query addCriteria(Criteria criteria);

    /**
     * Execute this query. Every call to this method will re-execute the query
     * and return a distinct results object.
     * <p/>
     * An empty results object will be returned (on timeout) for non-stop enabled caches with {@link net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType#NOOP} and
     * {@link net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType#LOCAL_READS} behavior
     *
     * @return query results
     * @throws SearchException
     */
    public Results execute() throws SearchException;

    /**
     * Optional method for terminating query creation. If called the query becomes
     * immutable, so that attempting any further mutations will result in an exception
     */
    public Query end();

}
