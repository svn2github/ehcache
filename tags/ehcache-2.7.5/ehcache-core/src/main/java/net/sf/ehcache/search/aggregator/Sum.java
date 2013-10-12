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
 * Sums the results
 * <p/>
 * Sum can be used with most numeric types
 *
 * @author Greg Luck
 */
public class Sum implements AggregatorInstance<Long> {

    private final Attribute<?> attribute;

    private Engine engine;

    /**
     * @param attribute
     */
    public Sum(Attribute<?> attribute) {
        this.attribute = attribute;
    }

    /**
     * {@inheritDoc}
     */
    public Sum createClone() {
        return new Sum(attribute);
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
            if (engine == null) {
                engine = Engine.create((Number)input);
            } else {
                engine.accept((Number)input);
            }
        } else {
            throw new AggregatorException("Non-number type encountered: " + input.getClass());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: May return null if no input supplied
     */
    public Number aggregateResult() {
        if (engine == null) {
            return null;
        } else {
            return engine.result();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * Abstract super-class for all sum calculating engines.
     */
    abstract static class Engine {

        /**
         * Create a type specific engine using the given initial value.
         *
         * @param value initial value
         * @return type specific engine
         */
        static Engine create(Number value) {
            if (value instanceof Float) {
                return new FloatEngine(value.floatValue());
            } else if (value instanceof Double) {
                return new DoubleEngine(value.doubleValue());
            } else {
                return new LongEngine(value.longValue());
            }
        }

        /**
         * Update the engine with the given value.
         *
         * @param input data value
         */
        abstract void accept(Number input) throws AggregatorException;

        /**
         * Get the (current) result of this engine.
         *
         * @return engine result
         */
        abstract Number result();

        /**
         * A long based summing engine.
         */
        static class LongEngine extends Engine {

            private long sum;

            /**
             * Creates a new instance starting with an initial value
             *
             * @param value initial value
             */
            LongEngine(long value) {
                this.sum = value;
            }

            @Override
            void accept(Number input) throws AggregatorException {
                sum += input.longValue();
            }

            @Override
            Number result() {
                return Long.valueOf(sum);
            }
        }

        /**
         * A float based summing engine.
         */
        static class FloatEngine extends Engine {

            private float sum;

            /**
             * Creates a new instance starting with an initial value
             *
             * @param value initial value
             */
            FloatEngine(float value) {
                this.sum = value;
            }

            @Override
            void accept(Number input) throws AggregatorException {
                sum += input.floatValue();
            }

            @Override
            Number result() {
                return Float.valueOf(sum);
            }
        }

        /**
         * A double based summing engine.
         */
        static class DoubleEngine extends Engine {

            private double sum;

            /**
             * Creates a new instance starting with an initial value
             *
             * @param value initial value
             */
            DoubleEngine(double value) {
                this.sum = value;
            }

            @Override
            void accept(Number input) throws AggregatorException {
                sum += input.doubleValue();
            }

            @Override
            Number result() {
                return Double.valueOf(sum);
            }
        }
    }
}
