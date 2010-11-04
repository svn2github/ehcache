/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.CacheException;

/**
 * Search configuration for a Cache
 *
 * @author teck
 */
public class Searchable {

    /**
     * The defined search attributes (if any) indexed by name
     */
    private final Map<String, SearchAttribute> searchAttributes = new HashMap<String, SearchAttribute>();
    private boolean frozen;

    /**
     * Add the given search attribute
     *
     * @throws InvalidConfigurationException if an attribute already exists for the same name
     * @param searchAttribute to add
     */
    public void addSearchAttribute(SearchAttribute searchAttribute) throws InvalidConfigurationException {
        checkDynamicChange();

        String attributeName = searchAttribute.getName();

        if (attributeName == null) {
            throw new InvalidConfigurationException("Search attribute has null name");
        }

        if (searchAttributes.containsKey(attributeName)) {
            throw new InvalidConfigurationException("Repeated searchAttribute name: " + attributeName);
        }

        searchAttributes.put(attributeName, searchAttribute);
    }

    private void checkDynamicChange() {
        if (frozen) {
            throw new CacheException("Dynamic configuration changes are disabled for this cache");
        }
    }

    /**
     * Get the defined search attributes indexed by attribute name
     *
     * @return search attributes
     */
    public Map<String, SearchAttribute> getSearchAttributes() {
        return Collections.unmodifiableMap(searchAttributes);
    }

    /**
     * Add a search attribute
     *
     * @param searchAttribute attribute to add
     * @return this
     */
    public Searchable searchAttribute(SearchAttribute searchAttribute) {
        addSearchAttribute(searchAttribute);
        return this;
    }

    /**
     * Freeze this configuration. Any subsequent changes will throw a CacheException
     */
    public void freezeConfiguration() {
        frozen = true;
    }

    /**
     * Get the defined search attributes indexed by attribute name *excluding* any search attributes that are automatically/implicitly
     * defined (eg. key and value attributes)
     *
     * @return search attributes
     */
    public Map<String, SearchAttribute> getUserDefinedSearchAttributes() {
        // XXX: implement this method correctly!

        return getSearchAttributes();
    }
}
