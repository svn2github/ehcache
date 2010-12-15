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

import java.util.Arrays;
import java.util.Map;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.store.StoreQuery;

/**
 * Result implementation
 *
 * @author teck
 */
public class ResultImpl implements Result {

    private final Object key;
    private final StoreQuery query;
    private final Map<String, Object> attributes;
    private final Object[] sortAttributes;

    /**
     * Constructor
     *
     * @param key
     * @param query
     * @param attributes
     * @param sortAttributes
     */
    public ResultImpl(Object key, StoreQuery query, Map<String, Object> attributes, Object[] sortAttributes) {
        this.query = query;
        this.key = key;
        this.attributes = attributes;
        this.sortAttributes = sortAttributes;
    }

    /**
     * Get attribute value for use in sorting
     *
     * @param pos
     * @return
     */
    Object getSortAttribute(int pos) {
        return sortAttributes[pos];
    }

    /**
     * {@inheritDoc}
     */
    public Object getKey() {
        if (query.requestsKeys()) {
            return key;
        }

        throw new SearchException("keys not included in query. Use includeKeys() to add keys to results.");
    }

    /**
     * {@inheritDoc}
     */
    public <T> T getAttribute(Attribute<T> attribute) {
        String name = attribute.getAttributeName();
        Object value = attributes.get(name);
        if (value == null && !query.requestedAttributes().contains(attribute)) {
            throw new SearchException("Attribute [" + name + "] not included in query");
        }
        return (T) value;
    }

    @Override
    public String toString() {
        return "ResultImpl [attributes=" + attributes + ", key=" + key + ", query=" + query + ", sortAttributes="
                + Arrays.toString(sortAttributes) + "]";
    }

}