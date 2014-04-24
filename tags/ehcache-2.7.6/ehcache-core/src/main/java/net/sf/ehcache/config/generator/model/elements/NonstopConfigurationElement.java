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

import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link NodeElement} representing the {@link NonstopConfiguration}
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopConfigurationElement extends SimpleNodeElement {

    private final NonstopConfiguration nonstopConfiguration;

    /**
     * Constructor accepting the parent and the {@link NonstopConfiguration}
     *
     * @param parent
     * @param nonstopConfiguration
     */
    public NonstopConfigurationElement(NodeElement parent, NonstopConfiguration nonstopConfiguration) {
        super(parent, "nonstop");
        this.nonstopConfiguration = nonstopConfiguration;
        init();
    }

    private void init() {
        if (nonstopConfiguration == null) {
            return;
        }
        if (nonstopConfiguration.getTimeoutBehavior() != null && !isDefault(nonstopConfiguration.getTimeoutBehavior())) {
            addChildElement(new TimeoutBehaviorConfigurationElement(this, nonstopConfiguration.getTimeoutBehavior()));
        }
        addAttribute(new SimpleNodeAttribute("enabled", nonstopConfiguration.isEnabled()).optional(true).defaultValue(
                NonstopConfiguration.DEFAULT_ENABLED));
        addAttribute(new SimpleNodeAttribute("immediateTimeout", nonstopConfiguration.isImmediateTimeout()).optional(true).defaultValue(
                NonstopConfiguration.DEFAULT_IMMEDIATE_TIMEOUT));
        addAttribute(new SimpleNodeAttribute("timeoutMillis", nonstopConfiguration.getTimeoutMillis()).optional(true).defaultValue(
                NonstopConfiguration.DEFAULT_TIMEOUT_MILLIS));
        addAttribute(new SimpleNodeAttribute("searchTimeoutMillis", nonstopConfiguration.getSearchTimeoutMillis()).optional(true).defaultValue(
                NonstopConfiguration.DEFAULT_SEARCH_TIMEOUT_MILLIS));
    }

    private boolean isDefault(TimeoutBehaviorConfiguration timeoutBehavior) {
        boolean rv = true;
        if (!NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getType().equals(timeoutBehavior.getType())) {
            rv = false;
        }
        if (!NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getProperties().equals(timeoutBehavior.getProperties())) {
            rv = false;
        }
        if (!NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getPropertySeparator().equals(timeoutBehavior.getPropertySeparator())) {
            rv = false;
        }
        return rv;
    }
}
