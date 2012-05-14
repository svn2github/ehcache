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
