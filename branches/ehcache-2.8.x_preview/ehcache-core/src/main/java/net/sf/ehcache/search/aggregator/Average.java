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
 * Compute the average (arithmetic mean) as a double
 *
 * @author teck
 */
public class Average implements AggregatorInstance<Double> {

    private final Attribute<?> attribute;

    private Engine engine;

    /**
     * @param attribute
     */
    public Average(Attribute<?> attribute) {
        this.attribute = attribute;
    }

    /**
     * {@inheritDoc}
     */
    public Average createClone() {
        return new Average(attribute);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: Null values are ignored and not included in the computation
     */
    public void accept(Object input) throws AggregatorException {
        if (input == null) {
            return;
        }

        if (input instanceof Number) {
            if (engine == null) {
                engine = Engine.create((Number) input);
            } else {
                engine.accept((Number) input);
            }
        } else {
            throw new AggregatorException("Non-number type encountered: " + input.getClass());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: null is returned if there was no input supplied to this function
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
     * Abstract super-class for all average calculating engines.
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
            } else if (value instanceof Long) {
                return new LongEngine(value.longValue());
            } else {
                return new IntegerEngine(value.intValue());
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
         * An int based averaging engine.
         */
        static class IntegerEngine extends Engine {

            private int count;
            private long sum;

            /**
             * Creates a new instance starting with an initial value
             *
             * @param value initial value
             */
            IntegerEngine(int value) {
                this.count = 1;
                this.sum = value;
            }

            @Override
            void accept(Number input) throws AggregatorException {
                count++;
                sum += input.intValue();
            }

            @Override
            Number result() {
                return Float.valueOf(((float) sum) / count);
            }
        }

        /**
         * A long based averaging engine.
         */
        static class LongEngine extends Engine {

            private int count;
            private long sum;

            /**
             * Creates a new instance starting with an initial value
             *
             * @param value initial value
             */
            LongEngine(long value) {
                this.count = 1;
                this.sum = value;
            }

            @Override
            void accept(Number input) throws AggregatorException {
                count++;
                sum += input.longValue();
            }

            @Override
            Number result() {
                return Double.valueOf(((double) sum) / count);
            }
        }

        /**
         * A float based averaging engine.
         */
        static class FloatEngine extends Engine {

            private int count;
            private float sum;

            /**
             * Creates a new instance starting with an initial value
             *
             * @param value initial value
             */
            FloatEngine(float value) {
                this.count = 1;
                this.sum = value;
            }

            @Override
            void accept(Number input) throws AggregatorException {
                count++;
                sum += input.floatValue();
            }

            @Override
            Number result() {
                return Float.valueOf(sum / count);
            }
        }

        /**
         * A double based averaging engine.
         */
        static class DoubleEngine extends Engine {

            private int count;
            private double sum;

            /**
             * Creates a new instance starting with an initial value
             *
             * @param value initial value
             */
            DoubleEngine(double value) {
                this.count = 1;
                this.sum = value;
            }

            @Override
            void accept(Number input) throws AggregatorException {
                count++;
                sum += input.doubleValue();
            }

            @Override
            Number result() {
                return Double.valueOf(sum / count);
            }
        }
    }
}
