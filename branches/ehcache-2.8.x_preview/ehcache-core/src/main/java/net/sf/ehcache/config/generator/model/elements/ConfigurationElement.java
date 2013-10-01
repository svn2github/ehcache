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

import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * Element representing the {@link Configuration}. This element does not have a parent and is always null.
 *
 * @author Abhishek Sanoujam
 *
 */
public class ConfigurationElement extends SimpleNodeElement {

    private final CacheManager cacheManager;
    private final Configuration configuration;

    /**
     * Constructor accepting the {@link Configuration}. This element does not have a parent and is always null.
     *
     * @param configuration
     */
    public ConfigurationElement(Configuration configuration) {
        super(null, "ehcache");
        this.cacheManager = null;
        this.configuration = configuration;
        init();
    }

    /**
     * Constructor accepting the {@link CacheManager}. This element does not have a parent and is always null.
     *
     * @param cacheManager
     */
    public ConfigurationElement(CacheManager cacheManager) {
        super(null, "ehcache");
        this.cacheManager = cacheManager;
        this.configuration = cacheManager.getConfiguration();
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
        addAttribute(new SimpleNodeAttribute("defaultTransactionTimeoutInSeconds", configuration.getDefaultTransactionTimeoutInSeconds())
                .optional(true).defaultValue(String.valueOf(Configuration.DEFAULT_TRANSACTION_TIMEOUT)));
        testAddMaxBytesLocalHeapAttribute();
        testAddMaxBytesLocalOffHeapAttribute();
        testAddMaxBytesLocalDiskAttribute();

        // add the child elements
        testAddDiskStoreElement();
        testAddSizeOfPolicyElement();
        testAddTransactionManagerLookupElement();
        testAddManagementRESTService();
        testAddCacheManagerEventListenerFactoryElement();
        testAddCacheManagerPeerProviderFactoryElement();
        testAddCacheManagerPeerListenerFactoryElement();

        addChildElement(new DefaultCacheConfigurationElement(this, configuration, configuration.getDefaultCacheConfiguration()));

        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                boolean decoratedCache = false;
                Ehcache cache = cacheManager.getCache(cacheName);
                if (cache == null) {
                    cache = cacheManager.getEhcache(cacheName);
                    decoratedCache = true;
                }
                CacheConfiguration config = decoratedCache ? cache.getCacheConfiguration().clone().name(cacheName) : cache.getCacheConfiguration();
                addChildElement(new CacheConfigurationElement(this, configuration, config));
            }
        } else {
            for (CacheConfiguration cacheConfiguration : configuration.getCacheConfigurations().values()) {
                addChildElement(new CacheConfigurationElement(this, configuration, cacheConfiguration));
            }
        }

        testAddTerracottaElement();
    }

    private void testAddMaxBytesLocalHeapAttribute() {
        if (configuration.getMaxBytesLocalHeap() > 0) {
            addAttribute(new SimpleNodeAttribute("maxBytesLocalHeap", configuration.getMaxBytesLocalHeapAsString())
                    .optional(true).defaultValue(String.valueOf(Configuration.DEFAULT_MAX_BYTES_ON_HEAP)));
        }
    }

    private void testAddMaxBytesLocalOffHeapAttribute() {
        if (configuration.getMaxBytesLocalOffHeap() > 0) {
            addAttribute(new SimpleNodeAttribute("maxBytesLocalOffHeap", configuration.getMaxBytesLocalOffHeapAsString())
                    .optional(true).defaultValue(String.valueOf(Configuration.DEFAULT_MAX_BYTES_OFF_HEAP)));
        }
    }

    private void testAddMaxBytesLocalDiskAttribute() {
        if (configuration.getMaxBytesLocalDisk() > 0) {
            addAttribute(new SimpleNodeAttribute("maxBytesLocalDisk", configuration.getMaxBytesLocalDiskAsString())
                    .optional(true).defaultValue(String.valueOf(Configuration.DEFAULT_MAX_BYTES_ON_DISK)));
        }
    }

    private void testAddDiskStoreElement() {
        DiskStoreConfiguration diskStoreConfiguration = configuration.getDiskStoreConfiguration();
        if (diskStoreConfiguration != null) {
            addChildElement(new DiskStoreConfigurationElement(this, diskStoreConfiguration));
        }
    }

    private void testAddSizeOfPolicyElement() {
        SizeOfPolicyConfiguration sizeOfPolicyConfiguration = configuration.getSizeOfPolicyConfiguration();
        if (sizeOfPolicyConfiguration != null &&
                !Configuration.DEFAULT_SIZEOF_POLICY_CONFIGURATION.equals(sizeOfPolicyConfiguration)) {
            addChildElement(new SizeOfPolicyConfigurationElement(this, sizeOfPolicyConfiguration));
        }
    }

    private void testAddTransactionManagerLookupElement() {
        FactoryConfiguration transactionManagerLookupConfiguration = configuration.getTransactionManagerLookupConfiguration();
        if (transactionManagerLookupConfiguration != null
                && !transactionManagerLookupConfiguration.equals(Configuration.DEFAULT_TRANSACTION_MANAGER_LOOKUP_CONFIG)) {
            addChildElement(new FactoryConfigurationElement(this, "transactionManagerLookup", transactionManagerLookupConfiguration));
        }
    }

    private void testAddManagementRESTService() {
        ManagementRESTServiceConfiguration managementRESTServiceConfiguration = configuration.getManagementRESTService();
        if (managementRESTServiceConfiguration != null) {
            addChildElement(new ManagementRESTServiceConfigurationElement(this, managementRESTServiceConfiguration));
        }
    }

    private void testAddCacheManagerEventListenerFactoryElement() {
        FactoryConfiguration cacheManagerEventListenerFactoryConfiguration = configuration
                .getCacheManagerEventListenerFactoryConfiguration();
        if (cacheManagerEventListenerFactoryConfiguration != null) {
            addChildElement(new FactoryConfigurationElement(this, "cacheManagerEventListenerFactory",
                    cacheManagerEventListenerFactoryConfiguration));
        }
    }

    private void testAddCacheManagerPeerProviderFactoryElement() {
        List<FactoryConfiguration> cacheManagerPeerProviderFactoryConfiguration = configuration
                .getCacheManagerPeerProviderFactoryConfiguration();
        if (cacheManagerPeerProviderFactoryConfiguration != null) {
            addAllFactoryConfigsAsChildElements(this, "cacheManagerPeerProviderFactory", cacheManagerPeerProviderFactoryConfiguration);
        }
    }

    private void testAddCacheManagerPeerListenerFactoryElement() {
        List<FactoryConfiguration> cacheManagerPeerListenerFactoryConfigurations = configuration
                .getCacheManagerPeerListenerFactoryConfigurations();
        if (cacheManagerPeerListenerFactoryConfigurations != null && !cacheManagerPeerListenerFactoryConfigurations.isEmpty()) {
            addAllFactoryConfigsAsChildElements(this, "cacheManagerPeerListenerFactory", cacheManagerPeerListenerFactoryConfigurations);
        }
    }

    private void testAddTerracottaElement() {
        TerracottaClientConfiguration terracottaConfiguration = configuration.getTerracottaConfiguration();
        if (terracottaConfiguration != null) {
            addChildElement(new TerracottaConfigConfigurationElement(this, terracottaConfiguration));
        }
    }
}
