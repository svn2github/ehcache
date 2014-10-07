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

package net.sf.ehcache.config;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.EhcacheDefaultClassLoader;
import net.sf.ehcache.FeaturesManager;
import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.generator.ConfigurationSource;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.util.PropertyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
     * Default value for defaultTransactionTimeoutInSeconds
     */
    public static final int  DEFAULT_TRANSACTION_TIMEOUT = 15;
    /**
     * Default value for maxBytesLocalHeap when not explicitly set
     */
    public static final long DEFAULT_MAX_BYTES_ON_HEAP   =  0;
    /**
     * Default value for maxBytesLocalOffHeap when not explicitly set
     */
    public static final long DEFAULT_MAX_BYTES_OFF_HEAP  =  0;
    /**
     * Default value for maxBytesLocalDisk when not explicitly set
     */
    public static final long DEFAULT_MAX_BYTES_ON_DISK   =  0;
    /**
     * Default value for monitoring
     */
    public static final Monitoring DEFAULT_MONITORING = Monitoring.AUTODETECT;
    /**
     * Default sizeOfPolicy configuration
     */
    public static final SizeOfPolicyConfiguration DEFAULT_SIZEOF_POLICY_CONFIGURATION = new SizeOfPolicyConfiguration();

    /**
     * Default transactionManagerLookupConfiguration
     */
    public static final FactoryConfiguration DEFAULT_TRANSACTION_MANAGER_LOOKUP_CONFIG = getDefaultTransactionManagerLookupConfiguration();
    private static final int HUNDRED = 100;
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    private volatile RuntimeCfg cfg;
    private final List<PropertyChangeListener> propertyChangeListeners = new CopyOnWriteArrayList<PropertyChangeListener>();

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

    /**
     * Enum of all properties that can change once the Configuration is being used by a CacheManager
     */
    private static enum DynamicProperty {

        cacheManagerName {
            @Override
            void applyChange(final PropertyChangeEvent evt, final RuntimeCfg config) {
                config.cacheManagerName = (String) evt.getNewValue();
            }
        },
        defaultCacheConfiguration {
            @Override
            void applyChange(final PropertyChangeEvent evt, final RuntimeCfg config) {
                LOG.debug("Default Cache Configuration has changed, previously created caches remain untouched");
            }
        },
        maxBytesLocalHeap {
            @Override
            void applyChange(final PropertyChangeEvent evt, final RuntimeCfg config) {

                Long newValue = (Long)evt.getNewValue();
                Long oldValue = (Long) evt.getOldValue();
                if (oldValue > newValue) {
                    // Double check for over-allocation again
                    validateOverAllocation(config, newValue);
                }
                // Recalculate % based caches
                long cacheAllocated = 0;
                for (Cache cache : getAllActiveCaches(config.cacheManager)) {
                    cache.getCacheConfiguration().configCachePools(config.getConfiguration());
                    long bytesLocalHeap = cache.getCacheConfiguration().getMaxBytesLocalHeap();
                    cacheAllocated += bytesLocalHeap;
                }
                config.cacheManager.getOnHeapPool().setMaxSize(newValue - cacheAllocated);
            }
        },
        maxBytesLocalDisk {
            @Override
            void applyChange(final PropertyChangeEvent evt, final RuntimeCfg config) {
                Long newValue = (Long) evt.getNewValue();
                Long oldValue = (Long) evt.getOldValue();
                if (oldValue > newValue) {
                    // Double check for over-allocation again
                    validateOverAllocation(config, newValue);
                }
                long diskAllocated = 0;
                //Recalculating final free space available at global level
                for (Cache cache : getAllActiveCaches(config.cacheManager)) {
                  cache.getCacheConfiguration().configCachePools(config.getConfiguration());
                  long bytesOnDiskPool = cache.getCacheConfiguration().getMaxBytesLocalDisk();
                  diskAllocated += bytesOnDiskPool;
                }
                config.cacheManager.getOnDiskPool().setMaxSize(newValue - diskAllocated);
            }
        };


        private static void validateOverAllocation(RuntimeCfg config,  Long newValue) {
            ArrayList<ConfigError> errors = new ArrayList<ConfigError>();

            for (Cache cache : getAllActiveCaches(config.cacheManager)) {
                CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
                errors.addAll(cacheConfiguration.validateCachePools(config.getConfiguration()));
                errors.addAll(cacheConfiguration.verifyPoolAllocationsBeforeAddingTo(config.cacheManager,
                    newValue, config.getConfiguration().getMaxBytesLocalOffHeap(),
                        config.getConfiguration().getMaxBytesLocalDisk(), null));
            }
            if (!errors.isEmpty()) {
                throw new InvalidConfigurationException("Can't reduce CacheManager byte tuning by so much", errors);
            }
        }

        abstract void applyChange(PropertyChangeEvent evt, RuntimeCfg config);
    }

    private String cacheManagerName;
    private boolean updateCheck = DEFAULT_UPDATE_CHECK;
    private int defaultTransactionTimeoutInSeconds = DEFAULT_TRANSACTION_TIMEOUT;
    private Monitoring monitoring = DEFAULT_MONITORING;
    private DiskStoreConfiguration diskStoreConfiguration;
    private CacheConfiguration defaultCacheConfiguration;
    private final List<FactoryConfiguration> cacheManagerPeerProviderFactoryConfiguration = new ArrayList<FactoryConfiguration>();
    private final List<FactoryConfiguration> cacheManagerPeerListenerFactoryConfiguration = new ArrayList<FactoryConfiguration>();
    private SizeOfPolicyConfiguration sizeOfPolicyConfiguration;
    private FactoryConfiguration transactionManagerLookupConfiguration;
    private FactoryConfiguration cacheManagerEventListenerFactoryConfiguration;
    private TerracottaClientConfiguration terracottaConfigConfiguration;
    private ManagementRESTServiceConfiguration managementRESTService;
    private final Map<String, CacheConfiguration> cacheConfigurations = new ConcurrentHashMap<String, CacheConfiguration>();
    private ConfigurationSource configurationSource;
    private boolean dynamicConfig = DEFAULT_DYNAMIC_CONFIG;
    private Long maxBytesLocalHeap;
    private String maxBytesLocalHeapInput;
    private Long maxBytesLocalOffHeap;
    private String maxBytesLocalOffHeapInput;
    private Long maxBytesLocalDisk;
    private String maxBytesLocalDiskInput;
    private volatile ClassLoader classLoader = EhcacheDefaultClassLoader.getInstance();

    /**
     * Empty constructor, which is used by {@link ConfigurationFactory}, and can be also used programmatically.
     * <p/>
     * If you are using it programmtically you need to call the relevant add and setter methods in this class to populate everything.
     */
    public Configuration() {
    }

    /**
     * Returns all active caches managed by the Manager
     * @param cacheManager The cacheManager
     * @return the Set of all active caches
     */
    static Set<Cache> getAllActiveCaches(CacheManager cacheManager) {
        final Set<Cache> caches = new HashSet<Cache>();
        for (String cacheName : cacheManager.getCacheNames()) {
            final Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                caches.add(cache);
            }
        }
        return caches;
    }

    /**
     * Freezes part of the configuration that need to be, and runs validation checks on the Configuration.
     *
     * @param cacheManager the CacheManager instance being configured
     * @throws InvalidConfigurationException With all the associated errors
     */
    public RuntimeCfg setupFor(final CacheManager cacheManager, final String fallbackName) throws InvalidConfigurationException {
        if (cfg != null) {
            if (cfg.cacheManager == cacheManager) {
                return cfg;
            } else if (cfg.cacheManager.getStatus() != Status.STATUS_SHUTDOWN) {
                throw new IllegalStateException("You cannot share a Configuration instance across multiple running CacheManager instances");
            }
        }
        final Collection<ConfigError> errors = validate();
        if (!errors.isEmpty()) {
            throw new InvalidConfigurationException(errors);
        }
        cfg = new Configuration.RuntimeCfg(cacheManager, fallbackName);
        return cfg;
    }

    /**
     * Validates the current configuration
     * @return the list of errors withing that configuration
     */
    public Collection<ConfigError> validate() {
        final Collection<ConfigError> errors = new ArrayList<ConfigError>();

        for (CacheConfiguration cacheConfiguration : cacheConfigurations.values()) {
            errors.addAll(cacheConfiguration.validate(this));
        }
        return errors;
    }

    /**
     * Checks whether the user explicitly set the maxBytesOnDisk
     * @return true if set by user, false otherwise
     * @see #setMaxBytesLocalDisk(Long)
     */
    public boolean isMaxBytesLocalDiskSet() {
        return maxBytesLocalDisk != null;
    }

    /**
     * Checks whether the user explicitly set the maxBytesOffHeat
     * @return true if set by user, false otherwise
     * @see #setMaxBytesLocalOffHeap(Long)
     */
    public boolean isMaxBytesLocalOffHeapSet() {
        return maxBytesLocalOffHeap != null;
    }

    /**
     * Checks whether the user explicitly set the maxBytesOnHeap
     * @return true if set by user, false otherwise
     * @see #setMaxBytesLocalHeap(Long)
     */
    public boolean isMaxBytesLocalHeapSet() {
        return maxBytesLocalHeap != null;
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
        assertArgumentNotNull("name", name);
        final String prop = "cacheManagerName";
        final boolean publishChange = checkDynChange(prop);
        String oldValue = this.cacheManagerName;
        this.cacheManagerName = name;
        if (publishChange) {
            firePropertyChange(prop, oldValue, name);
        }
    }

    private void assertArgumentNotNull(String name, Object object) {
        if (object == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
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
        String prop = "updateCheck";
        final boolean publish = checkDynChange(prop);
        final boolean oldValue = this.updateCheck;
        this.updateCheck = updateCheck;
        if (publish) {
            firePropertyChange(prop, oldValue, updateCheck);
        }
    }

    /**
     * Get flag for updateCheck
     */
    public final boolean getUpdateCheck() {
        return this.updateCheck;
    }

    /**
     * Builder to set the default transaction timeout.
     *
     * @param defaultTransactionTimeoutInSeconds the default transaction timeout in seconds
     * @return this configuration instance
     */
    public final Configuration defaultTransactionTimeoutInSeconds(int defaultTransactionTimeoutInSeconds) {
        setDefaultTransactionTimeoutInSeconds(defaultTransactionTimeoutInSeconds);
        return this;
    }

    /**
     * Allows BeanHandler to set the default transaction timeout.
     */
    public final void setDefaultTransactionTimeoutInSeconds(int defaultTransactionTimeoutInSeconds) {
        final String prop = "defaultTransactionTimeoutInSeconds";
        final boolean publish = checkDynChange(prop);
        final int oldValue = this.defaultTransactionTimeoutInSeconds;
        this.defaultTransactionTimeoutInSeconds = defaultTransactionTimeoutInSeconds;
        if (publish) {
            firePropertyChange(prop, oldValue, defaultTransactionTimeoutInSeconds);
        }
    }

    /**
     * Get default transaction timeout
     * @return default transaction timeout in seconds
     */
    public final int getDefaultTransactionTimeoutInSeconds() {
        return defaultTransactionTimeoutInSeconds;
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
        final String prop = "monitoring";
        final boolean publish = checkDynChange(prop);
        final Monitoring oldValue = this.monitoring;
        this.monitoring = monitoring;
        if (publish) {
            firePropertyChange(prop, oldValue, monitoring);
        }
        return this;
    }

    /**
     * Allows BeanHandler to set the monitoring flag
     */
    public final void setMonitoring(String monitoring) {
        assertArgumentNotNull("Monitoring", monitoring);
        monitoring(Monitoring.valueOf(Monitoring.class, monitoring.toUpperCase()));
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
        final String prop = "dynamicConfig";
        final boolean publish = checkDynChange(prop);
        final boolean oldValue = this.dynamicConfig;
        this.dynamicConfig = dynamicConfig;
        if (publish) {
            firePropertyChange(prop, oldValue, dynamicConfig);
        }
    }

    /**
     * Get flag for dynamicConfig
     */
    public final boolean getDynamicConfig() {
        return this.dynamicConfig;
    }

    /**
     * Maximum amount of bytes the CacheManager will use on the heap
     * @return amount of bytes, 0 is unbound
     */
    public long getMaxBytesLocalHeap() {
        return maxBytesLocalHeap == null ? DEFAULT_MAX_BYTES_ON_HEAP : maxBytesLocalHeap;
    }

    /**
     * Sets maximum amount of bytes the CacheManager will use on the Disk Tier.
     * @param maxBytesOnHeap String representation of the size.
     * @see MemoryUnit#parseSizeInBytes(String)
     */
    public void setMaxBytesLocalHeap(final String maxBytesOnHeap) {
        assertArgumentNotNull("MaxBytesLocalHeap", maxBytesOnHeap);
        if (isPercentage(maxBytesOnHeap)) {
            long maxMemory = Runtime.getRuntime().maxMemory();
            long mem = maxMemory / HUNDRED * parsePercentage(maxBytesOnHeap);
            setMaxBytesLocalHeap(mem);
        } else {
            setMaxBytesLocalHeap(MemoryUnit.parseSizeInBytes(maxBytesOnHeap));
        }
        maxBytesLocalHeapInput = maxBytesOnHeap;
    }

    /**
     * @return Original input for maxBytesLocalHeap
     */
    public String getMaxBytesLocalHeapAsString() {
        return maxBytesLocalHeapInput != null ? maxBytesLocalHeapInput : Long.toString(getMaxBytesLocalHeap());
    }

    private int parsePercentage(final String stringValue) {
        String trimmed = stringValue.trim();
        int percentage = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
        if (percentage > HUNDRED || percentage < 0) {
            throw new IllegalArgumentException("Percentage need values need to be between 0 and 100 inclusive, but got : " + percentage);
        }
        return percentage;
    }

    private boolean isPercentage(final String stringValue) {
        String trimmed = stringValue.trim();
        return trimmed.charAt(trimmed.length() - 1) == '%';
    }

    /**
     * Sets the maximum amount of bytes the cache manager being configured will use on the OnHeap tier
     * @param maxBytesOnHeap amount of bytes
     */
    public void setMaxBytesLocalHeap(final Long maxBytesOnHeap) {
        final String prop = "maxBytesLocalHeap";
        verifyGreaterThanZero(maxBytesOnHeap, prop);
        final boolean publish = checkDynChange(prop);
        Long oldValue = this.maxBytesLocalHeap;
        this.maxBytesLocalHeap = maxBytesOnHeap;
        if (publish) {
            firePropertyChange(prop, oldValue, maxBytesOnHeap);
        }
    }

    /**
     * Sets the maxOnHeap size for the cache being configured
     * @param amount the amount of unit
     * @param memoryUnit the actual unit
     * @return this
     * @see #setMaxBytesLocalHeap(Long)
     */
    public Configuration maxBytesLocalHeap(final long amount, final MemoryUnit memoryUnit) {
        setMaxBytesLocalHeap(memoryUnit.toBytes(amount));
        return this;
    }

    /**
     * Maximum amount of bytes the CacheManager will use on the OffHeap Tier.
     * @return amount in bytes
     */
    public long getMaxBytesLocalOffHeap() {
        return maxBytesLocalOffHeap == null ? DEFAULT_MAX_BYTES_OFF_HEAP : maxBytesLocalOffHeap;
    }

    /**
     * Sets maximum amount of bytes the CacheManager will use on the OffHeap Tier.
     * @param maxBytesOffHeap String representation of the size.
     * @see MemoryUnit#parseSizeInBytes(String)
     */
    public void setMaxBytesLocalOffHeap(final String maxBytesOffHeap) {
        assertArgumentNotNull("MaxBytesLocalOffHeap", maxBytesOffHeap);
        if (isPercentage(maxBytesOffHeap)) {
            long maxMemory = getOffHeapLimit();
            long mem = maxMemory / HUNDRED * parsePercentage(maxBytesOffHeap);
            setMaxBytesLocalOffHeap(mem);
        } else {
            setMaxBytesLocalOffHeap(MemoryUnit.parseSizeInBytes(maxBytesOffHeap));
        }
        maxBytesLocalOffHeapInput = maxBytesOffHeap;
    }

    /**
     * @return Original input for maxBytesLocalOffHeap
     */
    public String getMaxBytesLocalOffHeapAsString() {
        return maxBytesLocalOffHeapInput != null ? maxBytesLocalOffHeapInput : Long.toString(getMaxBytesLocalOffHeap());
    }

    /**
     * @return Total amount offheap configured by current caches
     */
    public long getTotalConfiguredOffheap() {
        long total = getMaxBytesLocalOffHeap();
        for (String cacheName : getCacheConfigurationsKeySet()) {
            CacheConfiguration config = getCacheConfigurations().get(cacheName);
            total += config.getMaxBytesLocalOffHeap();
        }
        return total;
    }

    private long getOffHeapLimit() {
        try {
            Class<Store> enterpriseFmClass = (Class<Store>) Class.forName(FeaturesManager.ENTERPRISE_FM_CLASSNAME);

            try {
                return (Long)enterpriseFmClass.getMethod("getMaxBytesAllocatable").invoke(null);
            } catch (NoSuchMethodException e) {
                throw new CacheException("Cache: " + getName() + " cannot find static factory"
                                         + " method create(Ehcache, String)" + " in store class " + FeaturesManager.ENTERPRISE_FM_CLASSNAME, e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw new CacheException("Cache: " + getName() + " cannot instantiate store "
                                         + FeaturesManager.ENTERPRISE_FM_CLASSNAME, cause);
            } catch (IllegalAccessException e) {
                throw new CacheException("Cache: " + getName() + " cannot instantiate store "
                                         + FeaturesManager.ENTERPRISE_FM_CLASSNAME, e);
            }
        } catch (ClassNotFoundException e) {
            throw new CacheException("Cache " + getName()
                                     + " cannot be configured because the off-heap store class could not be found. "
                                     + "You must use an enterprise version of Ehcache to successfully enable overflowToOffHeap.");
        }
    }

    /**
     * Sets maximum amount of bytes the CacheManager will use on the OffHeap Tier.
     * @param maxBytesOffHeap max bytes on disk in bytes. Needs be be greater than 0
     */
    public void setMaxBytesLocalOffHeap(final Long maxBytesOffHeap) {
        String prop = "maxBytesLocalOffHeap";
        verifyGreaterThanZero(maxBytesOffHeap, prop);
        boolean publish = checkDynChange(prop);
        Long oldValue = this.maxBytesLocalOffHeap;
        this.maxBytesLocalOffHeap = maxBytesOffHeap;
        if (publish) {
            firePropertyChange(prop, oldValue, maxBytesOffHeap);
        }
    }

    /**
     * Sets the maximum size for the OffHeap tier for all the caches this CacheManagers holds.
     * @param amount the amount of unit
     * @param memoryUnit the actual unit
     * @return this
     */
    public Configuration maxBytesLocalOffHeap(final long amount, final MemoryUnit memoryUnit) {
        setMaxBytesLocalOffHeap(memoryUnit.toBytes(amount));
        return this;
    }

    /**
     * Maximum amount of bytes the CacheManager will use on the Disk Tier.
     * @return amount in bytes
     */
    public long getMaxBytesLocalDisk() {
        return maxBytesLocalDisk == null ? DEFAULT_MAX_BYTES_ON_DISK : maxBytesLocalDisk;
    }

    /**
     * Sets maximum amount of bytes the CacheManager will use on the Disk Tier.
     * @param maxBytesOnDisk String representation of the size.
     * @see MemoryUnit#parseSizeInBytes(String)
     */
    public void setMaxBytesLocalDisk(final String maxBytesOnDisk) {
        assertArgumentNotNull("MaxBytesLocalDisk", maxBytesOnDisk);
        setMaxBytesLocalDisk(MemoryUnit.parseSizeInBytes(maxBytesOnDisk));
        maxBytesLocalDiskInput = maxBytesOnDisk;
    }

    /**
     * @return Original input for maxBytesLocalDisk
     */
    public String getMaxBytesLocalDiskAsString() {
        return maxBytesLocalDiskInput != null ? maxBytesLocalDiskInput : Long.toString(getMaxBytesLocalDisk());
    }

    /**
     * Sets maximum amount of bytes the CacheManager will use on the Disk Tier.
     * @param maxBytesOnDisk max bytes on disk in bytes. Needs be be greater than 0
     */
    public void setMaxBytesLocalDisk(final Long maxBytesOnDisk) {
        String prop = "maxBytesLocalDisk";
        verifyGreaterThanZero(maxBytesOnDisk, prop);
        boolean publish = checkDynChange(prop);
        Long oldValue = this.maxBytesLocalDisk;
        this.maxBytesLocalDisk = maxBytesOnDisk;
        if (publish) {
            firePropertyChange(prop, oldValue, maxBytesOnDisk);
        }
    }

    /**
     * Sets the maxOnDisk size
     * @param amount the amount of unit
     * @param memoryUnit the actual unit
     * @return this
     * @see #setMaxBytesLocalDisk(Long)
     */
    public Configuration maxBytesLocalDisk(final long amount, final MemoryUnit memoryUnit) {
        setMaxBytesLocalDisk(memoryUnit.toBytes(amount));
        return this;
    }

    private void verifyGreaterThanZero(final Long maxBytesOnHeap, final String field) {
        if (maxBytesOnHeap != null && maxBytesOnHeap < 1) {
            throw new IllegalArgumentException(field + " has to be larger than 0");
        }
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
        final String prop = "diskStoreConfiguration";
        boolean publish = checkDynChange(prop);
        DiskStoreConfiguration oldValue = diskStoreConfiguration;
        diskStoreConfiguration = diskStoreConfigurationParameter;
        if (publish) {
            firePropertyChange(prop, oldValue, diskStoreConfiguration);
        }
    }

    /**
     * Builder to set the default SizeOfPolicyConfiguration for this cache manager.
     *
     * @param sizeOfPolicyConfiguration the SizeOfPolicy Configuration
     * @return this configuration instance
     */
    public final Configuration sizeOfPolicy(SizeOfPolicyConfiguration sizeOfPolicyConfiguration) {
        addSizeOfPolicy(sizeOfPolicyConfiguration);
        return this;
    }

    /**
     * Sets the default SizeOfPolicyConfiguration for this cache manager.
     *
     * @param sizeOfPolicy the SizeOfPolicy Configuration
     */
    public final void addSizeOfPolicy(SizeOfPolicyConfiguration sizeOfPolicy) {
        if (sizeOfPolicyConfiguration != null) {
            throw new ObjectExistsException("The SizeOfPolicy class has already been configured");
        }
        sizeOfPolicyConfiguration = sizeOfPolicy;
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
        final String prop = "transactionManagerLookupConfiguration";
        boolean publish = checkDynChange(prop);
        FactoryConfiguration oldValue = this.transactionManagerLookupConfiguration;
        transactionManagerLookupConfiguration = transactionManagerLookupParameter;
        if (publish) {
            firePropertyChange(prop, oldValue, transactionManagerLookupParameter);
        }
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
        final String prop = "cacheManagerEventListenerFactoryConfiguration";
        boolean publish = checkDynChange(prop);
        if (this.cacheManagerEventListenerFactoryConfiguration == null) {
            this.cacheManagerEventListenerFactoryConfiguration = cacheManagerEventListenerFactoryConfiguration;
            if (publish) {
                firePropertyChange(prop, null, cacheManagerEventListenerFactoryConfiguration);
            }
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
        final String prop = "cacheManagerPeerProviderFactoryConfiguration";
        boolean publish = checkDynChange(prop);
        List<FactoryConfiguration> oldValue = null;
        if (publish) {
            oldValue = new ArrayList<FactoryConfiguration>(cacheManagerPeerProviderFactoryConfiguration);
        }
        cacheManagerPeerProviderFactoryConfiguration.add(factory);
        if (publish) {
            firePropertyChange(prop, oldValue, cacheManagerPeerProviderFactoryConfiguration);
        }
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
        final String prop = "cacheManagerPeerListenerFactoryConfiguration";
        boolean publish = checkDynChange(prop);
        List<FactoryConfiguration> oldValue = null;
        if (publish) {
            oldValue = new ArrayList<FactoryConfiguration>(cacheManagerPeerListenerFactoryConfiguration);
        }
        cacheManagerPeerListenerFactoryConfiguration.add(factory);
        if (publish) {
            firePropertyChange(prop, oldValue, cacheManagerPeerListenerFactoryConfiguration);
        }
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
        if (this.terracottaConfigConfiguration != null && terracottaConfiguration != null) {
            throw new ObjectExistsException("The TerracottaConfig has already been configured");
        }
        final String prop = "terracottaConfigConfiguration";
        final boolean publish = checkDynChange(prop);
        final TerracottaClientConfiguration oldValue = this.terracottaConfigConfiguration;
        this.terracottaConfigConfiguration = terracottaConfiguration;
        if (publish) {
            firePropertyChange(prop, oldValue, terracottaConfiguration);
        }
    }

    /**
     * Builder method to REST management capabilities to the cache manager through a dedicated configuration, this can only be used once.
     *
     * @return this configuration instance
     * @throws ObjectExistsException
     *             if the REST management config has already been configured
     */
    public final Configuration managementRESTService(ManagementRESTServiceConfiguration cfg) throws ObjectExistsException {
        addManagementRESTService(cfg);
        return this;
    }

    /**
     * Allows BeanHandler to add a ManagementRESTService configuration to the configuration
     */
    public final void addManagementRESTService(ManagementRESTServiceConfiguration managementRESTServiceConfiguration) throws ObjectExistsException {
        if (this.managementRESTService != null) {
            throw new ObjectExistsException("The ManagementRESTService has already been configured");
        }

        final String prop = "managementRESTService";
        final boolean publish = checkDynChange(prop);
        final ManagementRESTServiceConfiguration oldValue = this.managementRESTService;
        this.managementRESTService = managementRESTServiceConfiguration;
        if (publish) {
            firePropertyChange(prop, oldValue, managementRESTServiceConfiguration);
        }
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
        setDefaultCacheConfiguration(defaultCacheConfiguration);
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
        addCache(cacheConfiguration, true);
    }

    /**
     * Maintains the known Cache's configuration map in this Configuration
     * @param cacheConfiguration the CacheConfiguration
     * @param strict true if added regularly, validation dyn config constraints, false if added through the cache being added
     */
    void addCache(CacheConfiguration cacheConfiguration, final boolean strict) throws ObjectExistsException {
        final String prop = "cacheConfigurations";
        Object oldValue = null;
        boolean publishChange = strict && checkDynChange(prop);
        if (publishChange) {
            oldValue = new HashMap<String, CacheConfiguration>(cacheConfigurations);
        }
        if (cacheConfigurations.get(cacheConfiguration.name) != null) {
            throw new ObjectExistsException("Cannot create cache: " + cacheConfiguration.name + " with the same name as an existing one.");
        }
        if (cacheConfiguration.name.equalsIgnoreCase(net.sf.ehcache.Cache.DEFAULT_CACHE_NAME)) {
            throw new ObjectExistsException("The Default Cache has already been configured");
        }

        cacheConfigurations.put(cacheConfiguration.name, cacheConfiguration);
        if (publishChange) {
            firePropertyChange(prop, oldValue, cacheConfigurations);
        }
    }

    private boolean checkDynChange(final String prop) {
        if (!propertyChangeListeners.isEmpty()) {
            try {
                if (cfg != null) {
                    DynamicProperty.valueOf(prop);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(this.getClass().getName() + "." + prop + " can't be changed dynamically");
            }
            return true;
        } else {
            return false;
        }
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
        final String prop = "defaultCacheConfiguration";
        final boolean publish = checkDynChange(prop);
        final CacheConfiguration oldValue = this.defaultCacheConfiguration;
        this.defaultCacheConfiguration = defaultCacheConfiguration;
        if (publish) {
            firePropertyChange(prop, oldValue, defaultCacheConfiguration);
        }
    }

    /**
     * Gets the disk store configuration.
     */
    public final DiskStoreConfiguration getDiskStoreConfiguration() {
        return diskStoreConfiguration;
    }

    /**
     * Gets the SizeOf policy configuration.
     */
    public final SizeOfPolicyConfiguration getSizeOfPolicyConfiguration() {
        if (sizeOfPolicyConfiguration == null) {
            return DEFAULT_SIZEOF_POLICY_CONFIGURATION;
        }
        return sizeOfPolicyConfiguration;
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
     * Gets the ManagementRESTServiceConfiguration
     */
    public final ManagementRESTServiceConfiguration getManagementRESTService() {
        return managementRESTService;
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
        final String prop = "configurationSource";
        final boolean publish = checkDynChange(prop);
        final ConfigurationSource oldValue = this.configurationSource;
        this.configurationSource = configurationSource;
        if (publish) {
            firePropertyChange(prop, oldValue, configurationSource);
        }
    }

    /**
     * Gets a description of the source from which this configuration was created.
     */
    public final ConfigurationSource getConfigurationSource() {
        return configurationSource;
    }

    /**
     * Adds a {@link PropertyChangeListener} for this configuration
     * @param listener the listener instance
     * @return true if added, false otherwise
     */
    public boolean addPropertyChangeListener(final PropertyChangeListener listener) {
        return this.propertyChangeListeners.add(listener);
    }

    /**
     * Removes a {@link PropertyChangeListener} for this configuration
     * @param listener the listener to be removed
     * @return true if removed, false otherwise
     */
    public boolean removePropertyChangeListener(final PropertyChangeListener listener) {
        return this.propertyChangeListeners.remove(listener);
    }

    private <T> void firePropertyChange(final String prop, final T oldValue, final T newValue) {
        if ((oldValue != null && !oldValue.equals(newValue)) || newValue != null) {
            for (PropertyChangeListener propertyChangeListener : propertyChangeListeners) {
                propertyChangeListener.propertyChange(new PropertyChangeEvent(Configuration.this, prop, oldValue, newValue));
            }
        }
    }

    /**
     * Runtime configuration as being used by the CacheManager
     */
    public class RuntimeCfg implements PropertyChangeListener {

        private final CacheManager cacheManager;
        private volatile String cacheManagerName;
        private final boolean named;
        private TransactionManagerLookup transactionManagerLookup;
        private boolean allowsSizeBasedTunings;

        /**
         * Constructor
         * @param cacheManager the cacheManager instance using this config
         * @param fallbackName the fallbackName in case the configuration doesn't declare an explicit name
         */
        public RuntimeCfg(final CacheManager cacheManager, final String fallbackName) {
            if (Configuration.this.cacheManagerName != null) {
                this.cacheManagerName = Configuration.this.cacheManagerName;
                named = true;
            } else if (hasTerracottaClusteredCaches()) {
                this.cacheManagerName = CacheManager.DEFAULT_NAME;
                named = false;
            } else {
                this.cacheManagerName = fallbackName;
                named = false;
            }
            FactoryConfiguration lookupConfiguration = getTransactionManagerLookupConfiguration();
            try {
                Properties properties = PropertyUtil.parseProperties(lookupConfiguration.getProperties(),
                        lookupConfiguration.getPropertySeparator());
                
                ClassLoader loader = getClassLoader(); 
                
                // when loading the default impl use the same loader as ehcache itself
                if (DEFAULT_TRANSACTION_MANAGER_LOOKUP_CONFIG.getFullyQualifiedClassPath().equals(lookupConfiguration.getFullyQualifiedClassPath())) {                                                                      
                    loader = getClass().getClassLoader();
                }                
                
                Class<? extends TransactionManagerLookup> transactionManagerLookupClass = (Class<? extends TransactionManagerLookup>) loader
                        .loadClass(lookupConfiguration.getFullyQualifiedClassPath());
                this.transactionManagerLookup = transactionManagerLookupClass.newInstance();
                this.transactionManagerLookup.setProperties(properties);
            } catch (Exception e) {
                LOG.error("could not instantiate transaction manager lookup class: {}", lookupConfiguration.getFullyQualifiedClassPath(), e);
            }
            this.cacheManager = cacheManager;
            propertyChangeListeners.add(this);
            allowsSizeBasedTunings = defaultCacheConfiguration == null || !defaultCacheConfiguration.isCountBasedTuned();
            for (CacheConfiguration cacheConfiguration : cacheConfigurations.values()) {
                if (cacheConfiguration.isCountBasedTuned()) {
                    allowsSizeBasedTunings = false;
                    break;
                }
            }
        }

        /**
         * @return the CacheManager's name
         */
        public String getCacheManagerName() {
            return cacheManagerName;
        }

        /**
         * @return Whether dynamic config changes are available
         */
        public boolean allowsDynamicCacheConfig() {
            return getDynamicConfig();
        }

        /**
         * @return Whether the CacheManager is explicitly named
         */
        public boolean isNamed() {
            return named;
        }

        /**
         * @return the underlying Configuration instance
         */
        public Configuration getConfiguration() {
            return Configuration.this;
        }

        /**
         * @return Whether terracotta clustering is being used and rejoin is enabled
         */
        public boolean isTerracottaRejoin() {
            TerracottaClientConfiguration terracottaConfiguration = getTerracottaConfiguration();
            return terracottaConfiguration != null && terracottaConfiguration.isRejoin();
        }

        private boolean hasTerracottaClusteredCaches() {
            if (defaultCacheConfiguration != null
                    && defaultCacheConfiguration.isTerracottaClustered()) {
                return true;
            } else {
                for (CacheConfiguration config : cacheConfigurations.values()) {
                    if (config.isTerracottaClustered()) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * @return The transactionManagerLookup instance
         */
        public TransactionManagerLookup getTransactionManagerLookup() {
            return transactionManagerLookup;
        }

        /**
         * Removes a cache from the known list
         * @param cacheConfiguration the cacheConfiguration to be removed
         */
        public void removeCache(final CacheConfiguration cacheConfiguration) {
            if (cacheManager.getOnHeapPool() != null) {
                cacheManager.getOnHeapPool().setMaxSize(cacheManager.getOnHeapPool()
                                                            .getMaxSize() + cacheConfiguration.getMaxBytesLocalHeap());
            }
            if (cacheManager.getOnDiskPool() != null) {
                cacheManager.getOnDiskPool().setMaxSize(cacheManager.getOnDiskPool()
                                                            .getMaxSize() + cacheConfiguration.getMaxBytesLocalDisk());
            }
            getConfiguration().getCacheConfigurations().remove(cacheConfiguration.getName());
        }

        /**
         * Handles changes to the Configuration this RuntimeCfg backs
         * @param evt the PropertyChangeEvent
         */
        public void propertyChange(final PropertyChangeEvent evt) {
            try {
                DynamicProperty.valueOf(evt.getPropertyName()).applyChange(evt, this);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(evt.getPropertyName() + " can't be changed dynamically");
            }
        }

        /**
         * Checks whether the CacheManager uses a OffHeapPool
         * @return true if using one, false otherwise
         */
        public boolean hasOffHeapPool() {
            return isMaxBytesLocalOffHeapSet();
        }
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Set the classloader for the cache manager (and it's associated caches) to use when creating application objects (eg. cache values,
     * event listeners, etc). The default classloading behavior is to prefer Thread.currentThread().getContextClassLoader() and fallback
     * to the classloader that loaded ehcache itself. 
     * 
     * @param loader the classloader to use
     */
    public void setClassLoader(ClassLoader loader) {
        this.classLoader = loader;
    }

    public void cleanup() {
        propertyChangeListeners.remove(cfg);
        cfg = null;
    }
}
