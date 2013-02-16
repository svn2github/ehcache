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

package net.sf.ehcache.config.generator.model.elements;

import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link NodeElement} representing a {@link Searchable} config element
 *
 * @author teck
 */
public class SearchableConfigurationElement extends SimpleNodeElement {

    private final Searchable searchable;

    /**
     * Constructor accepting the parent and the {@link Searchable}
     *
     * @param parent
     * @param searchable
     */
    public SearchableConfigurationElement(NodeElement parent, Searchable searchable) {
        super(parent, "searchable");

        if (searchable == null) {
            throw new NullPointerException();
        }

        this.searchable = searchable;
        init();
    }

    private void init() {
        for (SearchAttribute sa : searchable.getUserDefinedSearchAttributes().values()) {
            addChildElement(sa.asConfigElement(this));
        }

        addAttribute(new SimpleNodeAttribute("keys", searchable.keys()).optional(true).defaultValue(Searchable.KEYS_DEFAULT));
        addAttribute(new SimpleNodeAttribute("values", searchable.values()).optional(true).defaultValue(Searchable.VALUES_DEFAULT));
        addAttribute(new SimpleNodeAttribute("allowDynamicIndexing", searchable.isDynamicIndexingAllowed()).optional(true)
                .defaultValue(Searchable.DYNAMIC_INDEXING_DEFAULT));
    }

}
