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

import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link NodeElement} representing the {@link FactoryConfiguration}
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class FactoryConfigurationElement extends SimpleNodeElement {

    private final FactoryConfiguration<? extends FactoryConfiguration> factoryConfiguration;

    /**
     * Constructor accepting the parent, the name and the {@link FactoryConfiguration}
     * 
     * @param parent
     * @param name
     * @param factoryConfiguration
     */
    public FactoryConfigurationElement(NodeElement parent, String name,
            FactoryConfiguration<? extends FactoryConfiguration> factoryConfiguration) {
        super(parent, name);
        this.factoryConfiguration = factoryConfiguration;
        init();
    }

    private void init() {
        if (factoryConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("class", factoryConfiguration.getFullyQualifiedClassPath()).optional(false));
        addAttribute(new SimpleNodeAttribute("properties", factoryConfiguration.getProperties()).optional(true));
        addAttribute(new SimpleNodeAttribute("propertySeparator", factoryConfiguration.getPropertySeparator()).optional(true));
    }

    /**
     * Returns the {@link FactoryConfiguration} associated with this element
     * 
     * @return the {@link FactoryConfiguration} associated with this element
     */
    public FactoryConfiguration<? extends FactoryConfiguration> getFactoryConfiguration() {
        return factoryConfiguration;
    }

}
