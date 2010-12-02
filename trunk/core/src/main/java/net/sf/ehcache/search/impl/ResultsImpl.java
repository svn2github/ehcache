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
import java.util.List;

import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;

/**
 * Results implementation
 *
 * @author teck
 */
public class ResultsImpl implements Results {

    private final List<Result> results;
    private final List<Object> aggregateResults;
    private final boolean hasKeys;
    private final boolean hasAttributes;

    /**
     * Constructor
     *
     * @param results
     * @param hasKeys
     * @param aggregateResults
     * @param hasAttributes
     */
    public ResultsImpl(List<Result> results, boolean hasKeys, List<Object> aggregateResults, boolean hasAttributes) {
        this.hasKeys = hasKeys;
        this.hasAttributes = hasAttributes;
        this.results = Collections.unmodifiableList(results);
        this.aggregateResults = Collections.unmodifiableList(aggregateResults);
    }

    /**
     * {@inheritDoc}
     */
    public void discard() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> all() throws SearchException {
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> range(int start, int length) throws SearchException {
        if (start < 0) {
            throw new IllegalArgumentException("start: " + start);
        }

        if (length < 0) {
            throw new IllegalArgumentException("length: " + length);
        }

        int size = results.size();

        if (start > size - 1 || length == 0) {
            return Collections.EMPTY_LIST;
        }

        int end = start + length;

        if (end > size) {
            end = size;
        }

        return results.subList(start, end);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return results.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasKeys() {
        return hasKeys;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAttributes() {
        return hasAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAggregators() {
        return aggregateResults.size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    public List<Object> getAggregatorResults() throws SearchException {
        if (!hasAggregators()) {
            throw new SearchException("No aggregators present in query");
        }
        return this.aggregateResults;
    }

}