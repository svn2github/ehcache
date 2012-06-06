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

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;


/**
 * {@link NodeElement} representing the {@link CacheConfiguration} of the "defaultCache"
 *
 * @author Abhishek Sanoujam
 *
 */
public class DefaultCacheConfigurationElement extends SimpleNodeElement {

    private final Configuration configuration;
    private final CacheConfiguration cacheConfiguration;

    /**
     * Constructor accepting the parent and the {@link CacheConfiguration}
     *
     * @param parent
     * @param cacheConfiguration
     */
    public DefaultCacheConfigurationElement(NodeElement parent, Configuration configuration, CacheConfiguration cacheConfiguration) {
        super(parent, "defaultCache");
        this.configuration = configuration;
        this.cacheConfiguration = cacheConfiguration;
        init();
        this.optional = false;
    }

    private void init() {
        if (cacheConfiguration == null) {
            return;
        }
        CacheConfigurationElement.addCommonAttributesWithDefaultCache(this, configuration, cacheConfiguration);

        CacheConfigurationElement.addCommonChildElementsWithDefaultCache(this, cacheConfiguration);
    }

}
