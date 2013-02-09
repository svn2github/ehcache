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
 * Inverse of {@link ILike}
 * @author vfunshte
 *
 */
public class NotILike extends BaseCriteria {

    private final ILike src;

    /**
     * Constructor
     * @param attributeName
     * @param regex
     */
    public NotILike(String attributeName, String regex) {
        src = new ILike(attributeName, regex);
    }

    @Override
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        return !src.execute(e, attributeExtractors);
    }

    /**
     * Return attribute name.
     *
     * @return String attribute name
     */
    public String getAttributeName() {
        return src.getAttributeName();
    }

    /**
     * Return regex string.
     *
     * @return String regex.
     */
    public String getRegex() {
        return src.getRegex();
    }
}
