package net.sf.ehcache.search.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.StoreQuery.Ordering;

/**
 * Results implementation
 */
public class ResultsImpl implements Results {

    private final List<Result> results;
    private final boolean hasKeys;

    public ResultsImpl(List<Result> results, boolean hasKeys) {
        this.hasKeys = hasKeys;
        this.results = Collections.unmodifiableList(results);
    }

    public void discard() {
        // no-op
    }

    public List<Result> all() throws SearchException {
        return results;
    }

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

    public Object aggregateResult() throws SearchException {
        throw new UnsupportedOperationException("Did not implemented yet.");
    }

    public int size() {
        return results.size();
    }

    public boolean hasKeys() {
        return hasKeys;
    }

    public boolean isAggregate() {
        return false;
    }

    /**
     * Result implementation
     */
    public static class ResultImpl implements Result {

        private final Object key;
        private final StoreQuery query;
        private final Map<String, String> attributes;
        private final Object[] sortAttributes;

        public ResultImpl(Object key, StoreQuery query, Map<String, String> attributes) {
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

        Object getSortAttribute(int pos) {
            return sortAttributes[pos];
        }

        public Object getKey() {
            if (query.requestsKeys()) {
                return key;
            }

            throw new SearchException("keys not included in query");
        }

        public Object getValue() {
            Object key = getKey();
            System.out.println("[ResultImpl] getValue for key : " + key);
            Element e = query.getCache().get(key);
            if (e == null) {
                return null;
            }
            return e.getObjectValue();
        }

        public <T> T getAttribute(Attribute<T> attribute) {
            String name = attribute.getAttributeName();
            Object value = attributes.get(name);
            if (value == null) {
                throw new SearchException("Attribute [" + name + "] not included in query");
            }
            return (T) value;
        }

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

        public OrderComparator(List<Ordering> orderings) {
            comparators = new ArrayList<Comparator<Result>>();
            int pos = 0;
            for (Ordering ordering : orderings) {
                switch (ordering.getDirection()) {
                    case ASCENDING: {
                        comparators.add(new AscendingComparator(pos));
                        break;
                    }
                    case DESCENDING: {
                        comparators.add(new DescendingComparator(pos));
                        break;
                    }
                    default: {
                        throw new AssertionError(ordering.getDirection());
                    }
                }

                pos++;
            }
        }

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

            private final int pos;

            public AscendingComparator(int pos) {
                this.pos = pos;
            }

            public int compare(Result o1, Result o2) {
                Object attr1 = ((ResultImpl) o1).getSortAttribute(pos);
                Object attr2 = ((ResultImpl) o2).getSortAttribute(pos);

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

            private final int pos;

            public DescendingComparator(int pos) {
                this.pos = pos;
            }

            public int compare(Result o1, Result o2) {
                Object attr1 = ((ResultImpl) o1).getSortAttribute(pos);
                Object attr2 = ((ResultImpl) o2).getSortAttribute(pos);

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