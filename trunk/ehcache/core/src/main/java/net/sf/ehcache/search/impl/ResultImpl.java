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

import net.sf.ehcache.store.StoreQuery;

/**
 * Result implementation
 *
 * @author teck
 */
public class ResultImpl extends BaseResult {

    private final Object key;
    private final Object value;
    private final Map<String, Object> attributes;
    private final Object[] sortAttributes;

    /**
     * Constructor
     *
     * @param key
     * @param value
     * @param query
     * @param attributes
     * @param sortAttributes
     */
    public ResultImpl(Object key, Object value, StoreQuery query, Map<String, Object> attributes, Object[] sortAttributes) {
        super(query);
        this.key = key;
        this.value = value;
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

    @Override
    protected Object basicGetKey() {
        return key;
    }

    @Override
    protected Object basicGetValue() {
        return value;
    }

    @Override
    protected Object basicGetAttribute(String name) {
        return attributes.get(name);
    }

}
