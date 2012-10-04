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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeType;

/**
 * Abstract base class for criteria involving {@link java.lang.Comparable} values
 *
 * @author teck
 */
public abstract class ComparableValue extends BaseCriteria {

    private static final Comparator<String> LUCENE_STRING_COMPARATOR = new LuceneCaseAgnosticStringComparator();

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
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        Object attrValue = getExtractor(getAttributeName(), attributeExtractors).attributeFor(e, getAttributeName());
        if (attrValue == null) {
            return false;
        } else {
            AttributeType attrType = AttributeType.typeFor(getAttributeName(), attrValue);
            if (!getType().equals(attrType)) {
                throw new SearchException("Expecting attribute of type " + getType().name() + " but was " + attrType.name());
            }

            if (getType().equals(AttributeType.STRING)) {
                return executeComparableString((Comparable) attrValue);
            } else {
                return executeComparable((Comparable) attrValue);
            }
        }
    }

    /**
     * Execute this criteria for the given {@link Comparable} attribute value
     *
     * @param attributeValue Comparable attribute value
     * @return true if criteria is met
     */
    protected abstract boolean executeComparable(Comparable attributeValue);

    /**
     * Execute this criteria for the given {@link Comparable} strin type attribute value
     *
     * @param attributeValue Comparable attribute value
     * @return true if criteria is met
     */
    protected abstract boolean executeComparableString(Comparable attributeValue);

    /**
     * Perform a Lucene compatible case insensitive string comparison.
     *
     * @param s1 first string
     * @param s2 second string
     * @return the comparison result
     */
    protected static int luceneStringCompare(String s1, String s2) {
        return LUCENE_STRING_COMPARATOR.compare(s1, s2);
    }

    /**
     * A Lucene compatible case insensitive string comparator.
     */
    private static class LuceneCaseAgnosticStringComparator implements Comparator<String>, Serializable {

      public int compare(String s1, String s2) {
        int n1 = s1.length();
        int n2 = s2.length();
        for (int i = 0; i < n1 && i < n2; i++) {
          char c1 = s1.charAt(i);
          char c2 = s2.charAt(i);

          if (c1 != c2) {
            c1 = Character.toLowerCase(c1);
            c2 = Character.toLowerCase(c2);
            if (c1 != c2) { return c1 - c2; }
          }
        }
        return n1 - n2;
      }
    }
}
