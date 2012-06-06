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

import java.util.List;

/**
 * Represents a single cache entry that has been selected by a cache query.
 * <p/>
 *
 * @author teck
 * @author Greg Luck
 */
public interface Result {

    /**
     * Return the key for this cache entry.
     *
     * @return key object
     * @throws SearchException if keys were not selected by the originating query
     */
    Object getKey() throws SearchException;

    /**
     * Return the value for this cache entry.
     *
     * @return value object. This value might be null if the value is no longer referenced
     *         by the cache (ie. a concurrent update removed this entry).
     * @throws SearchException if values were not selected by the originating query
     */
    Object getValue() throws SearchException;

    /**
     * Retrieve the given attribute value for this cache entry
     *
     * @param attribute the attribute to retrieve
     * @return the attribute value, or null if there is none
     * @throws SearchException if the given attribute was not explicitly selected by the
     *             originating query
     */
    <T> T getAttribute(Attribute<T> attribute) throws SearchException;

    /**
     * Retrieve the aggregator value(s)
     *
     * @return the aggregators value as a {@link List}. The aggregator results will be in the same order they were added to the query
     * @throws SearchException if no aggregators were requested in the query
     */
    List<Object> getAggregatorResults() throws SearchException;

}
