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
import net.sf.ehcache.search.attribute.AttributeExtractor;

/**
 * Criteria interface defines a boolean function that computes a search match result
 *
 * @author teck
 */
public interface Criteria {

    /**
     * Test this criteria against a cache element
     *
     * @param element cache element
     * @param attributeExtractors map of attribute extractors to attribute value names
     * @return true if the criteria matches this element
     */
    boolean execute(Element element, Map<String, AttributeExtractor> attributeExtractors);

    /**
     * Produce a criteria that is the boolean "and" of this and the given other criteria
     *
     * @param other
     * @return and criteria
     */
    Criteria and(Criteria other);

    /**
     * Produce a criteria that is the boolean "or" of this and the given other criteria
     *
     * @param other
     * @return or criteria
     */
    Criteria or(Criteria other);

    /**
     * Produce a criteria that is the boolean "not" of this
     *
     * @return not criteria
     */
    Criteria not();
}
