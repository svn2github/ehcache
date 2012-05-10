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

package net.sf.ehcache.search.attribute;

import net.sf.ehcache.Element;

/**
 * Attempt to use the element value object itself as a search attribute. If the value is not a legal type it is omitted
 *
 * @author teck
 */
public class ValueObjectAttributeExtractor implements AttributeExtractor {

    /**
     * {@inheritDoc}
     */
    public Object attributeFor(Element element, String attributeName) throws AttributeExtractorException {
        Object value = element.getObjectValue();

        if (AttributeType.isSupportedType(value)) {
            return value;
        }

        // not a supported type return null to not define anything in the index
        return null;
    }

}
