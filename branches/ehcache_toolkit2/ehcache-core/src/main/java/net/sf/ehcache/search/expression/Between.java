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

package net.sf.ehcache.search.expression;

import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeType;

/**
 * Range criteria
 *
 * @author teck
 */
public class Between extends ComparableValue {

    private final Comparable min;
    private final Comparable max;
    private final boolean minInclusive;
    private final boolean maxInclusive;

    /**
     * Constructor
     *
     * @param attributeName attribute name
     * @param min           minimum value of range
     * @param max           maximum value of range
     * @param minInclusive  is minimum inclusive?
     * @param maxInclusive  is maximum inclusive?
     */
    public Between(String attributeName, Object min, Object max, boolean minInclusive, boolean maxInclusive) {
        super(attributeName, computeType(attributeName, min, max));

        this.min = (Comparable) min;
        this.max = (Comparable) max;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    private static AttributeType computeType(String attributeName, Object min, Object max) {
        if ((min == null) || (max == null)) {
            throw new NullPointerException();
        }

        AttributeType minType = AttributeType.typeFor(attributeName, min);
        AttributeType maxType = AttributeType.typeFor(attributeName, max);

        if (minType != maxType) {
            throw new SearchException("Different types for min (" + minType + ") and max (" + maxType + ")");
        }

        return minType;
    }

    /**
     * Get the minimum value
     *
     * @return min value
     */
    public Comparable getMin() {
        return min;
    }

    /**
     * Get the maximum value
     *
     * @return max value
     */
    public Comparable getMax() {
        return max;
    }

    /**
     * @return true if the min is included in range
     */
    public boolean isMinInclusive() {
        return minInclusive;
    }

    /**
     * @return true if the max is included in range
     */
    public boolean isMaxInclusive() {
        return maxInclusive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeComparable(Comparable attributeValue) {
        int minCmp = attributeValue.compareTo(min);
        if (minCmp < 0 || minCmp == 0 && !minInclusive) {
            return false;
        }

        int maxCmp = attributeValue.compareTo(max);
        if (maxCmp > 0 || maxCmp == 0 && !maxInclusive) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeComparableString(Comparable attributeValue) {
        int minCmp = luceneStringCompare(attributeValue.toString(), min.toString());
        if (minCmp < 0 || minCmp == 0 && !minInclusive) {
            return false;
        }

        int maxCmp = luceneStringCompare(attributeValue.toString(), max.toString());
        if (maxCmp > 0 || maxCmp == 0 && !maxInclusive) {
            return false;
        }

        return true;
    }

}
