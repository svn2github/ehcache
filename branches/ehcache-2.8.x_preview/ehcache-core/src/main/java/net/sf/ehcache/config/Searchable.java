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

package net.sf.ehcache.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.attribute.KeyObjectAttributeExtractor;
import net.sf.ehcache.search.attribute.ValueObjectAttributeExtractor;

/**
 * Search configuration for a Cache
 *
 * @author teck
 */
public class Searchable {

    /**
     * Default for auto-searchable keys
     */
    public static final boolean KEYS_DEFAULT = true;

    /**
     * Default for auto-searchable values
     */
    public static final boolean VALUES_DEFAULT = true;

    /**
     * Default for allowing dynamic indexing
     */
    public static final boolean DYNAMIC_INDEXING_DEFAULT = false;

    /**
     * The defined search attributes (if any) indexed by name
     */
    private final Map<String, SearchAttribute> searchAttributes = new HashMap<String, SearchAttribute>();
    private boolean frozen;
    private boolean keys;
    private boolean values;
    private boolean allowDynamicIndexing = DYNAMIC_INDEXING_DEFAULT;

    /**
     * Constructor
     */
    public Searchable() {
        setKeys(KEYS_DEFAULT);
        setValues(VALUES_DEFAULT);
    }

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

        disallowBuiltins(attributeName);

        if (searchAttributes.containsKey(attributeName)) {
            throw new InvalidConfigurationException("Repeated searchAttribute name: " + attributeName);
        }

        searchAttributes.put(attributeName, searchAttribute);
    }

    private void disallowBuiltins(String attributeName) {
        if (Query.KEY.getAttributeName().equals(attributeName) || Query.VALUE.getAttributeName().equals(attributeName)) {
            throw new InvalidConfigurationException("\"" + attributeName + "\" is a reserved attribute name");
        }
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
        Map<String, SearchAttribute> copy = new HashMap<String, SearchAttribute>(searchAttributes);
        copy.remove(Query.KEY.getAttributeName());
        copy.remove(Query.VALUE.getAttributeName());
        return copy;
    }

    /**
     * Are keys searchable?
     *
     * @return true if keys are searchable
     */
    public boolean keys() {
        return keys;
    }

    /**
     * Are values searchable?
     *
     * @return true if values are searchable
     */
    public boolean values() {
        return values;
    }

    /**
     * Is dynamic indexing allowed?
     * @return
     */
    public boolean isDynamicIndexingAllowed() {
        return allowDynamicIndexing;
    }

    /**
     * Toggle searchable values
     *
     * @param b
     */
    public void values(boolean b) {
        setValues(b);
    }

    /**
     * Toggle searchable keys
     *
     * @param b
     */
    public void keys(boolean b) {
        setKeys(b);
    }

    /**
     * Toggle searchable keys
     *
     * @param keys
     */
    public void setKeys(boolean keys) {
        checkDynamicChange();
        this.keys = keys;
        if (!keys) {
            searchAttributes.remove(Query.KEY.getAttributeName());
        } else {
            String keyAttr = Query.KEY.getAttributeName();
            searchAttributes.put(keyAttr, new SearchAttribute().name(keyAttr).className(KeyObjectAttributeExtractor.class.getName()));
        }
    }

    /**
     * Toggle searchable values
     *
     * @param values
     */
    public void setValues(boolean values) {
        checkDynamicChange();
        this.values = values;
        if (!values) {
            searchAttributes.remove(Query.VALUE.getAttributeName());
        } else {
            String valueAttr = Query.VALUE.getAttributeName();
            searchAttributes.put(valueAttr, new SearchAttribute().name(valueAttr).className(ValueObjectAttributeExtractor.class.getName()));
        }
    }

    /**
     * Allow or disallow dynamic search attribute extraction
     * @param allow
     */
    public void setAllowDynamicIndexing(boolean allow) {
        checkDynamicChange();
        this.allowDynamicIndexing = allow;
    }

    /**
     * Allow or disallow dynamic search attribute extraction
     * @param allow
     */
    public void allowDynamicIndexing(boolean allow) {
        setAllowDynamicIndexing(allow);
    }
}
