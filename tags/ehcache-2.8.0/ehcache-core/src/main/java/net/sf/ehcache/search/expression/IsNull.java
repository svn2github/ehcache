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
 * Tests whether the value of attribute with given name is null
 * @author vfunshte
 */
public class IsNull extends BaseCriteria {

    private final String attributeName;
    
    /**
     * @param attributeName
     */
    public IsNull(String attributeName) {
        this.attributeName = attributeName;
    }
    
    /**
     * Get attribute name.
     *
     * @return attribute name.
     */
    public String getAttributeName() {
        return attributeName;
    }


    @Override
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        return getExtractor(getAttributeName(), attributeExtractors).attributeFor(e, getAttributeName()) == null;
    }

}
