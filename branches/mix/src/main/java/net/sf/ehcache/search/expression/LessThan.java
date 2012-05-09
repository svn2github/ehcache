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

/**
 * Less than criteria
 *
 * @author teck
 */
public class LessThan extends ComparableValue {

    private final Comparable comparableValue;

    /**
     * Constructor
     *
     * @param attributeName attribute name
     * @param value
     */
    public LessThan(String attributeName, Object value) {
        super(attributeName, value);
        this.comparableValue = (Comparable) value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeComparable(Comparable attributeValue) {
        return attributeValue.compareTo(comparableValue) < 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeComparableString(Comparable attributeValue) {
        return luceneStringCompare(attributeValue.toString(), comparableValue.toString()) < 0;
    }

    /**
     * Comparable value.
     *
     * @return value
     */
    public Comparable getComparableValue() {
        return comparableValue;
    }

}
