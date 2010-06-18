/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import java.util.List;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.TerracottaConfigConfiguration;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * Element representing the {@link Configuration}. This element does not have a parent and is always null.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class ConfigurationElement extends SimpleNodeElement {

    private final Configuration configuration;

    /**
     * Constructor accepting the {@link Configuration}. This element does not have a parent and is always null.
     * 
     * @param configuration
     */
    public ConfigurationElement(Configuration configuration) {
        super(null, "ehcache");
        this.configuration = configuration;
        init();
    }

    private void init() {
        if (configuration == null) {
            return;
        }
        // add the attributes
        addAttribute(new SimpleNodeAttribute("name", configuration.getName()).optional(true));
        addAttribute(new SimpleNodeAttribute("updateCheck", configuration.getUpdateCheck()).optional(true).defaultValue(
                String.valueOf(Configuration.DEFAULT_UPDATE_CHECK)));
        addAttribute(new SimpleNodeAttribute("monitoring", configuration.getMonitoring()).optional(true).defaultValue(
                Configuration.DEFAULT_MONITORING.name().toLowerCase()));
        addAttribute(new SimpleNodeAttribute("dynamicConfig", configuration.getDynamicConfig()).optional(true).defaultValue(
                String.valueOf(Configuration.DEFAULT_DYNAMIC_CONFIG)));

        // add the child elements
        DiskStoreConfiguration diskStoreConfiguration = configuration.getDiskStoreConfiguration();
        if (diskStoreConfiguration != null) {
            addChildElement(new DiskStoreConfigurationElement(this, diskStoreConfiguration));
        }
        FactoryConfiguration transactionManagerLookupConfiguration = configuration.getTransactionManagerLookupConfiguration();
        if (transactionManagerLookupConfiguration != null
                && !transactionManagerLookupConfiguration.equals(Configuration.DEFAULT_TRANSACTION_MANAGER_LOOKUP_CONFIG)) {
            addChildElement(new FactoryConfigurationElement(this, "transactionManagerLookup", transactionManagerLookupConfiguration));
        }
        FactoryConfiguration cacheManagerEventListenerFactoryConfiguration = configuration
                .getCacheManagerEventListenerFactoryConfiguration();
        if (cacheManagerEventListenerFactoryConfiguration != null) {
            addChildElement(new FactoryConfigurationElement(this, "cacheManagerEventListenerFactory",
                    cacheManagerEventListenerFactoryConfiguration));
        }
        List<FactoryConfiguration> cacheManagerPeerProviderFactoryConfiguration = configuration
                .getCacheManagerPeerProviderFactoryConfiguration();
        if (cacheManagerPeerProviderFactoryConfiguration != null) {
            addAllFactoryConfigsAsChildElements(this, "cacheManagerPeerProviderFactory", cacheManagerPeerProviderFactoryConfiguration);
        }
        List<FactoryConfiguration> cacheManagerPeerListenerFactoryConfigurations = configuration
                .getCacheManagerPeerListenerFactoryConfigurations();
        if (cacheManagerPeerListenerFactoryConfigurations != null && !cacheManagerPeerListenerFactoryConfigurations.isEmpty()) {
            addAllFactoryConfigsAsChildElements(this, "cacheManagerPeerListenerFactory", cacheManagerPeerListenerFactoryConfigurations);
        }
        addChildElement(new DefaultCacheConfigurationElement(this, configuration.getDefaultCacheConfiguration()));
        for (CacheConfiguration cacheConfiguration : configuration.getCacheConfigurations().values()) {
            addChildElement(new CacheConfigurationElement(this, cacheConfiguration));
        }
        TerracottaConfigConfiguration terracottaConfiguration = configuration.getTerracottaConfiguration();
        if (terracottaConfiguration != null) {
            addChildElement(new TerracottaConfigConfigurationElement(this, terracottaConfiguration));
        }
    }

}
