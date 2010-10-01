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

package net.sf.ehcache.search;

import java.util.Collection;

import net.sf.ehcache.search.expression.BetweenCriteria;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.expression.EqualCriteria;
import net.sf.ehcache.search.expression.GreaterThanCriteria;
import net.sf.ehcache.search.expression.GreaterThanOrEqualCriteria;
import net.sf.ehcache.search.expression.InCollectionCriteria;
import net.sf.ehcache.search.expression.LessThanCriteria;
import net.sf.ehcache.search.expression.LessThanOrEqualCriteria;
import net.sf.ehcache.search.expression.LikeCriteria;
import net.sf.ehcache.search.expression.NotEqualCriteria;

/**
 * A search attribute. The main purpose of this class is to construct search {@link Criteria} referencing this attribute
 * 
 * @param <T>
 *            the parameterize type of this attribute
 * @author teck
 */
public class Attribute<T> {

    private final String attributeName;

    /**
     * Construct a new attribute instance. Instances are normally obtained from a specific {@link net.sf.ehcache.Cache} however
     * 
     * @param attributeName
     *            the name of search attribute
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
     * @param min
     *            the minimum value in the range
     * @param max
     *            the maximum value in the range
     * @return criteria instance
     */
    public Criteria between(T min, T max) {
        return between(min, max, true, true);
    }

    /**
     * Create a range criteria between the given min/max with specified inclusiveness
     * 
     * @param min
     *            the minimum value in the range
     * @param max
     *            the maximum value in the range
     * @param minInclusive
     *            is the minimum inclusive in the range
     * @param maxInclusive
     *            is the maximum inclusive in the range
     * @return criteria instance
     */
    public Criteria between(T min, T max, boolean minInclusive, boolean maxInclusive) {
        return new BetweenCriteria(attributeName, min, max, minInclusive, maxInclusive);
    }

    /**
     * Create a criteria where this attribute is 'in' (ie. contained within) the given collection of values. With the exception of very
     * small collections a {@link java.util.Set} should perform better here to get constant time <code>contains()</code> checks
     * 
     * @param values
     * @return criteria instance
     */
    public Criteria in(Collection<? extends T> values) {
        return new InCollectionCriteria(attributeName, values);
    }

    /**
     * Create a criteria where this attribute is not equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria ne(T value) {
        return new NotEqualCriteria(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is less than the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria lt(T value) {
        return new LessThanCriteria(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is less than or equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria le(T value) {
        return new LessThanOrEqualCriteria(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is greater than the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria gt(T value) {
        return new GreaterThanCriteria(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is greater than or equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria ge(T value) {
        return new GreaterThanOrEqualCriteria(attributeName, value);
    }

    /**
     * Create a criteria where this attribute is equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria eq(T value) {
        return new EqualCriteria(attributeName, value);
    }

    /**
     * Create a criteria where this attribute's toString() matches the given expression
     * See {@link LikeCriteria} for the expression syntax
     * 
     * @param regex
     * @return criteria instance
     */
    public Criteria like(String regex) {
        return new LikeCriteria(attributeName, regex);
    }

}
