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
 * Determine the minimum value
 *
 * @author teck
 * @param <T>
 */
public class Min<T> implements AggregatorInstance<T> {

    private Comparable min;
    private final Attribute<?> attribute;

    /**
     * @param attribute
     */
    public Min(Attribute<?> attribute) {
        this.attribute = attribute;
    }

    /**
     * {@inheritDoc}
     */
    public Min<T> createClone() {
        return new Min(attribute);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: May return null if no input provided
     */
    public T aggregateResult() {
        return (T) min;

    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: null input values are ignored
     */
    public void accept(Object input) throws AggregatorException {
        if (input == null) {
            return;
        }

        Comparable next = getComparable(input);

        if (min == null) {
            min = next;
        } else if (next.compareTo(min) < 0) {
            min = next;
        }
    }

    private static Comparable getComparable(Object o) {
        if (o instanceof Comparable) {
            return (Comparable) o;
        }

        throw new AggregatorException("Value is not Comparable: " + o.getClass());
    }

    /**
     * {@inheritDoc}
     */
    public Attribute getAttribute() {
        return attribute;
    }
}
