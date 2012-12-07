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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

public final class StatisticBuilder {
    
    public static final String NAME_PROP = "name";
    public static final String TAGS_PROP = "tags";
    public static final String RETRIEVAL_COST_PROP = "retrieval-cost";
    public static final String RECORDING_COST_PROP = "recording-cost";

    private StatisticBuilder() {

    }

    public static <T extends Enum<T>> OperationStatisticBuilder<T> operation(Class<T> type) {
        return new OperationStatisticBuilder(type);
    }

    public static <T extends Number> PassThroughStatisticBuilder<T> passThrough(Callable<T> callable) {
        return new PassThroughStatisticBuilder(callable);
    }

    public static EventStatisticBuilder event() {
        return new EventStatisticBuilder();
    }

    public static class OperationStatisticBuilder<T extends Enum<T>> extends AbstractStatisticBuilder<OperationStatisticBuilder<T>> {

        private final Class<T> type;

        public OperationStatisticBuilder(Class<T> type) {
            this.type = type;
        }
        
        public OperationStatisticBuilder<T> recordingCost(Cost cost) {
            addProperty(RECORDING_COST_PROP, cost);
            return this;
        }
        
        public OperationObserver<T> build() {
            if (context == null || !properties.containsKey(RETRIEVAL_COST_PROP) || !properties.containsKey(RECORDING_COST_PROP)) {
                throw new IllegalStateException();
            } else {
                return StatisticsManager.createOperationStatistic(context, properties, type);
            }
        }
    }
    
    public static class PassThroughStatisticBuilder<T extends Number> extends AbstractStatisticBuilder<PassThroughStatisticBuilder<T>> {
        
        private final Callable<T> callable;
        
        public PassThroughStatisticBuilder(Callable<T> callable) {
            this.callable = callable;
        }
        
        
        public void build() {
            StatisticsManager.<T>createPassThroughStatistic(context, properties, callable);
        }
    }
    
    public static class EventStatisticBuilder {
        
    }

    static class AbstractStatisticBuilder<T extends AbstractStatisticBuilder> {
        
        protected final Map<String, Object> properties = new HashMap<String, Object>();
        
        protected Object context;
        
        public T of(Object of) {
            if (context == null) {
                context = of;
                return (T) this;
            } else {
                throw new IllegalStateException("Context already defined");
            }
        }
        
        public T named(String name) {
            addProperty(NAME_PROP, name);
            return (T) this;
        }
        
        public T tag(String ... tags) {
            Set<String> tagSet = (Set<String>) properties.get(TAGS_PROP);
            if (tagSet == null) {
                tagSet = new HashSet<String>();
                properties.put(TAGS_PROP, tagSet);
            }
            Collections.addAll(tagSet, tags);
            return (T) this;
        }

        public T retrievalCost(Cost cost) {
            addProperty(RETRIEVAL_COST_PROP, cost);
            return (T) this;
        }
        
        protected void validate() {
            if (context == null || !properties.containsKey(RETRIEVAL_COST_PROP)) {
                throw new IllegalStateException();
            }
        }
        
        protected void addProperty(String name, Object value) {
            if (properties.containsKey(name)) {
                throw new IllegalStateException();
            } else {
                properties.put(name, value);
            }
        }
    }
}
