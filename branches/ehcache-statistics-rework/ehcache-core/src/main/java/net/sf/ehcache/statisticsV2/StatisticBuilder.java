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

package net.sf.ehcache.statisticsV2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

public final class StatisticBuilder {
    
    private StatisticBuilder() {

    }

    public static <T extends Enum<T>> OperationStatisticBuilder<T> operation(Class<T> type) {
        return new OperationStatisticBuilder(type);
    }

    public static class OperationStatisticBuilder<T extends Enum<T>> extends AbstractStatisticBuilder<OperationStatisticBuilder<T>> {

        private final Class<T> type;

        public OperationStatisticBuilder(Class<T> type) {
            this.type = type;
        }
        
        public OperationObserver<T> build() {
            if (context == null || name == null) {
                throw new IllegalStateException();
            } else {
                return StatisticsManager.createOperationStatistic(context, name, tags, type);
            }
        }
    }
    
    static class AbstractStatisticBuilder<T extends AbstractStatisticBuilder> {
        
        protected final Set<String> tags = new HashSet<String>();
        protected Object context;
        protected String name;
        
        public T of(Object of) {
            if (context == null) {
                context = of;
                return (T) this;
            } else {
                throw new IllegalStateException("Context already defined");
            }
        }
        
        public T named(String name) {
            if (this.name == null) {
                this.name = name;
                return (T) this;
            } else {
                throw new IllegalStateException("Name already defined");
            }
        }
        
        public T tag(String ... tags) {
            Collections.addAll(this.tags, tags);
            return (T) this;
        }
    }
}
