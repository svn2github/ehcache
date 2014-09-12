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

import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * Element representing the {@link net.sf.ehcache.config.PersistenceConfiguration}
 *
 * @author Chris Dennis
 *
 */
public class PersistenceConfigurationElement extends SimpleNodeElement {
    private final PersistenceConfiguration persistenceConfiguration;

    /**
     * Construtor accepting the parent and the {@link net.sf.ehcache.config.PersistenceConfiguration}
     *
     * @param parent
     * @param persistenceConfiguration
     */
    public PersistenceConfigurationElement(ConfigurationElement parent, PersistenceConfiguration persistenceConfiguration) {
        super(parent, "persistence");
        this.persistenceConfiguration = persistenceConfiguration;
        init();
    }

    /**
     * Construtor accepting the element and the {@link net.sf.ehcache.config.PersistenceConfiguration}
     *
     * @param element
     * @param persistenceConfiguration
     */
    public PersistenceConfigurationElement(NodeElement element, PersistenceConfiguration persistenceConfiguration) {
        super(element, "persistence");
        this.persistenceConfiguration = persistenceConfiguration;
        init();
    }

    private void init() {
        if (persistenceConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("strategy", persistenceConfiguration.getStrategy()));
        addAttribute(new SimpleNodeAttribute("synchronousWrites", persistenceConfiguration.getSynchronousWrites()).optional(true)
                .defaultValue(PersistenceConfiguration.DEFAULT_SYNCHRONOUS_WRITES));
    }

}
