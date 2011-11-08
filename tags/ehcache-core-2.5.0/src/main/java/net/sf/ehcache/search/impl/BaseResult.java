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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.store.StoreQuery;

/**
 * Base result implementation
 *
 * @author teck
 */
public abstract class BaseResult implements Result {

    private final StoreQuery query;
    private volatile List<Object> aggregateResults = Collections.emptyList();

    /**
     * Constructor
     *
     * @param query
     */
    public BaseResult(StoreQuery query) {
        this.query = query;
    }

    /**
     * Set the aggregate results for this row
     *
     * @param aggregateResults
     */
    public void setAggregateResults(List<Object> aggregateResults) {
        this.aggregateResults = Collections.unmodifiableList(aggregateResults);
    }

    /**
     * {@inheritDoc}
     */
    public Object getKey() {
        if (query.requestsKeys()) {
            return basicGetKey();
        }

        throw new SearchException("keys not included in query. Use includeKeys() to add keys to results.");
    }

    /**
     * Get the actual key value
     *
     * @return key
     */
    protected abstract Object basicGetKey();

    /**
     * {@inheritDoc}
     */
    public List<Object> getAggregatorResults() throws SearchException {
        if (this.aggregateResults.isEmpty()) {
            throw new SearchException("No aggregators present in query");
        }
        return this.aggregateResults;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValue() throws SearchException {
        if (query.requestsValues()) {
            return basicGetValue();
        }

        throw new SearchException("values not included in query. Use includeValues() to add values to results.");
    }

    /**
     * Get the actual value
     *
     * @return value
     */
    protected abstract Object basicGetValue();

    /**
     * {@inheritDoc}
     */
    public <T> T getAttribute(Attribute<T> attribute) {
        String name = attribute.getAttributeName();

        if (!query.requestedAttributes().contains(attribute)) {
            throw new SearchException("Attribute [" + name + "] not included in query");
        }

        return (T) basicGetAttribute(name);
    }


    /**
     * Get the actual attribute value
     *
     * @return attribute
     */
    protected abstract Object basicGetAttribute(String name);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Result(");

        if (query.requestsKeys()) {
            sb.append("key=");
            sb.append(getKey());
        } else {
            sb.append("[no key]");
        }

        sb.append(", ");
        if (query.requestsValues()) {
            sb.append("value=");
            sb.append(getValue());
        } else {
            sb.append("[no value]");
        }

        sb.append(", ");
        if (!query.requestedAttributes().isEmpty()) {
            Map<String, String> attrs = new HashMap<String, String>();
            for (Attribute a : query.requestedAttributes()) {
                attrs.put(a.getAttributeName(), String.valueOf(getAttribute(a)));
            }

            sb.append("attributes=" + attrs);
        } else {
            sb.append("[no attributes]");
        }

        sb.append(", ");
        if (!aggregateResults.isEmpty()) {
            sb.append("aggregateResults=" + getAggregatorResults());
        } else {
            sb.append("[no aggregateResults]");
        }

        sb.append(")");
        return sb.toString();
    }


}
