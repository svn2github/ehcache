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

package net.sf.ehcache.search.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;

/**
 * Utility class for verifying dynamically extracted search attribute against "static" search configuration
 * @author vfunshte
 */
public class DynamicSearchChecker {

    /**
     * Extracts dynamically indexed search attributes from cache element using provided extractor,
     * validating against reserved set of attribute names (provided by Ehcache search config)
     *
     * @param e cache element for which to get dynamically extracted attribute values
     * @param reservedAttrs disallowed attribute names
     * @param extractor dynamic attributes extractor
     * @return map of dynamically extracted search attribute names to their values. If passed in extractor is null, map will be empty.
     * @throws SearchException
     */
    public static Map<String, ? extends Object> getSearchAttributes(Element e, Set<String> reservedAttrs,
            DynamicAttributesExtractor extractor) throws SearchException {
        if (extractor == null)  {
            return Collections.emptyMap();
        }
        Map<String, ? extends Object> dynamic = extractor.attributesFor(e);
        boolean error = new HashSet<String>(reservedAttrs).removeAll(dynamic.keySet());

        if (error) {
            throw new SearchException("Dynamic extractor produced attributes already used in static search config");
        }
        return dynamic;
    }
}
