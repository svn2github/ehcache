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

import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link NodeElement} representing the {@link TerracottaClientConfiguration}
 *
 * @author Abhishek Sanoujam
 *
 */
public class TerracottaConfigConfigurationElement extends SimpleNodeElement {

    private final TerracottaClientConfiguration tcConfigConfiguration;

    /**
     * Constructor accepting the parent and the {@link TerracottaClientConfiguration}
     *
     * @param parent
     * @param tcConfigConfiguration
     */
    public TerracottaConfigConfigurationElement(NodeElement parent, TerracottaClientConfiguration tcConfigConfiguration) {
        super(parent, "terracottaConfig");
        this.tcConfigConfiguration = tcConfigConfiguration;
        init();
    }

    private void init() {
        if (tcConfigConfiguration == null) {
            return;
        }
        if (tcConfigConfiguration.getUrl() != null) {
            addAttribute(new SimpleNodeAttribute("url", tcConfigConfiguration.getUrl()).optional(true));
        }
        addAttribute(new SimpleNodeAttribute("rejoin", tcConfigConfiguration.isRejoin()).optional(true).defaultValue(
                TerracottaClientConfiguration.DEFAULT_REJOIN_VALUE));

        if (tcConfigConfiguration.getOriginalEmbeddedConfig() != null) {
            addChildElement(new TCConfigElement(this, tcConfigConfiguration.getOriginalEmbeddedConfig()));
        }
    }

    /**
     * Element representing the "tc-config" element inside "terracottaConfig"
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class TCConfigElement extends SimpleNodeElement {

        /**
         * Constructor accepting the {@link TerracottaConfigConfigurationElement} parent and the inner string content
         *
         * @param parent
         * @param content
         */
        public TCConfigElement(TerracottaConfigConfigurationElement parent, String content) {
            super(parent, "tc-config");
            this.setInnerContent(content);
        }

    }
}
