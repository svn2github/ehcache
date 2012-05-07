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

import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link net.sf.ehcache.config.generator.model.NodeElement} representing the {@link net.sf.ehcache.config.PinningConfiguration}
 *
 * @author Ludovic Orban
 *
 */
public class PinningConfigurationElement extends SimpleNodeElement {

    private final PinningConfiguration pinningConfiguration;

    /**
     * Constructor accepting the parent and the {@link net.sf.ehcache.config.TerracottaConfiguration}
     *
     * @param parent
     * @param pinningConfiguration
     */
    public PinningConfigurationElement(NodeElement parent, PinningConfiguration pinningConfiguration) {
        super(parent, "pinning");
        this.pinningConfiguration = pinningConfiguration;
        init();
    }

    private void init() {
        if (pinningConfiguration == null) {
            return;
        }

        addAttribute(new SimpleNodeAttribute("store", pinningConfiguration.getStore()).optional(false));
    }

}
