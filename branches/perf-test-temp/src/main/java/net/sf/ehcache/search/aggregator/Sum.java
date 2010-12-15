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

package net.sf.ehcache.search.aggregator;

import net.sf.ehcache.search.Attribute;

/**
 * Sums the results
 * <p/>
 * Sum can be used with most numeric types
 *
 * @author Greg Luck
 */
public class Sum implements AggregatorInstance<Long> {

    private long sum;
    private final Attribute<?> attribute;

    /**
     * @param attribute
     */
    public Sum(Attribute<?> attribute) {
        this.attribute = attribute;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: null inputs are ignored
     */
    public void accept(Object input) throws AggregatorException {
        if (input == null) {
            return;
        }

        if (input instanceof Number) {
            sum += ((Number) input).longValue();
        } else {
            throw new AggregatorException("Non-number type encounted: " + input.getClass());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: May return null if no input supplied
     */
    public Long aggregateResult() {
        return sum;
    }

    /**
     * {@inheritDoc}
     */
    public Attribute getAttribute() {
        return attribute;
    }

}
