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

package net.sf.ehcache.config;

import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.config.generator.ConfigurationSource;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bean, used by BeanUtils, to set configuration from an XML configuration file.
 * 
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class Configuration {

    /**
     * Default value for dynamicConfig
     */
    public static final boolean DEFAULT_DYNAMIC_CONFIG = true;
    /**
     * Default value for updateCheck
     */
    public static final boolean DEFAULT_UPDATE_CHECK = true;
    /**
     * Default value for monitoring
     */
    public static final Monitoring DEFAULT_MONITORING = Monitoring.AUTODETECT;

    /**
     * Default transactionManagerLookupConfiguration
     */
    public static final FactoryConfiguration DEFAULT_TRANSACTION_MANAGER_LOOKUP_CONFIG = getDefaultTransactionManagerLookupConfiguration();

    /**
     * Represents whether monitoring should be enabled or not.
     * 
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
    private boolean updateCheck = DEFAULT_UPDATE_CHECK;
    private Monitoring monitoring = DEFAULT_MONITORING;
    private DiskStoreConfiguration diskStoreConfiguration;
    private CacheConfiguration defaultCacheConfiguration;
    private List<FactoryConfiguration> cacheManagerPeerProviderFactoryConfiguration = new ArrayList<FactoryConfiguration>();
    private List<FactoryConfiguration> cacheManagerPeerListenerFactoryConfiguration = new ArrayList<FactoryConfiguration>();
    private FactoryConfiguration transactionManagerLookupConfiguration;
    private FactoryConfiguration cacheManagerEventListenerFactoryConfiguration;
    private TerracottaClientConfiguration terracottaConfigConfiguration;
    private final Map<String, CacheConfiguration> cacheConfigurations = new ConcurrentHashMap<String, CacheConfiguration>();
    private ConfigurationSource configurationSource;
    private boolean dynamicConfig = DEFAULT_DYNAMIC_CONFIG;
    private Object defaultTransactionManager;

    /**
     * Empty constructor, which is used by {@link ConfigurationFactory}, and can be also used programmatically.
     * <p/>
     * If you are using it programmtically you need to call the relevant add and setter methods in this class to populate everything.
     */
    public Configuration() {
    }

    private static FactoryConfiguration getDefaultTransactionManagerLookupConfiguration() {
        FactoryConfiguration configuration = new FactoryConfiguration();
        configuration.setClass(DefaultTransactionManagerLookup.class.getName());
        return configuration;
    }

    /**
     * Builder to set the cache manager name.
     * 
     * @see #setName(String)
     * @param name
     *            the name to set
     * @return this configuration instance
     */
    public final Configuration name(String name) {
        setName(name);
        return this;
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
     * Builder to set the state of the automated update check.
     * 
     * @param updateCheck
     *            {@code true} if the update check should be turned on; or {@code false} otherwise
     * @return this configuration instance
     */
    public final Configuration updateCheck(boolean updateCheck) {
        setUpdateCheck(updateCheck);
        return this;
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
     * Builder to set the monitoring approach
     * 
     * @param monitoring
     *            an non-null instance of {@link Monitoring}
     * @return this configuration instance
     */
    public final Configuration monitoring(Monitoring monitoring) {
        if (null == monitoring) {
            throw new IllegalArgumentException("Monitoring value must be non-null");
        }

        this.monitoring = monitoring;
        return this;
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
     * Builder to set the dynamic config capability
     * 
     * @param dynamicConfig
     *            {@code true} if dynamic config should be enabled; or {@code false} otherwise.
     * @return this configuration instance
     */
    public final Configuration dynamicConfig(boolean dynamicConfig) {
        setDynamicConfig(dynamicConfig);
        return this;
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
     * Builder to add a disk store to the cache manager, only one disk store can be added.
     * 
     * @param diskStoreConfigurationParameter
     *            the disk store configuration to use
     * @return this configuration instance
     * @throws ObjectExistsException
     *             if the disk store has already been configured
     */
    public final Configuration diskStore(DiskStoreConfiguration diskStoreConfigurationParameter) throws ObjectExistsException {
        addDiskStore(diskStoreConfigurationParameter);
        return this;
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
     * Builder to add a transaction manager lookup class to the cache manager, only one of these can be added.
     * 
     * @param transactionManagerLookupParameter
     *            the transaction manager lookup class to use
     * @return this configuration instance
     * @throws ObjectExistsException
     *             if the transaction manager lookup has already been configured
     */
    public final Configuration transactionManagerLookup(FactoryConfiguration transactionManagerLookupParameter)
            throws ObjectExistsException {
        addTransactionManagerLookup(transactionManagerLookupParameter);
        return this;
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
     * Builder to set the event lister through a factory, only one of these can be added and subsequent calls are ignored.
     * 
     * @return this configuration instance
     */
    public final Configuration cacheManagerEventListenerFactory(FactoryConfiguration cacheManagerEventListenerFactoryConfiguration) {
        addCacheManagerEventListenerFactory(cacheManagerEventListenerFactoryConfiguration);
        return this;
    }

    /**
     * Allows BeanHandler to add the CacheManagerEventListener to the configuration.
     */
    public final void addCacheManagerEventListenerFactory(FactoryConfiguration cacheManagerEventListenerFactoryConfiguration) {
        if (this.cacheManagerEventListenerFactoryConfiguration == null) {
            this.cacheManagerEventListenerFactoryConfiguration = cacheManagerEventListenerFactoryConfiguration;
        }
    }

    /**
     * Builder method to add a peer provider through a factory.
     * 
     * @return this configuration instance
     */
    public final Configuration cacheManagerPeerProviderFactory(FactoryConfiguration factory) {
        addCacheManagerPeerProviderFactory(factory);
        return this;
    }

    /**
     * Adds a CacheManagerPeerProvider through FactoryConfiguration.
     */
    public final void addCacheManagerPeerProviderFactory(FactoryConfiguration factory) {
        cacheManagerPeerProviderFactoryConfiguration.add(factory);
    }

    /**
     * Builder method to add a peer listener through a factory.
     * 
     * @return this configuration instance
     */
    public final Configuration cacheManagerPeerListenerFactory(FactoryConfiguration factory) {
        addCacheManagerPeerListenerFactory(factory);
        return this;
    }

    /**
     * Adds a CacheManagerPeerListener through FactoryConfiguration.
     */
    public final void addCacheManagerPeerListenerFactory(FactoryConfiguration factory) {
        cacheManagerPeerListenerFactoryConfiguration.add(factory);
    }

    /**
     * Builder method to Terracotta capabilities to the cache manager through a dedicated configuration, this can only be used once.
     * 
     * @return this configuration instance
     * @throws ObjectExistsException
     *             if the Terracotta config has already been configured
     */
    public final Configuration terracotta(TerracottaClientConfiguration terracottaConfiguration) throws ObjectExistsException {
        addTerracottaConfig(terracottaConfiguration);
        return this;
    }

    /**
     * Allows BeanHandler to add a Terracotta configuration to the configuration
     */
    public final void addTerracottaConfig(TerracottaClientConfiguration terracottaConfiguration) throws ObjectExistsException {
        if (this.terracottaConfigConfiguration != null) {
            throw new ObjectExistsException("The TerracottaConfig has already been configured");
        }
        this.terracottaConfigConfiguration = terracottaConfiguration;
    }

    /**
     * Builder method to set the default cache configuration, this can only be used once.
     * 
     * @return this configuration instance
     * @throws ObjectExistsException
     *             if the default cache config has already been configured
     */
    public final Configuration defaultCache(CacheConfiguration defaultCacheConfiguration) throws ObjectExistsException {
        setDefaultCacheConfiguration(defaultCacheConfiguration);
        return this;
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
     * Builder to add a new cache through its config
     * 
     * @return this configuration instance
     * @throws ObjectExistsException
     *             if a cache with the same name already exists, or if the name conflicts with the name of the default cache
     */
    public final Configuration cache(CacheConfiguration cacheConfiguration) throws ObjectExistsException {
        addCache(cacheConfiguration);
        return this;
    }

    /**
     * Allows BeanHandler to add Cache Configurations to the configuration.
     */
    public final void addCache(CacheConfiguration cacheConfiguration) throws ObjectExistsException {
        if (cacheConfigurations.get(cacheConfiguration.name) != null) {
            throw new ObjectExistsException("Cannot create cache: " + cacheConfiguration.name + " with the same name as an existing one.");
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
     * Gets the TerracottaClientConfiguration
     */
    public final TerracottaClientConfiguration getTerracottaConfiguration() {
        return this.terracottaConfigConfiguration;
    }

    /**
     * Gets a Map of cache configurations, keyed by name.
     */
    public final Map<String, CacheConfiguration> getCacheConfigurations() {
        return cacheConfigurations;
    }

    /**
     * Builder to set the configuration source.
     * 
     * @return this configuration instance
     */
    public final Configuration source(ConfigurationSource configurationSource) {
        setSource(configurationSource);
        return this;
    }

    /**
     * Sets the configuration source.
     * 
     * @param configurationSource
     *            an informative description of the source, preferably
     *            including the resource name and location.
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

    /**
     * Builder to set the default transaction manager to inject in XA caches.
     * 
     * @return this configuration instance
     */
    public final Configuration defaultTransactionManager(final Object defaultTransactionManager) {
        setDefaultTransactionManager(defaultTransactionManager);
        return this;
    }

    /**
     * Should the CacheManager inject a specific TransactionManager in the XA Caches
     * 
     * @param defaultTransactionManager
     *            TransactionManager to inject
     */
    public void setDefaultTransactionManager(final Object defaultTransactionManager) {
        this.defaultTransactionManager = defaultTransactionManager;
    }

    /**
     * The default CacheManager XA Caches should use
     * 
     * @return the default one, or null
     */
    public Object getDefaultTransactionManager() {
        return defaultTransactionManager;
    }
}
