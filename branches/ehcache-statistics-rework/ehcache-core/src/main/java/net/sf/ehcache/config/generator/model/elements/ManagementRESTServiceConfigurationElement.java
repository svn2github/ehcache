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

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;
import net.sf.ehcache.statistics.StatisticsGateway;

/**
 * Element representing the {@link net.sf.ehcache.config.ManagementRESTServiceConfiguration}
 *
 * @author Ludovic Orban
 *
 */
public class ManagementRESTServiceConfigurationElement extends SimpleNodeElement {
    private final ManagementRESTServiceConfiguration managementRESTServiceConfiguration;

    /**
     * Construtor accepting the parent and the {@link net.sf.ehcache.config.ManagementRESTServiceConfiguration}
     *
     * @param parent
     * @param cfg
     */
    public ManagementRESTServiceConfigurationElement(ConfigurationElement parent, ManagementRESTServiceConfiguration cfg) {
        super(parent, "managementRESTService");
        this.managementRESTServiceConfiguration = cfg;
        init();
    }

    /**
     * Construtor accepting the element and the {@link net.sf.ehcache.config.ManagementRESTServiceConfiguration}
     *
     * @param element
     * @param cfg
     */
    public ManagementRESTServiceConfigurationElement(NodeElement element, ManagementRESTServiceConfiguration cfg) {
        super(element, "managementRESTService");
        this.managementRESTServiceConfiguration = cfg;
        init();
    }

    private void init() {
        if (managementRESTServiceConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("enabled", managementRESTServiceConfiguration.isEnabled()).defaultValue(false));
        addAttribute(new SimpleNodeAttribute("bind", managementRESTServiceConfiguration.getBind())
            .defaultValue(ManagementRESTServiceConfiguration.DEFAULT_BIND));
        addAttribute(new SimpleNodeAttribute("securityServiceLocation", managementRESTServiceConfiguration
            .getSecurityServiceLocation()).optional(true));
        addAttribute(new SimpleNodeAttribute("securityServiceTimeout", managementRESTServiceConfiguration
            .getSecurityServiceTimeout()).optional(true)
            .defaultValue(ManagementRESTServiceConfiguration.DEFAULT_SECURITY_SVC_TIMEOUT));
        addAttribute(new SimpleNodeAttribute("sslEnabled", managementRESTServiceConfiguration.isSslEnabled())
            .optional(true).defaultValue(false));
        addAttribute(new SimpleNodeAttribute("needClientAuth", managementRESTServiceConfiguration.isNeedClientAuth())
            .optional(true).defaultValue(false));
        addAttribute(new SimpleNodeAttribute("sampleHistorySize", managementRESTServiceConfiguration.getSampleHistorySize())
            .optional(true).defaultValue(StatisticsGateway.DEFAULT_HISTORY_SIZE));
        addAttribute(new SimpleNodeAttribute("sampleIntervalSeconds", managementRESTServiceConfiguration
            .getSampleIntervalSeconds()).optional(true).defaultValue(StatisticsGateway.DEFAULT_INTERVAL_SECS));
        addAttribute(new SimpleNodeAttribute("sampleSearchIntervalSeconds", managementRESTServiceConfiguration
            .getSampleSearchIntervalSeconds()).optional(true).defaultValue(StatisticsGateway.DEFAULT_SEARCH_INTERVAL_SECS));
    }
}
