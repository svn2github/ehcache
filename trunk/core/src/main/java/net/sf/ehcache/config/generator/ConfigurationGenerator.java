/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.config.generator;

import java.util.List;
import java.util.Map;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.TerracottaConfigConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheExceptionHandlerFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheExtensionFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheLoaderFactoryConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * Utility class for generating configuration texts.
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public class ConfigurationGenerator {

    private static final String EOL = System.getProperty("line.separator");
    private static final String INDENT = "    ";

    private StringBuilder builder;
    private int numIndents;
    private String spacer = "";

    private void visitCacheManagerConfig(Configuration configuration) {
        builder.append(spacer).append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append(EOL);

        builder.append(spacer).append("<ehcache ");
        if (notBlank(configuration.getName())) {
            builder.append(spacer).append("name =\"").append(configuration.getName()).append("\" ");
        }
        builder.append(spacer).append("updateCheck=\"").append(configuration.getUpdateCheck()).append("\" ");
        builder.append(spacer).append("monitoring=\"").append(configuration.getMonitoring().name().toLowerCase()).append("\"");
        builder.append(spacer).append(">");
        builder.append(EOL);

        indent(1);

        DiskStoreConfiguration diskConfig = configuration.getDiskStoreConfiguration();
        if (diskConfig != null) {
            builder.append(spacer).append("<diskStore path=\"").append(diskConfig.getOriginalPath()).append("\">");
            builder.append(EOL);
        }

        FactoryConfiguration cacheManagerEventListenerConfig = configuration.getCacheManagerEventListenerFactoryConfiguration();
        if (cacheManagerEventListenerConfig != null) {
            builder.append(spacer).append(
                    "<cacheManagerEventListenerFactory class=\"" + cacheManagerEventListenerConfig.getFullyQualifiedClassPath()).append(
                    "\" properties=\"" + cacheManagerEventListenerConfig.getProperties()).append("\"/>");
            builder.append(EOL);
        }

        List<FactoryConfiguration> peerProviderConfig = configuration.getCacheManagerPeerProviderFactoryConfiguration();
        if (peerProviderConfig != null && peerProviderConfig.size() > 0) {
            for (FactoryConfiguration config : peerProviderConfig) {
                builder.append(spacer).append("<cacheManagerPeerProviderFactory class=\"").append(config.getFullyQualifiedClassPath())
                        .append("\" properties=\"").append(config.getProperties()).append("\"");
                builder.append(EOL);
            }
        }

        List<FactoryConfiguration> peerListenersConfig = configuration.getCacheManagerPeerListenerFactoryConfigurations();
        if (peerListenersConfig != null && peerListenersConfig.size() > 0) {
            for (FactoryConfiguration config : peerListenersConfig) {
                builder.append(spacer).append("<cacheManagerPeerListenerFactory class=\"").append(config.getFullyQualifiedClassPath())
                        .append("\" properties=\"").append(config.getProperties()).append("\"");
                builder.append(EOL);
            }
        }

        TerracottaConfigConfiguration terracottaConfig = configuration.getTerracottaConfiguration();
        if (terracottaConfig != null) {
            if (terracottaConfig.isUrlConfig()) {
                builder.append(spacer).append("<terracottaConfig url=\"").append(terracottaConfig.getUrl()).append("\">");
                builder.append(EOL);
            } else {
                builder.append(spacer).append("<terracottaConfig>");
                builder.append(EOL);
                builder.append(terracottaConfig.getOriginalEmbeddedConfig());
                builder.append(spacer).append("<terracottaConfig>");
                builder.append(EOL);
            }
        }

    }

    private boolean notBlank(String str) {
        return str != null && !"".equals(str.trim());
    }

    private void visitDefaultCache(CacheConfiguration defaultCacheConfiguration) {
        visitCache("defaultCache", defaultCacheConfiguration);
    }

    private void visitCache(String tagName, CacheConfiguration cacheConfiguration) {
        builder.append(spacer).append("<").append(tagName);
        if (notBlank(cacheConfiguration.getName())) {
            builder.append(" name=\"").append(cacheConfiguration.getName()).append("\"").append(EOL);
        } else {
            builder.append(EOL);
        }

        indent(1);
        builder.append(spacer).append("maxElementsInMemory=\"").append(cacheConfiguration.getMaxElementsInMemory()).append("\"")
                .append(EOL);
        builder.append(spacer).append("maxElementsOnDisk=\"").append(cacheConfiguration.getMaxElementsOnDisk()).append("\"").append(EOL);
        builder.append(spacer).append("eternal=\"").append(cacheConfiguration.isEternal()).append("\"").append(EOL);
        builder.append(spacer).append("overflowToDisk=\"").append(cacheConfiguration.isOverflowToDisk()).append("\"").append(EOL);
        // optional attributes
        builder.append(spacer).append("timeToIdleSeconds=\"").append(cacheConfiguration.getTimeToIdleSeconds()).append("\"").append(EOL);
        builder.append(spacer).append("timeToLiveSeconds=\"").append(cacheConfiguration.getTimeToLiveSeconds()).append("\"").append(EOL);
        builder.append(spacer).append("diskPersistent=\"").append(cacheConfiguration.isDiskPersistent()).append("\"").append(EOL);
        builder.append(spacer).append("diskExpiryThreadIntervalSeconds=\"").append(cacheConfiguration.getDiskExpiryThreadIntervalSeconds())
                .append("\"" + EOL);
        builder.append(spacer).append("diskSpoolBufferSizeMB=\"").append(cacheConfiguration.getDiskSpoolBufferSizeMB()).append("\"")
                .append(EOL);
        MemoryStoreEvictionPolicy memoryStoreEvictionPolicy = cacheConfiguration.getMemoryStoreEvictionPolicy();
        if (memoryStoreEvictionPolicy != null) {
            builder.append(spacer).append("memoryStoreEvictionPolicy=\"").append(memoryStoreEvictionPolicy.toString()).append("\"").append(
                    EOL);
        }
        builder.append(spacer).append("clearOnFlush=\"").append(cacheConfiguration.isClearOnFlush()).append("\">").append(EOL);

        indent(1);

        List<CacheEventListenerFactoryConfiguration> listenerConfigs = cacheConfiguration.getCacheEventListenerConfigurations();
        if (listenerConfigs != null && listenerConfigs.size() > 0) {
            for (CacheEventListenerFactoryConfiguration config : listenerConfigs) {
                builder.append(spacer).append("<cacheEventListenerFactory class=\"").append(config.getFullyQualifiedClassPath()).append(
                        "\" properties=\"" + config.getProperties()).append("\"");
                builder.append(EOL);
            }
        }

        BootstrapCacheLoaderFactoryConfiguration bootstrapCacheLoaderFactoryConfiguration = cacheConfiguration
                .getBootstrapCacheLoaderFactoryConfiguration();
        if (bootstrapCacheLoaderFactoryConfiguration != null) {
            builder.append(spacer).append(
                    "<bootstrapCacheLoaderFactory class=\"" + bootstrapCacheLoaderFactoryConfiguration.getFullyQualifiedClassPath())
                    .append("\" properties=\"" + bootstrapCacheLoaderFactoryConfiguration.getProperties()).append("\"");
            builder.append(EOL);
        }

        CacheExceptionHandlerFactoryConfiguration exceptionConfig = cacheConfiguration.getCacheExceptionHandlerFactoryConfiguration();
        if (exceptionConfig != null) {
            builder.append(spacer).append("<cacheExceptionHandlerFactory class=\"").append(exceptionConfig.getFullyQualifiedClassPath())
                    .append("\" properties=\"").append(exceptionConfig.getProperties()).append("\"");
            builder.append(EOL);
        }

        List<CacheLoaderFactoryConfiguration> loaderConfigs = cacheConfiguration.getCacheLoaderConfigurations();
        if (loaderConfigs != null && loaderConfigs.size() > 0) {
            for (CacheLoaderFactoryConfiguration config : loaderConfigs) {
                builder.append(spacer).append("<cacheLoaderFactory class=\"").append(exceptionConfig.getFullyQualifiedClassPath()).append(
                        "\" properties=\"" + exceptionConfig.getProperties()).append("\"");
                builder.append(EOL);
            }
        }

        List<CacheExtensionFactoryConfiguration> extensionConfigs = cacheConfiguration.getCacheExtensionConfigurations();
        if (extensionConfigs != null && extensionConfigs.size() > 0) {
            for (CacheExtensionFactoryConfiguration config : extensionConfigs) {
                builder.append(spacer).append("<cacheExtensionFactory class=\"").append(exceptionConfig.getFullyQualifiedClassPath())
                        .append("\" properties=\"").append(exceptionConfig.getProperties()).append("\"");
                builder.append(EOL);
            }
        }

        TerracottaConfiguration terracottaConfiguration = cacheConfiguration.getTerracottaConfiguration();
        if (terracottaConfiguration != null) {
            builder.append(spacer).append("<terracotta");
            indent(1);
            builder.append(" clustered=\"").append(terracottaConfiguration.isClustered()).append("\"");
            if (!TerracottaConfiguration.DEFAULT_VALUE_MODE.equals(terracottaConfiguration.getValueMode())) {
                builder.append(EOL).append(spacer).append(" valueMode=\"").append(
                        terracottaConfiguration.getValueMode().name().toLowerCase()).append("\"");
            }
            if (TerracottaConfiguration.DEFAULT_COHERENT_READS != terracottaConfiguration.getCoherentReads()) {
                builder.append(EOL).append(spacer).append(" coherentReads=\"").append(terracottaConfiguration.getCoherentReads()).append(
                        "\"");
            }
            if (TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE != terracottaConfiguration.getLocalKeyCache()) {
                builder.append(EOL).append(spacer).append(" localKeyCache=\"").append(terracottaConfiguration.getLocalKeyCache()).append(
                        "\"");
            }
            if (TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE_SIZE != terracottaConfiguration.getLocalKeyCacheSize()) {
                builder.append(EOL).append(spacer).append(" localKeyCacheSize=\"").append(terracottaConfiguration.getLocalKeyCacheSize())
                        .append("\"");
            }
            if (TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION != terracottaConfiguration.getOrphanEviction()) {
                builder.append(EOL).append(spacer).append(" orphanEviction=\"").append(terracottaConfiguration.getOrphanEviction()).append(
                        "\"");
            }
            if (TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION_PERIOD != terracottaConfiguration.getOrphanEvictionPeriod()) {
                builder.append(EOL).append(spacer).append(" orphanEvictionPeriod=\"").append(
                        terracottaConfiguration.getOrphanEvictionPeriod()).append("\"");
            }
            if (TerracottaConfiguration.DEFAULT_CACHE_COHERENT != terracottaConfiguration.isCoherent()) {
                builder.append(EOL).append(spacer).append(" coherent=\"").append(terracottaConfiguration.isCoherent()).append(
                "\"");
            }
            if (TerracottaConfiguration.DEFAULT_SYNCHRONOUS_WRITE != terracottaConfiguration.isSynchronousWrite()) {
                builder.append(EOL).append(spacer).append(" synchronousWrite=\"").append(terracottaConfiguration.isSynchronousWrite())
                    .append("\"");
            }
            builder.append(">").append(EOL);
            indent(-1);
            builder.append(spacer).append("</terracotta>");
            builder.append(EOL);
        }
        indent(-1);
        indent(-1);
        builder.append(spacer).append("</").append(tagName).append(">");
        builder.append(EOL);
    }

    private void indent(int delta) {
        numIndents += delta;
        spacer = "";
        for (int i = 0; i < numIndents; i++) {
            spacer += INDENT;
        }
    }

    private void visitCaches(Map<String, CacheConfiguration> cacheConfigs) {
        for (CacheConfiguration config : cacheConfigs.values()) {
            visitCache("cache", config);
        }
    }

    /**
     * Generates the configuration text for the provided {@link Configuration}, the default {@link CacheConfiguration} and the map of
     * {@link CacheConfiguration}'s
     * 
     * @param configuration
     * @param defaultCacheConfiguration
     * @param cacheConfigs
     * @return String equivalent to an ehcache.xml for the input parameters
     */
    public String generate(Configuration configuration, CacheConfiguration defaultCacheConfiguration,
            Map<String, CacheConfiguration> cacheConfigs) {
        builder = new StringBuilder();
        visitCacheManagerConfig(configuration);
        visitDefaultCache(defaultCacheConfiguration);
        visitCaches(cacheConfigs);
        indent(-1);
        builder.append(spacer).append("</ehcache>");
        return builder.toString();
    }

    /**
     * Generates configuration text for a specific cache using the input {@link CacheConfiguration}
     * 
     * @param cacheConfiguration
     * @return String containing configuration for the input {@link CacheConfiguration}
     */
    public String generate(CacheConfiguration cacheConfiguration) {
        builder = new StringBuilder();
        visitCache("cache", cacheConfiguration);
        return builder.toString();
    }

}
