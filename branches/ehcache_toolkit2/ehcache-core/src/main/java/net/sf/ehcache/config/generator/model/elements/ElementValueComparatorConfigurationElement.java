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

import net.sf.ehcache.config.ElementValueComparatorConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link net.sf.ehcache.config.generator.model.NodeElement} representing the
 * {@link net.sf.ehcache.config.ElementValueComparatorConfiguration}
 *
 * @author Ludovic Orban
 *
 */
public class ElementValueComparatorConfigurationElement extends SimpleNodeElement {

    private final ElementValueComparatorConfiguration elementValueComparatorConfiguration;

    /**
     * Constructor accepting the parent and the {@link net.sf.ehcache.config.ElementValueComparatorConfiguration}
     *
     * @param parent
     * @param elementValueComparatorConfiguration
     */
    public ElementValueComparatorConfigurationElement(NodeElement parent,
            ElementValueComparatorConfiguration elementValueComparatorConfiguration) {
        super(parent, "elementValueComparator");
        this.elementValueComparatorConfiguration = elementValueComparatorConfiguration;
        init();
    }

    private void init() {
        if (elementValueComparatorConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("class", elementValueComparatorConfiguration.getClassName()).optional(false));
    }

}
