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

import net.sf.ehcache.search.aggregator.Aggregator;

/**
 * Represents a single cache entry that has been selected by a cache query
 *
 * @author teck
 * @author Greg Luck
 */
public interface Result {

    /**
     * Return the key for this cache entry.
     *
     * @return key object (never null)
     * @throws SearchException if keys were not selected by the originating query
     */
    Object getKey() throws SearchException;

    /**
     * Return the value object for this cache entry. These are returned without further
     * need to access the cache.
     *
     * @return value object (which might be null if this entry no longer exists
     *         in the cache)
     * @throws SearchException if keys were not selected by the originating query
     */
    Object getValue() throws SearchException;

    /**
     * Retrieve the given attribute value for this cache entry
     *
     * @param attribute the attribute to retrieve
     * @return the attribute value, or null if there is none
     * @throws SearchException if the given attribute was not explicitly selected by the
     *                         originating query
     */
    <T> T getAttribute(Attribute<T> attribute) throws SearchException;


     /**
     * Retrieve the given aggregator value for this cache entry
     *
     * @param aggregator the aggregator to retrieve
     * @return the aggregator value, or null if there is none
     * @throws SearchException if the given attribute was not explicitly selected by the
     *                         originating query
     */
    <T> T getAggregator(Aggregator<T> aggregator) throws SearchException;

}
