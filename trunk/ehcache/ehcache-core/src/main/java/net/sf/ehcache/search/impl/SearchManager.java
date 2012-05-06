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

package net.sf.ehcache.search.impl;

import java.util.Map;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.StoreQuery;

/**
 * Interface for ehcache search engine implementations
 *
 * @author teck
 */
public interface SearchManager {

    /**
     * shutdown the search manager
     */
    void shutdown();

    /**
     * initialize the search manager
     */
    void init();

    /**
     * Execute a query against the given cache
     *
     * @param cacheName cache name
     * @param query query to execute
     * @param attributeExtractors defined attribute extractors for the cache
     * @return search results
     */
    Results executeQuery(String cacheName, StoreQuery query, Map<String, AttributeExtractor> attributeExtractors);

    /**
     * Notify an element added to a segment of a given cache
     *
     * @param cacheName cache name
     * @param segmentId segment of cache
     * @param uniqueKey unique key of element
     * @param serializedKey serialized form of the element key
     * @param element element being added to cache
     * @param extractors the attribute extractors for the cache
     */
    void put(String cacheName, int segmentId, String uniqueKey, byte[] serializedKey, Element element,
            Map<String, AttributeExtractor> extractors);

    /**
     * Notify an element removed from a segment of a given cache
     *
     * @param cacheName cache name
     * @param uniqueKey unique key of element
     * @param segmentId segment of cache
     */
    void remove(String cacheName, String uniqueKey, int segmentId);

    /**
     * Clear a segment of the given cache
     *
     * @param cacheName cache name to clear
     * @param segmentId segment of cache
     */
    void clear(String cacheName, int segmentId);

}
