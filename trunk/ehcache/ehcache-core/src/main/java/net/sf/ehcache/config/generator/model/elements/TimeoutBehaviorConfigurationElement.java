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

import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link NodeElement} representing the {@link TimeoutBehaviorConfiguration}
 *
 * @author Abhishek Sanoujam
 *
 */
public class TimeoutBehaviorConfigurationElement extends SimpleNodeElement {

    private final TimeoutBehaviorConfiguration timeoutBehaviorConfiguration;

    /**
     * Constructor accepting the parent and the {@link TimeoutBehaviorConfiguration}
     *
     * @param parent
     * @param timeoutBehaviorConfiguration
     */
    public TimeoutBehaviorConfigurationElement(NodeElement parent, TimeoutBehaviorConfiguration timeoutBehaviorConfiguration) {
        super(parent, "timeoutBehavior");
        this.timeoutBehaviorConfiguration = timeoutBehaviorConfiguration;
        init();
    }

    private void init() {
        if (timeoutBehaviorConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("type", timeoutBehaviorConfiguration.getType()).optional(true).defaultValue(
                TimeoutBehaviorConfiguration.DEFAULT_VALUE));
        addAttribute(new SimpleNodeAttribute("properties", timeoutBehaviorConfiguration.getProperties()).optional(true).defaultValue(
                TimeoutBehaviorConfiguration.DEFAULT_PROPERTIES));
        addAttribute(new SimpleNodeAttribute("propertySeparator", timeoutBehaviorConfiguration.getPropertySeparator()).optional(true)
                .defaultValue(TimeoutBehaviorConfiguration.DEFAULT_PROPERTY_SEPARATOR));
    }
}
