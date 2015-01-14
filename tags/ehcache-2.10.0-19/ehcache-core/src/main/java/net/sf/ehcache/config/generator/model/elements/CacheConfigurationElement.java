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
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.CopyStrategyConfiguration;
import net.sf.ehcache.config.ElementValueComparatorConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;
import net.sf.ehcache.store.DefaultElementValueComparator;

/**
 * Element representing the {@link CacheConfiguration}
 *
 * @author Abhishek Sanoujam
 *
 */
public class CacheConfigurationElement extends SimpleNodeElement {

    private final Configuration configuration;
    private final CacheConfiguration cacheConfiguration;

    /**
     * Constructor accepting the parent and the {@link CacheConfiguration}
     *
     * @param parent
     * @param cacheConfiguration
     */
    public CacheConfigurationElement(NodeElement parent, Configuration configuration,  CacheConfiguration cacheConfiguration) {
        super(parent, "cache");
        this.configuration = configuration;
        this.cacheConfiguration = cacheConfiguration;
        init();
    }

    private void init() {
        if (cacheConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("name", cacheConfiguration.getName()).optional(false));
        addCommonAttributesWithDefaultCache(this, configuration, cacheConfiguration);
        addAttribute(new SimpleNodeAttribute("logging", cacheConfiguration.getLogging()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_LOGGING));

        addCommonChildElementsWithDefaultCache(this, cacheConfiguration);
        if (cacheConfiguration.getMaxBytesLocalHeap() > 0 || cacheConfiguration.isMaxBytesLocalHeapPercentageSet()) {
            addAttribute(new SimpleNodeAttribute("maxBytesLocalHeap", cacheConfiguration.getMaxBytesLocalHeapAsString())
                .optional(true).defaultValue(String.valueOf(CacheConfiguration.DEFAULT_MAX_BYTES_ON_HEAP)));
        }
        if (cacheConfiguration.getMaxBytesLocalOffHeap() > 0 || cacheConfiguration.isMaxBytesLocalOffHeapPercentageSet()) {
            addAttribute(new SimpleNodeAttribute("maxBytesLocalOffHeap", cacheConfiguration.getMaxBytesLocalOffHeapAsString())
                    .optional(true).defaultValue(String.valueOf(CacheConfiguration.DEFAULT_MAX_BYTES_OFF_HEAP)));
        }
        if (!cacheConfiguration.isTerracottaClustered() &&
             (cacheConfiguration.getMaxBytesLocalDisk() > 0 || cacheConfiguration.isMaxBytesLocalDiskPercentageSet())) {
            addAttribute(new SimpleNodeAttribute("maxBytesLocalDisk", cacheConfiguration.getMaxBytesLocalDiskAsString())
                .optional(true).defaultValue(String.valueOf(CacheConfiguration.DEFAULT_MAX_BYTES_ON_DISK)));
        }
    }

    /**
     * Adds all attributes which are common with the "defaultCache" element in ehcache.xsd
     *
     * @param element
     * @param cacheConfiguration
     */
    public static void addCommonAttributesWithDefaultCache(NodeElement element, Configuration configuration, CacheConfiguration cacheConfiguration) {
        element.addAttribute(new SimpleNodeAttribute("eternal", cacheConfiguration.isEternal()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_ETERNAL_VALUE));
        if (!(cacheConfiguration.getMaxBytesLocalHeap() > 0 || configuration.getMaxBytesLocalHeap() > 0)) {
            element.addAttribute(new SimpleNodeAttribute("maxEntriesLocalHeap", cacheConfiguration.getMaxEntriesLocalHeap()).optional(false));
        }
        element.addAttribute(new SimpleNodeAttribute("clearOnFlush", cacheConfiguration.isClearOnFlush()).optional(true).defaultValue(
                String.valueOf(CacheConfiguration.DEFAULT_CLEAR_ON_FLUSH)));
        element.addAttribute(new SimpleNodeAttribute("diskAccessStripes", cacheConfiguration.getDiskAccessStripes()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_DISK_ACCESS_STRIPES));
        element.addAttribute(new SimpleNodeAttribute("diskSpoolBufferSizeMB", cacheConfiguration.getDiskSpoolBufferSizeMB()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_SPOOL_BUFFER_SIZE));
        element
                .addAttribute(new SimpleNodeAttribute("diskExpiryThreadIntervalSeconds", cacheConfiguration
                        .getDiskExpiryThreadIntervalSeconds()).optional(true).defaultValue(
                        CacheConfiguration.DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS));
        element.addAttribute(new SimpleNodeAttribute("copyOnWrite", cacheConfiguration.isCopyOnWrite()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_COPY_ON_WRITE));
        element.addAttribute(new SimpleNodeAttribute("copyOnRead", cacheConfiguration.isCopyOnRead()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_COPY_ON_READ));
        element.addAttribute(new SimpleNodeAttribute("timeToIdleSeconds", cacheConfiguration.getTimeToIdleSeconds()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_TTI));
        element.addAttribute(new SimpleNodeAttribute("timeToLiveSeconds", cacheConfiguration.getTimeToLiveSeconds()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_TTL));
        if (cacheConfiguration.isTerracottaClustered()) {
            element.addAttribute(new SimpleNodeAttribute("maxEntriesInCache", cacheConfiguration.getMaxEntriesInCache()).optional(true)
                    .defaultValue(CacheConfiguration.DEFAULT_MAX_ENTRIES_IN_CACHE));
        }
        if (!cacheConfiguration.isTerracottaClustered() && cacheConfiguration.getMaxEntriesLocalDisk() > 0) {
            element.addAttribute(new SimpleNodeAttribute("maxEntriesLocalDisk", cacheConfiguration.getMaxEntriesLocalDisk()).optional(true)
                    .defaultValue(CacheConfiguration.DEFAULT_MAX_ELEMENTS_ON_DISK));
        }
        element.addAttribute(new SimpleNodeAttribute("overflowToOffHeap", cacheConfiguration.isOverflowToOffHeap()).optional(true)
                .defaultValue(false));
        element.addAttribute(new SimpleNodeAttribute("cacheLoaderTimeoutMillis", cacheConfiguration.getCacheLoaderTimeoutMillis())
                .optional(true).defaultValue(0L));
        element.addAttribute(new SimpleNodeAttribute("transactionalMode", cacheConfiguration.getTransactionalMode()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_TRANSACTIONAL_MODE));
        element.addAttribute(new SimpleNodeAttribute("memoryStoreEvictionPolicy", cacheConfiguration.getMemoryStoreEvictionPolicy()
                .toString().toUpperCase()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_MEMORY_STORE_EVICTION_POLICY.toString().toUpperCase()));
        if (cacheConfiguration.isOverflowToDisk() && cacheConfiguration.isDiskPersistent()) {
            element.addAttribute(new SimpleNodeAttribute("diskPersistent", "true"));
            element.addAttribute(new SimpleNodeAttribute("overflowToDisk", "true"));
        }
    }

    /**
     * Adds all common child elements with the "defaultCache" element in ehcache.xsd
     *
     * @param element
     * @param cacheConfiguration
     */
    public static void addCommonChildElementsWithDefaultCache(NodeElement element, CacheConfiguration cacheConfiguration) {
        for (FactoryConfigurationElement child : getAllFactoryElements(element, "cacheEventListenerFactory", cacheConfiguration
                .getCacheEventListenerConfigurations())) {
            CacheEventListenerFactoryConfiguration factoryConfiguration = (CacheEventListenerFactoryConfiguration) child
                    .getFactoryConfiguration();
            child.addAttribute(new SimpleNodeAttribute("listenFor", factoryConfiguration.getListenFor()));
            element.addChildElement(child);
        }
        addAllFactoryConfigsAsChildElements(element, "cacheExtensionFactory", cacheConfiguration.getCacheExtensionConfigurations());
        addAllFactoryConfigsAsChildElements(element, "cacheLoaderFactory", cacheConfiguration.getCacheLoaderConfigurations());
        addBootstrapCacheLoaderFactoryConfigurationElement(element, cacheConfiguration);
        addCacheExceptionHandlerFactoryConfigurationElement(element, cacheConfiguration);
        addSizeOfPolicyConfigurationElement(element, cacheConfiguration);
        if (!cacheConfiguration.isOverflowToDisk() || !cacheConfiguration.isDiskPersistent()) {
            addPersistenceConfigurationElement(element, cacheConfiguration);
        }
        addCopyStrategyConfigurationElement(element, cacheConfiguration);
        addElementValueComparatorConfigurationElement(element, cacheConfiguration);
        addCacheWriterConfigurationElement(element, cacheConfiguration);
        addAllFactoryConfigsAsChildElements(element, "cacheDecoratorFactory", cacheConfiguration.getCacheDecoratorConfigurations());
        addTerracottaConfigurationElement(element, cacheConfiguration);
        addPinningElement(element, cacheConfiguration);
        addSearchElement(element, cacheConfiguration);
    }

    private static void addBootstrapCacheLoaderFactoryConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        BootstrapCacheLoaderFactoryConfiguration bootstrapCacheLoaderFactoryConfiguration = cacheConfiguration
                .getBootstrapCacheLoaderFactoryConfiguration();
        if (bootstrapCacheLoaderFactoryConfiguration != null) {
            element.addChildElement(new FactoryConfigurationElement(element, "bootstrapCacheLoaderFactory",
                    bootstrapCacheLoaderFactoryConfiguration));
        }
    }

    private static void addCacheExceptionHandlerFactoryConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        CacheConfiguration.CacheExceptionHandlerFactoryConfiguration cacheExceptionHandlerFactoryConfiguration = cacheConfiguration
                .getCacheExceptionHandlerFactoryConfiguration();
        if (cacheExceptionHandlerFactoryConfiguration != null) {
            element.addChildElement(new FactoryConfigurationElement(element, "cacheExceptionHandlerFactory",
                    cacheExceptionHandlerFactoryConfiguration));
        }
    }

    private static void addSizeOfPolicyConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        SizeOfPolicyConfiguration sizeOfPolicyConfiguration = cacheConfiguration.getSizeOfPolicyConfiguration();
        if (sizeOfPolicyConfiguration != null &&
                !Configuration.DEFAULT_SIZEOF_POLICY_CONFIGURATION.equals(sizeOfPolicyConfiguration)) {
            element.addChildElement(new SizeOfPolicyConfigurationElement(element, sizeOfPolicyConfiguration));
        }
    }

    private static void addPersistenceConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        PersistenceConfiguration persistenceConfiguration = cacheConfiguration.getPersistenceConfiguration();
        if (persistenceConfiguration != null) {
            element.addChildElement(new PersistenceConfigurationElement(element, persistenceConfiguration));
        }
    }

    private static void addCopyStrategyConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        CopyStrategyConfiguration copyStrategyConfiguration = cacheConfiguration.getCopyStrategyConfiguration();
        if (copyStrategyConfiguration != null &&
                !copyStrategyConfiguration.equals(CacheConfiguration.DEFAULT_COPY_STRATEGY_CONFIGURATION)) {
            element.addChildElement(new CopyStrategyConfigurationElement(element, copyStrategyConfiguration));
        }
    }

    private static void addElementValueComparatorConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        ElementValueComparatorConfiguration elementValueComparatorConfiguration = cacheConfiguration
                .getElementValueComparatorConfiguration();
        if (elementValueComparatorConfiguration != null &&
                !elementValueComparatorConfiguration.getClassName().equals(DefaultElementValueComparator.class.getName())) {
            element.addChildElement(new ElementValueComparatorConfigurationElement(element, elementValueComparatorConfiguration));
        }
    }

    private static void addCacheWriterConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        CacheWriterConfiguration cacheWriterConfiguration = cacheConfiguration.getCacheWriterConfiguration();
        if (cacheWriterConfiguration != null && !CacheConfiguration.DEFAULT_CACHE_WRITER_CONFIGURATION.equals(cacheWriterConfiguration)) {
            element.addChildElement(new CacheWriterConfigurationElement(element, cacheWriterConfiguration));
        }
    }

    private static void addTerracottaConfigurationElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        TerracottaConfiguration terracottaConfiguration = cacheConfiguration.getTerracottaConfiguration();
        if (terracottaConfiguration != null) {
            element.addChildElement(new TerracottaConfigurationElement(element, terracottaConfiguration));
        }
    }

    private static void addSearchElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        if (cacheConfiguration.isSearchable()) {
            element.addChildElement(new SearchableConfigurationElement(element, cacheConfiguration.getSearchable()));
        }
    }

    private static void addPinningElement(NodeElement element, CacheConfiguration cacheConfiguration) {
        PinningConfiguration pinningConfiguration = cacheConfiguration.getPinningConfiguration();
        if (pinningConfiguration != null) {
            element.addChildElement(new PinningConfigurationElement(element, pinningConfiguration));
        }
    }

}
