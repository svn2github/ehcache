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

package net.sf.ehcache.search.expression;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeType;
import net.sf.ehcache.store.ElementAttributeValues;

/**
 * Abstract base class for criteria involving {@link java.lang.Comparable} values
 *
 * @author teck
 */
public abstract class ComparableValue extends BaseCriteria {

    private final String attributeName;
    private final AttributeType type;

    /**
     * Constructor
     *
     * @param attributeName attribute name
     * @param value         comparable value (used to infer type)
     */
    public ComparableValue(String attributeName, Object value) {
        this(attributeName, AttributeType.typeFor(attributeName, value));
    }

    /**
     * Constructor
     *
     * @param attributeName attribute name
     * @param type          the expeceted type for values evaluated by this criteria
     */
    public ComparableValue(String attributeName, AttributeType type) {
        this.attributeName = attributeName;
        this.type = type;

        if (!this.type.isComparable()) {
            throw new SearchException("Illegal (non-comparable) type for comparsion (" + type + ")");
        }
    }

    /**
     * Attribute name.
     *
     * @return name
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Attribute type.
     *
     * @return type
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(Element e, ElementAttributeValues attributeValues) {
        Object attrValue = attributeValues.getAttributeValue(attributeName, type);
        if (attrValue == null) {
            return false;
        }

        return executeComparable((Comparable) attrValue);
    }

    /**
     * Execute this criteria for the given {@link Comparable} attribute value
     *
     * @param attributeValue Comparable attribute value
     * @return true if criteria is met
     */
    protected abstract boolean executeComparable(Comparable attributeValue);

}
