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

import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * Element representing the {@link net.sf.ehcache.config.SizeOfPolicyConfiguration}
 *
 * @author Ludovic Orban
 *
 */
public class SizeOfPolicyConfigurationElement extends SimpleNodeElement {
    private final SizeOfPolicyConfiguration sizeOfPolicyConfiguration;

    /**
     * Construtor accepting the parent and the {@link net.sf.ehcache.config.SizeOfPolicyConfiguration}
     *
     * @param parent
     * @param sizeOfPolicyConfiguration
     */
    public SizeOfPolicyConfigurationElement(ConfigurationElement parent, SizeOfPolicyConfiguration sizeOfPolicyConfiguration) {
        super(parent, "sizeOfPolicy");
        this.sizeOfPolicyConfiguration = sizeOfPolicyConfiguration;
        init();
    }

    /**
     * Construtor accepting the element and the {@link net.sf.ehcache.config.SizeOfPolicyConfiguration}
     *
     * @param element
     * @param sizeOfPolicyConfiguration
     */
    public SizeOfPolicyConfigurationElement(NodeElement element, SizeOfPolicyConfiguration sizeOfPolicyConfiguration) {
        super(element, "sizeOfPolicy");
        this.sizeOfPolicyConfiguration = sizeOfPolicyConfiguration;
        init();
    }

    private void init() {
        if (sizeOfPolicyConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("maxDepth", sizeOfPolicyConfiguration.getMaxDepth())
            .optional(true).defaultValue(SizeOfPolicyConfiguration.DEFAULT_MAX_SIZEOF_DEPTH));
        addAttribute(new SimpleNodeAttribute("maxDepthExceededBehavior", sizeOfPolicyConfiguration.getMaxDepthExceededBehavior())
            .optional(true).defaultValue(SizeOfPolicyConfiguration.DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR));
    }

}
