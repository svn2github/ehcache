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

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.CopyStrategyConfiguration;
import net.sf.ehcache.config.ElementValueComparatorConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy.MemoryStoreEvictionPolicyEnum;

/**
 * Element representing the {@link CacheConfiguration}
 *
 * @author Abhishek Sanoujam
 *
 */
public class CacheConfigurationElement extends SimpleNodeElement {

    private final CacheConfiguration cacheConfiguration;

    /**
     * Constructor accepting the parent and the {@link CacheConfiguration}
     *
     * @param parent
     * @param cacheConfiguration
     */
    public CacheConfigurationElement(NodeElement parent, CacheConfiguration cacheConfiguration) {
        super(parent, "cache");
        this.cacheConfiguration = cacheConfiguration;
        init();
    }

    private void init() {
        if (cacheConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("name", cacheConfiguration.getName()).optional(false));
        addCommonAttributesWithDefaultCache(this, cacheConfiguration);
        addAttribute(new SimpleNodeAttribute("logging", cacheConfiguration.getLogging()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_LOGGING));

        addCommonChildElementsWithDefaultCache(this, cacheConfiguration);
        addAttribute(new SimpleNodeAttribute("maxBytesLocalHeap", cacheConfiguration.getMaxBytesLocalHeap())
                .optional(true).defaultValue(String.valueOf(CacheConfiguration.DEFAULT_MAX_BYTES_ON_HEAP)));
        addAttribute(new SimpleNodeAttribute("maxBytesLocalOffHeap", cacheConfiguration.getMaxBytesLocalOffHeap())
                .optional(true).defaultValue(String.valueOf(CacheConfiguration.DEFAULT_MAX_BYTES_OFF_HEAP)));
        addAttribute(new SimpleNodeAttribute("maxBytesLocalDisk", cacheConfiguration.getMaxBytesLocalDisk())
            .optional(true).defaultValue(String.valueOf(CacheConfiguration.DEFAULT_MAX_BYTES_ON_DISK)));
    }

    /**
     * Adds all attributes which are common with the "defaultCache" element in ehcache.xsd
     *
     * @param element
     * @param cacheConfiguration
     */
    public static void addCommonAttributesWithDefaultCache(NodeElement element, CacheConfiguration cacheConfiguration) {
        element.addAttribute(new SimpleNodeAttribute("eternal", cacheConfiguration.isEternal()).optional(false));
        element.addAttribute(new SimpleNodeAttribute("maxElementsInMemory", cacheConfiguration.getMaxElementsInMemory()).optional(false));
        element.addAttribute(new SimpleNodeAttribute("maxEntriesLocalHeap", cacheConfiguration.getMaxEntriesLocalHeap()).optional(false));
        element.addAttribute(new SimpleNodeAttribute("overflowToDisk", cacheConfiguration.isOverflowToDisk()).optional(false));
        element.addAttribute(new SimpleNodeAttribute("clearOnFlush", cacheConfiguration.isClearOnFlush()).optional(true).defaultValue(
                String.valueOf(CacheConfiguration.DEFAULT_CLEAR_ON_FLUSH)));
        element.addAttribute(new SimpleNodeAttribute("diskAccessStripes", cacheConfiguration.getDiskAccessStripes()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_DISK_ACCESS_STRIPES));
        element.addAttribute(new SimpleNodeAttribute("diskPersistent", cacheConfiguration.isDiskPersistent()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_DISK_PERSISTENT));
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
        element.addAttribute(new SimpleNodeAttribute("maxElementsOnDisk", cacheConfiguration.getMaxElementsOnDisk()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_MAX_ELEMENTS_ON_DISK));
        element.addAttribute(new SimpleNodeAttribute("maxEntriesLocalDisk", cacheConfiguration.getMaxEntriesLocalDisk()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_MAX_ELEMENTS_ON_DISK));
        element.addAttribute(new SimpleNodeAttribute("maxMemoryOffHeap", cacheConfiguration.getMaxMemoryOffHeap()).optional(true)
                .defaultValue((String) null));
        element.addAttribute(new SimpleNodeAttribute("overflowToOffHeap", cacheConfiguration.isOverflowToOffHeap()).optional(true)
                .defaultValue(false));
        element.addAttribute(new SimpleNodeAttribute("cacheLoaderTimeoutMillis", cacheConfiguration.getCacheLoaderTimeoutMillis())
                .optional(true).defaultValue(0L));
        element.addAttribute(new SimpleNodeAttribute("transactionalMode", cacheConfiguration.getTransactionalMode()).optional(true)
                .defaultValue(CacheConfiguration.DEFAULT_TRANSACTIONAL_MODE));
        element.addAttribute(new SimpleNodeAttribute("statistics", cacheConfiguration.getStatistics()).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_STATISTICS));
        element.addAttribute(new SimpleNodeAttribute("memoryStoreEvictionPolicy", MemoryStoreEvictionPolicyEnum.valueOf(cacheConfiguration
                .getMemoryStoreEvictionPolicy().toString())).optional(true).defaultValue(
                CacheConfiguration.DEFAULT_MEMORY_STORE_EVICTION_POLICY.toString().toLowerCase()));
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
        if (elementValueComparatorConfiguration != null
                && !elementValueComparatorConfiguration.equals(CacheConfiguration.DEFAULT_ELEMENT_VALUE_COMPARATOR_CONFIGURATION)) {
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
