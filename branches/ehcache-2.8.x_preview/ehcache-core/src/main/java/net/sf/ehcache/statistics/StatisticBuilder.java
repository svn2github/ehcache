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

package net.sf.ehcache.statistics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

/**
 * The StatisticBuilder.
 *
 * @author cdennis
 */
public final class StatisticBuilder {

    /**
     * Instantiates a new statistic builder.
     */
    private StatisticBuilder() {

    }

    /**
     * Operation.
     *
     * @param <T> the generic type
     * @param type the type
     * @return the operation statistic builder
     */
    public static <T extends Enum<T>> OperationStatisticBuilder<T> operation(Class<T> type) {
        return new OperationStatisticBuilder(type);
    }

    /**
     * The Class OperationStatisticBuilder.
     *
     * @param <T> the generic type
     */
    public static class OperationStatisticBuilder<T extends Enum<T>> extends AbstractStatisticBuilder<OperationStatisticBuilder<T>> {

        /** The type. */
        private final Class<T> type;

        /**
         * Instantiates a new operation statistic builder.
         *
         * @param type the type
         */
        public OperationStatisticBuilder(Class<T> type) {
            this.type = type;
        }

        /**
         * Builds the.
         *
         * @return the operation observer
         */
        public OperationObserver<T> build() {
            if (context == null || name == null) {
                throw new IllegalStateException();
            } else {
                return StatisticsManager.createOperationStatistic(context, name, tags, type);
            }
        }
    }

    /**
     * The Class AbstractStatisticBuilder.
     *
     * @param <T> the generic type
     */
    static class AbstractStatisticBuilder<T extends AbstractStatisticBuilder> {

        /** The tags. */
        protected final Set<String> tags = new HashSet<String>();

        /** The context. */
        protected Object context;

        /** The name. */
        protected String name;

        /**
         * Of.
         *
         * @param of the of
         * @return the t
         */
        public T of(Object of) {
            if (context == null) {
                context = of;
                return (T) this;
            } else {
                throw new IllegalStateException("Context already defined");
            }
        }

        /**
         * Named.
         *
         * @param name the name
         * @return the t
         */
        public T named(String name) {
            if (this.name == null) {
                this.name = name;
                return (T) this;
            } else {
                throw new IllegalStateException("Name already defined");
            }
        }

        /**
         * Tag.
         *
         * @param tags the tags
         * @return the t
         */
        public T tag(String ... tags) {
            Collections.addAll(this.tags, tags);
            return (T) this;
        }
    }
}
