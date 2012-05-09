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

import java.io.Serializable;

import net.sf.ehcache.Element;

/**
 * Used to extract a search attribute value for a given cache element.<br>
 * <br>
 * Instances must be {@link Serializable} in order to ensure identical
 * extractors are used in distributed caches
 *
 * @author teck
 */
public interface AttributeExtractor extends Serializable {

    /**
     * Extract the attribute value. The instance returned from this method must
     * be one of:
     * <ul>
     * <li>java.lang.Boolean
     * <li>java.lang.Byte
     * <li>java.lang.Character
     * <li>java.lang.Double
     * <li>java.lang.Float
     * <li>java.lang.Integer
     * <li>java.lang.Long
     * <li>java.lang.Short
     * <li>java.lang.String
     * <li>java.util.Date
     * <li>java.sql.Date
     * <li>java.lang.Enum
     * </ul>
     * <p/>
     * NOTE: null is a legal return here as well indicating that this attribute will not be available for the given element
     *
     * @param element the cache element to inspect
     * @param attributeName the name of the requested attribute
     * @return the attribute value
     * @throws AttributeExtractorException if the attribute cannot be found or extracted
     */
    Object attributeFor(Element element, String attributeName) throws AttributeExtractorException;
}
