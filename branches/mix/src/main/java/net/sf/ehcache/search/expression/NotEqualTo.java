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
 * Criteria for plain "not equals to" condition
 *
 * @author teck
 */
public class NotEqualTo extends EqualTo {

    /**
     * Constructor
     *
     * @param attributeName attribute name
     * @param value
     */
    public NotEqualTo(String attributeName, Object value) {
        super(attributeName, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        return !super.execute(e, attributeExtractors);
    }

}
