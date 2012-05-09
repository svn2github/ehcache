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

import net.sf.ehcache.store.StoreQuery;

/**
 * A result implementation intended solely for holding aggregate results. This particular result is used when
 * only aggregates are requested in a query
 *
 * @author teck
 */
public class AggregateOnlyResult extends BaseResult {

    /**
     * Constructor
     *
     * @param query
     */
    public AggregateOnlyResult(StoreQuery query) {
        super(query);
    }

    @Override
    protected Object basicGetKey() {
        throw new AssertionError();
    }

    @Override
    protected Object basicGetValue() {
        throw new AssertionError();
    }

    @Override
    protected Object basicGetAttribute(String name) {
        throw new AssertionError();
    }

    @Override
    Object getSortAttribute(int pos) {
        throw new AssertionError();
    }

}
