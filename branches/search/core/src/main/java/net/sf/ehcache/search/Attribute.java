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

import net.sf.ehcache.search.expression.Criteria;

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
        throw new AssertionError();
    }

    /**
     * Create a criteria where this attribute is 'in' (ie. contained within) the given collection of values.
     * 
     * @param values
     * @return criteria instance
     */
    public Criteria in(Collection<? extends T> values) {
        throw new AssertionError();
    }

    // public Criteria isNull() {
    // throw new AssertionError();
    // }
    //
    // public Criteria isNotNull() {
    // throw new AssertionError();
    // }

    /**
     * Create a criteria where this attribute is not equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria ne(T value) {
        throw new AssertionError();
    }

    /**
     * Create a criteria where this attribute is less than the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria lt(T value) {
        throw new AssertionError();
    }

    /**
     * Create a criteria where this attribute is less than or equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria le(T value) {
        throw new AssertionError();
    }

    /**
     * Create a criteria where this attribute is greater than the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria gt(T value) {
        throw new AssertionError();
    }

    /**
     * Create a criteria where this attribute is greater than or equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria ge(T value) {
        throw new AssertionError();
    }

    /**
     * Create a criteria where this attribute is equal to the given value
     * 
     * @param value
     * @return criteria instance
     */
    public Criteria eq(T value) {
        throw new AssertionError();
    }

    // // XXX: need to define exactly what regex format/subset we support
    // public Criteria like(String regex) {
    // throw new AssertionError();
    // }
    //
    // public Criteria ilike(String regex) {
    // throw new AssertionError();
    // }

}
