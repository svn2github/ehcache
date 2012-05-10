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

package net.sf.ehcache.search.aggregator;

import net.sf.ehcache.search.Attribute;


/**
 * Counts the number of results
 *
 * @author Greg Luck
 */
public class Count implements AggregatorInstance<Integer> {

    private int count;

    /**
     * {@inheritDoc}
     */
    public Count createClone() {
        return new Count();
    }
    /**
     * {@inheritDoc}
     */
    public void accept(Object input) throws AggregatorException {
        count++;
    }

    /**
     * {@inheritDoc}
     */
    public Integer aggregateResult() {
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public Attribute<?> getAttribute() {
        return null;
    }

}
