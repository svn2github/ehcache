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

package net.sf.ehcache.store;

import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
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
     * Get the list of attributes requested by this query
     * 
     * @return the requested attributes (if any)
     */
    List<Attribute<?>> requestedAttributes();

}
