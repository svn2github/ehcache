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
 * Helper class to construct the builtin aggregator types. These methods can be statically imported to make query building look better in
 * source code
 *
 * @author teck
 */
public final class Aggregators {

    private Aggregators() {
        //
    }

    /**
     * Construct a minimum value aggregator
     *
     * @param attribute
     * @return min aggregator
     */
    public static Aggregator min(final Attribute<?> attribute) {
        return new Aggregator() {
            public <T> AggregatorInstance<T> createInstance() {
                return new Min(attribute);
            }
        };
    }

    /**
     * Construct a maximum value aggregator
     *
     * @param attribute
     * @return max aggregator
     */
    public static Aggregator max(final Attribute<?> attribute) {
        return new Aggregator() {
            public <T> AggregatorInstance<T> createInstance() {
                return new Max(attribute);
            }
        };
    }

    /**
     * Construct an average value aggregator
     *
     * @param attribute
     * @return average aggregator
     */
    public static Aggregator average(final Attribute<?> attribute) {
        return new Aggregator() {
            public AggregatorInstance<Double> createInstance() {
                return new Average(attribute);
            }
        };
    }

    /**
     * Construct a sum aggregator
     *
     * @param attribute
     * @return sum aggregator
     */
    public static Aggregator sum(final Attribute<?> attribute) {
        return new Aggregator() {
            public AggregatorInstance<Long> createInstance() {
                return new Sum(attribute);
            }
        };
    }

    /**
     * Construct a counting aggregator
     *
     * @return count aggregator
     */
    public static Aggregator count() {
        return new Aggregator() {
            public AggregatorInstance<Integer> createInstance() {
                return new Count();
            }
        };

    }
}
