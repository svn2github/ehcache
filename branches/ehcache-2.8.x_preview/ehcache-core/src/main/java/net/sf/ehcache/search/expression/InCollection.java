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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeType;

/**
 * Criteria for inclusion in a given Collection (presumably a Set) of values
 *
 * @author teck
 */
public class InCollection extends BaseCriteria {

    private final String attributeName;
    private final Collection<?> values;
    private final AttributeType type;
    private final boolean empty;

    /**
     * Constructor
     *
     * @param attributeName attribute name
     * @param values
     */
    public InCollection(String attributeName, Collection<?> values) {
        if (attributeName == null || values == null) {
            throw new NullPointerException();
        }
        this.attributeName = attributeName;
        this.values = values;
        this.empty = values.isEmpty();

        if (!empty) {
            this.type = verifyCommonType();
        } else {
            this.type = null;
        }
    }

    /**
     * Return attributeName
     *
     * @return String attribute name
     */
    public String getAttributeName() {
        return this.attributeName;
    }

    /**
     * Return values.
     *
     * @return Collection<?> values
     */
    public Collection<?> values() {
        return Collections.unmodifiableCollection(this.values);
    }

    private AttributeType verifyCommonType() {
        if (values.isEmpty()) {
            throw new AssertionError();
        }

        AttributeType rv = null;
        for (Object value : values) {
            if (value == null) {
                throw new NullPointerException("null element in set");
            }

            AttributeType at = AttributeType.typeFor(attributeName, value);
            if (rv == null) {
                rv = at;
            } else if (at != rv) {
                throw new SearchException("Multiple types detected in collection: " + at + " and " + rv);
            }
        }
        return rv;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        if (empty) {
            return false;
        }

        Object attrValue = getExtractor(attributeName, attributeExtractors).attributeFor(e, attributeName);
        if (attrValue == null) {
            return false;
        } else {
            AttributeType attrType = AttributeType.typeFor(getAttributeName(), attrValue);
            if (!type.equals(attrType)) {
                throw new SearchException("Expecting attribute of type " + type.name() + " but was " + attrType.name());
            }
            if (AttributeType.STRING.equals(type)) {
                for (Object o : values) {
                    if (attrValue.toString().equalsIgnoreCase(o.toString())) {
                        return true;
                    }
                }
                return false;
            } else {
                return values.contains(attrValue);
            }
        }
    }

}
