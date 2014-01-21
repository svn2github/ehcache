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

import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.ExecutionHints;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.AggregatorInstance;
import net.sf.ehcache.search.expression.Criteria;

/**
 * An immutable query that a {@link Store} can execute
 *
 * @author teck
 */
public interface StoreQuery {

    /**
     * Get the search criteria
     *
     * @return the search criteria
     */
    Criteria getCriteria();

    /**
     * Are keys requested?
     *
     * @return true if keys requested
     */
    boolean requestsKeys();

    /**
     * Are values requested?
     *
     * @return true if values requested
     */
    boolean requestsValues();

    /**
     * Get the cache this query originated from
     *
     * @return cache
     */
    Cache getCache();

    /**
     * This needs to be cleaned up. We don't want to introduce any kind of ordering here, as there simply is none.
     * <p>
     * Meanwhile, this is used to keep the ordering on BMQL created Queries. Yet, {@link net.sf.ehcache.search.query.QueryManagerBuilder
     * QueryManagerBuilder}, which creates the {@link net.sf.ehcache.search.query.QueryManager QueryManager}, doesn't let us qualify the
     * {@link net.sf.ehcache.search.Query Query} type further to add this "ordering" concept of "targets" to it.<br />
     * And this "ordering" shouldn't become part of the QueryManager API neither as each implementation might require something different.
     *
     * @return select target names, searchAttribute or aggregator
     * @see <a href="https://jira.terracotta.org/jira/browse/API-43">API-43</a>
     * @deprecated
     */
    @Deprecated
    String[] getTargets();

    /**
     * Set the names of the select targets. These will either be searchAttribute names or
     * aggregator display names, eg. 'ave(salary)'
     *
     * This is used by BMQL to form the return results.
     * @deprecated
     * @see StoreQuery#getTargets()
     */
    @Deprecated
    void targets(String[] targets);


    /**
     * Get the set of attributes requested by this query
     *
     * @return the requested attributes (if any)
     */
    Set<Attribute<?>> requestedAttributes();

    /**
     * Get the set of attributes to group result set by
     * @return attributes to group by (if any)
     * @since 2.6
     */
    Set<Attribute<?>> groupByAttributes();

    /**
     * Get the requested search orderings
     *
     * @return the request sort orders (if any)
     */
    List<Ordering> getOrdering();

    /**
     * Get the maximum number of results to return
     *
     * @return max results. A negative number means unlimited results
     */
    int maxResults();

    /**
     * Get the requested aggregators
     * 
     * @return the include aggregators (if any)
     */
    List<Aggregator> getAggregators();
    
    /**
     * Get execution hints for this query
     * @return null if no hints were provided
     */
    ExecutionHints getExecutionHints();
    
    /**
     * Get the requested aggregator instances
     *
     * @return the include aggregator instances (if any)
     */
    List<AggregatorInstance<?>> getAggregatorInstances();

    /**
     * An attribute / direction ordering pair
     */
    public interface Ordering {
        /**
         * Attribute to order by
         *
         * @return attribute
         */
        Attribute<?> getAttribute();

        /**
         * Ordering direction
         *
         * @return direction
         */
        Direction getDirection();
    }
    
}
