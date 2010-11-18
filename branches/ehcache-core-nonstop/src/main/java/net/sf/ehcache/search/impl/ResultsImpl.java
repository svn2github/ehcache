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

package net.sf.ehcache.search.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.StoreQuery.Ordering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Results implementation
 *
 * @author nelrahma
 */
public class ResultsImpl implements Results {

    private static final Logger LOG = LoggerFactory.getLogger(ResultsImpl.class);

    private final List<Result> results;
    private final List aggregatorResults;
    private final boolean hasKeys;
    private final boolean hasAggregates;
    private final boolean hasAttributes;

    /**
     * Constructs a list of results
     *
     * @param results
     * @param hasKeys
     */
    public ResultsImpl(List<Result> results, boolean hasKeys, boolean hasAggregates, boolean hasAttributes, List aggregatorResults) {
        this.hasKeys = hasKeys;
        this.hasAggregates = hasAggregates;
        this.hasAttributes = hasAttributes;
        this.aggregatorResults = Collections.unmodifiableList(aggregatorResults);
        this.results = Collections.unmodifiableList(results);
    }

    /**
     * {@inheritDoc}
     */
    public void discard() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> all() throws SearchException {
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> range(int start, int length) throws SearchException, IndexOutOfBoundsException {
        if (start < 0) {
            throw new IllegalArgumentException("start: " + start);
        }

        if (length < 0) {
            throw new IllegalArgumentException("length: " + length);
        }

        int size = results.size();

        if (start > size - 1 || length == 0) {
            return Collections.EMPTY_LIST;
        }

        int end = start + length;

        if (end > size) {
            end = size;
        }

        return results.subList(start, end);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return results.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasKeys() {
        return hasKeys;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAttributes() {
        return hasAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAggregators() {
        return hasAggregates;
    }

    /**
     * {@inheritDoc}
     */
    public List<Object> getAggregatorResults() throws SearchException {
        return this.aggregatorResults;
    }

    /**
     * Result implementation
     */
    public static class ResultImpl implements Result {

        private final Object key;
        private final StoreQuery query;
        private final Map<String, Object> attributes;
        private final Object[] sortAttributes;

        /**
         * Result implementation
         *
         * @param query
         * @param attributes
         */
        public ResultImpl(StoreQuery query, Map<String, Object> attributes) {
            this(null, query, attributes);
        }

        /**
         * Result implementation
         *
         * @param key
         * @param query
         * @param attributes
         */
        public ResultImpl(Object key, StoreQuery query, Map<String, Object> attributes) {
            this.query = query;
            this.key = key;
            this.attributes = attributes;

            List<Ordering> orderings = query.getOrdering();
            if (orderings.isEmpty()) {
                sortAttributes = null;
            } else {
                sortAttributes = new Object[orderings.size()];
                for (int i = 0; i < sortAttributes.length; i++) {
                    String name = orderings.get(i).getAttribute().getAttributeName();
                    sortAttributes[i] = attributes.get(name);
                }
            }
        }

        /**
         * @param position
         * @return the sort attributes
         */
        Object getSortAttribute(int position) {
            return sortAttributes[position];
        }

        /**
         * {@inheritDoc}
         */
        public Object getKey() {
            if (query.requestsKeys()) {
                return key;
            }

            throw new SearchException("keys not included in query");
        }

        /**
         * {@inheritDoc}
         */
        public <T> T getAttribute(Attribute<T> attribute) {
            String name = attribute.getAttributeName();
            Object value = attributes.get(name);
            if (value == null) {
                throw new SearchException("Attribute [" + name + "] not included in query");
            }
            return (T) value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "ResultImpl [attributes=" + attributes + ", key=" + key + ", query=" + query + ", sortAttributes="
                    + Arrays.toString(sortAttributes) + "]";
        }
    }

    /**
     * A compound comparator to implements query ordering
     */
    public static class OrderComparator implements Comparator<Result> {

        private final List<Comparator<Result>> comparators;

        /**
         * @param orderings
         */
        public OrderComparator(List<Ordering> orderings) {
            comparators = new ArrayList<Comparator<Result>>();
            int position = 0;
            for (Ordering ordering : orderings) {
                switch (ordering.getDirection()) {
                    case ASCENDING: {
                        comparators.add(new AscendingComparator(position));
                        break;
                    }
                    case DESCENDING: {
                        comparators.add(new DescendingComparator(position));
                        break;
                    }
                    default: {
                        throw new AssertionError(ordering.getDirection());
                    }
                }

                position++;
            }
        }

        /**
         * {@inheritDoc}
         */
        public int compare(Result o1, Result o2) {
            for (Comparator<Result> c : comparators) {
                int cmp = c.compare(o1, o2);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        }

        /**
         * Simple ascending comparator
         */
        public static class AscendingComparator implements Comparator<Result> {

            private final int position;

            /**
             * @param position
             */
            public AscendingComparator(int position) {
                this.position = position;
            }

            /**
             * {@inheritDoc}
             */
            public int compare(Result o1, Result o2) {
                Object attr1 = ((ResultImpl) o1).getSortAttribute(position);
                Object attr2 = ((ResultImpl) o2).getSortAttribute(position);

                if ((attr1 == null) && (attr2 == null)) {
                    return 0;
                }

                if (attr1 == null) {
                    return -1;
                }

                if (attr2 == null) {
                    return 1;
                }

                return ((Comparable) attr1).compareTo(attr2);
            }
        }

        /**
         * Simple descending comparator
         */
        public static class DescendingComparator implements Comparator<Result> {

            private final int position;

            /**
             * @param position
             */
            public DescendingComparator(int position) {
                this.position = position;
            }

            /**
             * {@inheritDoc}
             */
            public int compare(Result o1, Result o2) {
                Object attr1 = ((ResultImpl) o1).getSortAttribute(position);
                Object attr2 = ((ResultImpl) o2).getSortAttribute(position);

                if ((attr1 == null) && (attr2 == null)) {
                    return 0;
                }

                if (attr1 == null) {
                    return 1;
                }

                if (attr2 == null) {
                    return -1;
                }

                return ((Comparable) attr2).compareTo(attr1);
            }
        }
    }

}
