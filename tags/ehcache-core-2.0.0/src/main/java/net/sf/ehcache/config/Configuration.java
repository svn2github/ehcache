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

package net.sf.ehcache.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.config.generator.ConfigurationSource;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;

/**
 * A bean, used by BeanUtils, to set configuration from an XML configuration file.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class Configuration {

    /**
     * Represents whether monitoring should be enabled or not.
     * @author amiller
     */
    public enum Monitoring {
        /** When possible, notice the use of Terracotta and auto register the SampledCacheMBean. */
        AUTODETECT,
        
        /** Always auto register the SampledCacheMBean */
        ON,
        
        /** Never auto register the SampledCacheMBean */
        OFF;
    }
    
    private String cacheManagerName;
    private boolean updateCheck = true;
    private Monitoring monitoring = Monitoring.AUTODETECT;
    private DiskStoreConfiguration diskStoreConfiguration;
    private CacheConfiguration defaultCacheConfiguration;
    private List<FactoryConfiguration> cacheManagerPeerProviderFactoryConfiguration = new ArrayList<FactoryConfiguration>();
    private List<FactoryConfiguration> cacheManagerPeerListenerFactoryConfiguration = new ArrayList<FactoryConfiguration>();
    private FactoryConfiguration transactionManagerLookupConfiguration;
    private FactoryConfiguration cacheManagerEventListenerFactoryConfiguration;
    private TerracottaConfigConfiguration terracottaConfigConfiguration;
    private final Map<String, CacheConfiguration> cacheConfigurations = new HashMap();
    private ConfigurationSource configurationSource;
    private boolean dynamicConfig = true;
    /**
     * Empty constructor, which is used by {@link ConfigurationFactory}, and can be also used programmatically.
     * <p/>
     * If you are using it programmtically you need to call the relevant add and setter methods in this class to
     * populate everything.
     */
    public Configuration() {
    }

    
    private FactoryConfiguration getDefaultTransactionManagerLookupConfiguration() {
        FactoryConfiguration configuration = new FactoryConfiguration();
        configuration.setClass(DefaultTransactionManagerLookup.class.getName());
        return configuration;
    }
    /**
     * Allows BeanHandler to set the CacheManager name.
     */
    public final void setName(String name) {
        this.cacheManagerName = name;
    }

    /**
     * CacheManager name
     */
    public final String getName() {
        return this.cacheManagerName;
    }

    /**
     * Allows BeanHandler to set the updateCheck flag.
     */
    public final void setUpdateCheck(boolean updateCheck) {
        this.updateCheck = updateCheck;
    }
    
    /**
     * Get flag for updateCheck
     */
    public final boolean getUpdateCheck() {
        return this.updateCheck;
    }
    
    /**
     * Allows BeanHandler to set the monitoring flag
     */
    public final void setMonitoring(String monitoring) {
        if (monitoring == null) {
            throw new IllegalArgumentException("Monitoring value must be non-null");
        }
        this.monitoring = Monitoring.valueOf(Monitoring.class, monitoring.toUpperCase());
    }
    
    /**
     * Get monitoring type, should not be null
     */
    public final Monitoring getMonitoring() {
        return this.monitoring;
    }

    /**
     * Allows BeanHandler to set the dynamic configuration flag
     */
    public final void setDynamicConfig(boolean dynamicConfig) {
        this.dynamicConfig = dynamicConfig;
    }

    /**
     * Get flag for dynamicConfig
     */
    public final boolean getDynamicConfig() {
        return this.dynamicConfig;
    }

    /**
     * Allows BeanHandler to add disk store location to the configuration.
     */
    public final void addDiskStore(DiskStoreConfiguration diskStoreConfigurationParameter) throws ObjectExistsException {
        if (diskStoreConfiguration != null) {
            throw new ObjectExistsException("The Disk Store has already been configured");
        }
        diskStoreConfiguration = diskStoreConfigurationParameter;
    }

    /**
     * Allows BeanHandler to add transaction manager lookup to the configuration.
     */
    public final void addTransactionManagerLookup(FactoryConfiguration transactionManagerLookupParameter) throws ObjectExistsException {
        if (transactionManagerLookupConfiguration != null) {
            throw new ObjectExistsException("The TransactionManagerLookup class has already been configured");
        }
        transactionManagerLookupConfiguration = transactionManagerLookupParameter;
    }

    /**
     * Allows BeanHandler to add the CacheManagerEventListener to the configuration.
     */
    public final void addCacheManagerEventListenerFactory(FactoryConfiguration
            cacheManagerEventListenerFactoryConfiguration) throws ObjectExistsException {
        if (this.cacheManagerEventListenerFactoryConfiguration == null) {
            this.cacheManagerEventListenerFactoryConfiguration = cacheManagerEventListenerFactoryConfiguration;
        }
    }


    /**
     * Adds a CachePeerProviderFactoryConfiguration.
     */
    public final void addCacheManagerPeerProviderFactory(FactoryConfiguration factory) {
        cacheManagerPeerProviderFactoryConfiguration.add(factory);
    }

    /**
     * Adds a CachePeerProviderFactoryConfiguration.
     * cachePeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory"
     * properties="hostName=localhost, port=5000"
     */
    public final void addCacheManagerPeerListenerFactory(FactoryConfiguration factory) {
        cacheManagerPeerListenerFactoryConfiguration.add(factory);
    }

    /**
     * Allows BeanHandler to add a Terracotta configuration to the configuration
     */
    public final void addTerracottaConfig(TerracottaConfigConfiguration terracottaConfiguration) {
        this.terracottaConfigConfiguration = terracottaConfiguration;
    }
    
    /**
     * Allows BeanHandler to add a default configuration to the configuration.
     */
    public final void addDefaultCache(CacheConfiguration defaultCacheConfiguration) throws ObjectExistsException {
        if (this.defaultCacheConfiguration != null) {
            throw new ObjectExistsException("The Default Cache has already been configured");
        }
        this.defaultCacheConfiguration = defaultCacheConfiguration;
    }

    /**
     * Allows BeanHandler to add Cache Configurations to the configuration.
     */
    public final void addCache(CacheConfiguration cacheConfiguration) throws ObjectExistsException {
        if (cacheConfigurations.get(cacheConfiguration.name) != null) {
            throw new ObjectExistsException("Cannot create cache: " + cacheConfiguration.name
                    + " with the same name as an existing one.");
        }
        if (cacheConfiguration.name.equalsIgnoreCase(net.sf.ehcache.Cache.DEFAULT_CACHE_NAME)) {
            throw new ObjectExistsException("The Default Cache has already been configured");
        }

        cacheConfigurations.put(cacheConfiguration.name, cacheConfiguration);
    }

    /**
     * Gets a Map of cacheConfigurations.
     */
    public final Set<String> getCacheConfigurationsKeySet() {
        return cacheConfigurations.keySet();
    }

    /**
     * @return the configuration's default cache configuration
     */
    public final CacheConfiguration getDefaultCacheConfiguration() {
        return defaultCacheConfiguration;
    }

    /**
     * @param defaultCacheConfiguration
     */
    public final void setDefaultCacheConfiguration(CacheConfiguration defaultCacheConfiguration) {
        this.defaultCacheConfiguration = defaultCacheConfiguration;
    }


    /**
     * Gets the disk store configuration.
     */
    public final DiskStoreConfiguration getDiskStoreConfiguration() {
        return diskStoreConfiguration;
    }
    
    /**
     * Gets the transaction manager lookup configuration.
     */
    public final FactoryConfiguration getTransactionManagerLookupConfiguration() {
        if (transactionManagerLookupConfiguration == null) {
            return getDefaultTransactionManagerLookupConfiguration();
        }
        return transactionManagerLookupConfiguration;
    }

    /**
     * Gets the CacheManagerPeerProvider factory configuration.
     */
    public final List<FactoryConfiguration> getCacheManagerPeerProviderFactoryConfiguration() {
        return cacheManagerPeerProviderFactoryConfiguration;
    }

    /**
     * Gets the CacheManagerPeerListener factory configuration.
     */
    public final List<FactoryConfiguration> getCacheManagerPeerListenerFactoryConfigurations() {
        return cacheManagerPeerListenerFactoryConfiguration;
    }

    /**
     * Gets the CacheManagerEventListener factory configuration.
     */
    public final FactoryConfiguration getCacheManagerEventListenerFactoryConfiguration() {
        return cacheManagerEventListenerFactoryConfiguration;
    }

    /**
     * Gets the TerracottaConfigConfiguration
     */
    public final TerracottaConfigConfiguration getTerracottaConfiguration() {
        return this.terracottaConfigConfiguration;
    }

    /**
     * Gets a Map of cache configurations, keyed by name.
     */
    public final Map<String, CacheConfiguration> getCacheConfigurations() {
        return cacheConfigurations;
    }

    /**
     * Sets the configuration source.
     *
     * @param configurationSource an informative description of the source, preferably
     *                            including the resource name and location.
     */
    public final void setSource(ConfigurationSource configurationSource) {
        this.configurationSource = configurationSource;
    }

    /**
     * Gets a description of the source from which this configuration was created.
     */
    public final ConfigurationSource getConfigurationSource() {
        return configurationSource;
    }
}
