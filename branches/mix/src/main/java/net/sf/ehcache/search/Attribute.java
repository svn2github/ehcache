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

package net.sf.ehcache.search;

import java.util.Collection;

import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.Aggregators;
import net.sf.ehcache.search.expression.Between;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.expression.EqualTo;
import net.sf.ehcache.search.expression.GreaterThan;
import net.sf.ehcache.search.expression.GreaterThanOrEqual;
import net.sf.ehcache.search.expression.InCollection;
import net.sf.ehcache.search.expression.LessThan;
import net.sf.ehcache.search.expression.LessThanOrEqual;
import net.sf.ehcache.search.expression.ILike;
import net.sf.ehcache.search.expression.NotEqualTo;

/**
 * A search attribute. The main purpose of this class is to construct search {@link Criteria} referencing this attribute
 *
 * @author teck
 * @param <T>
 *            the parameterize type of this attribute
 */
public class Attribute<T> {

    private final String attributeName;

    /**
     * Construct a new attribute instance. Instances are normally obtained from a specific {@link net.sf.ehcache.Cache} however
     *
     * @param attributeName the name of search attribute
     */
    public Attribute(String attributeName) {
        if (attributeName == null) {
            throw new NullPointerException();
        }
        this.attributeName = attributeName;
    }

    /**
     * Get the attribute name
     *
     * @return the attribute name
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Create a range criteria between the given min/max (inclusive). This is the same as calling <code>between(min, max, true, true)</code>
     *
     * @param min the minimum value in the range
     * @param max the maximum value in the range
     * @return criteria instance
     */
    public Criteria between(T min, T max) {
        return between(min, max, true, true);
    }

    /**
     * Create a range criteria between the given min/max with specified inclusiveness
     *
     * @param min the minimum value in the range
     * @param max the maximum value in the range
     * @param minInclusive is the minimum inclusive in the range
     * @param maxInclusive is the maximum inclusive in the range
     * @return criteria instance
     */
    public Criteria between(T min, T max, boolean minInclusive, boolean maxInclusive) {
        return new Between(attributeName, min, max, minInclusive, maxInclusive);
    }

    /**
     * Create a criteria where this attribute is 'in' (ie. contained within) the given collection of values. With the exception of very
     * small collections a {@link java.util.Set} should perform better here to get constant time <code>contains()</code> checks
     *
     * @param values
     * @return criteria instance
     */
    public Criteria in(Collection<? extends T> values) {
        return new InCollection(attributeName, values);
    }

    /**
     * Create a criteria where this attribute is not equal to the given value
     *
     * @param value
     * @return criteria instance
     */
    public Criteria ne(T value) {
        return new NotEqualTo(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is less than the given value
     *
     * @param value
     * @return criteria instance
     */
    public Criteria lt(T value) {
        return new LessThan(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is less than or equal to the given value
     *
     * @param value
     * @return criteria instance
     */
    public Criteria le(T value) {
        return new LessThanOrEqual(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is greater than the given value
     *
     * @param value
     * @return criteria instance
     */
    public Criteria gt(T value) {
        return new GreaterThan(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is greater than or equal to the given value
     *
     * @param value
     * @return criteria instance
     */
    public Criteria ge(T value) {
        return new GreaterThanOrEqual(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is equal to the given value
     *
     * @param value
     * @return criteria instance
     */
    public Criteria eq(T value) {
        return new EqualTo(attributeName, value);
    }

    /**
     * Create a criteria where this attribute's toString() matches the given expression
     * See {@link net.sf.ehcache.search.expression.ILike} for the expression syntax
     *
     * @param regex
     * @return criteria instance
     */
    public Criteria ilike(String regex) {
        return new ILike(attributeName, regex);
    }

    /**
     * Request a count aggregation of this attribute
     *
     * @return count aggregator
     */
    public Aggregator count() {
        return Aggregators.count();
    }

    /**
     * Request a maximum value aggregation of this attribute
     *
     * @return max aggregator
     */
    public Aggregator max() {
        return Aggregators.max(this);
    }

    /**
     * Request a minimum value aggregation of this attribute
     *
     * @return min aggregator
     */
    public Aggregator min() {
        return Aggregators.min(this);
    }

    /**
     * Request a sum aggregation of this attribute
     *
     * @return sum aggregator
     */
    public Aggregator sum() {
        return Aggregators.sum(this);
    }

    /**
     * Request an average value aggregation of this attribute
     *
     * @return average aggregator
     */
    public Aggregator average() {
        return Aggregators.average(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return attributeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return attributeName.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Attribute) {
            Attribute other = (Attribute) obj;
            return attributeName.equals(other.attributeName);
        }
        return false;
    }

}
