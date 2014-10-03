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

import java.util.Map;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeType;

/**
 * A comparison operator meaning Java "equals to" condition
 *
 * @author teck
 */
public class EqualTo extends BaseCriteria {

    private final Object value;
    private final String attributeName;
    private final AttributeType type;

    /**
     * Constructor
     *
     * @param attributeName attribute name
     * @param value
     */
    public EqualTo(String attributeName, Object value) {
        if (value == null || attributeName == null) {
            throw new NullPointerException();
        }

        this.attributeName = attributeName;
        this.value = value;

        this.type = AttributeType.typeFor(attributeName, value);
    }

    /**
     * Get attribute value.
     *
     * @return attribute value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get attribute name.
     *
     * @return attribute name.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Get attribute type.
     *
     * @return attribute type.
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        Object attributeValue = getExtractor(getAttributeName(), attributeExtractors).attributeFor(e, getAttributeName());
        if (attributeValue == null) {
            return false;
        } else {
            AttributeType attrType = AttributeType.typeFor(getAttributeName(), attributeValue);
            if (!getType().equals(attrType)) {
                throw new SearchException("Expecting attribute of type " + getType().name() + " but was " + attrType.name());
            }

            if (getType().equals(AttributeType.STRING)) {
                return ((String) this.value).equalsIgnoreCase((String) attributeValue);
            } else {
                return this.value.equals(attributeValue);
            }
        }
    }
}
