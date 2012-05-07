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

package net.sf.ehcache.search.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.sf.ehcache.store.StoreQuery.Ordering;

/**
 * Compound sort ordering comparactor
 *
 * @author teck
 * @param <T>
 */
public class OrderComparator<T extends BaseResult> implements Comparator<T> {

    private final List<Comparator<T>> comparators;

    /**
     * Constructor
     *
     * @param orderings
     */
    public OrderComparator(List<Ordering> orderings) {
        comparators = new ArrayList<Comparator<T>>();
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

    /**
     * {@inheritDoc}
     */
    public int compare(T o1, T o2) {
        for (Comparator<T> c : comparators) {
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
    private class AscendingComparator implements Comparator<T>, Serializable {

        private final int pos;

        AscendingComparator(int pos) {
            this.pos = pos;
        }

        public int compare(T o1, T o2) {
            Object attr1 = o1.getSortAttribute(pos);
            Object attr2 = o2.getSortAttribute(pos);

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
    private class DescendingComparator implements Comparator<T>, Serializable {

        private final int pos;

        DescendingComparator(int pos) {
            this.pos = pos;
        }

        public int compare(T o1, T o2) {
            Object attr1 = o1.getSortAttribute(pos);
            Object attr2 = o2.getSortAttribute(pos);

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
