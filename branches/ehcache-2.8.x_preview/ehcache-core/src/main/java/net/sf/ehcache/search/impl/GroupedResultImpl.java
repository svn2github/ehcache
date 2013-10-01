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

package net.sf.ehcache.search.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.store.StoreQuery;

/**
 * Representation of single result row from group-by queries
 * @author vfunshte
 *
 */
public class GroupedResultImpl extends BaseResult {
    private final Map<String, Object> attributes;
    private final Object[] sortAttributes;
    private final Map<String, Object> groupByValues;

    /**
     * Constructor
     *
     * @param query
     * @param attributes
     * @param sortAttributes
     * @param aggregatorResults
     * @param groupBy
     */
    public GroupedResultImpl(StoreQuery query, Map<String, Object> attributes, Object[] sortAttributes,
            List<Object> aggregatorResults, Map<String, Object> groupBy) {
        super(query);
        this.attributes = attributes;
        this.sortAttributes = sortAttributes;
        this.groupByValues = groupBy;
        setAggregateResults(aggregatorResults);
    }

    @Override
    protected Object basicGetKey() {
        throw new AssertionError("Not supported");
    }

    @Override
    protected Object basicGetValue() {
        throw new AssertionError("Not supported");
    }

    @Override
    protected Object basicGetAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    Object getSortAttribute(int pos) {
        return sortAttributes[pos];
    }

    /**
     * Map of attributes to their values, used to create this grouped result
     * @return read-only map of group by attributes
     */
    public Map<String, Object> getGroupByValues() {
        return Collections.unmodifiableMap(groupByValues);
    }
}
