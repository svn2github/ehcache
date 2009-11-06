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


package net.sf.ehcache;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.ConfigurationHelper;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.TerracottaConfigConfiguration;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.CacheManagerEventListenerRegistry;
import net.sf.ehcache.management.provider.MBeanRegistrationProvider;
import net.sf.ehcache.management.provider.MBeanRegistrationProviderException;
import net.sf.ehcache.management.provider.MBeanRegistrationProviderFactory;
import net.sf.ehcache.management.provider.MBeanRegistrationProviderFactoryImpl;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreFactory;
import net.sf.ehcache.util.FailSafeTimer;
import net.sf.ehcache.util.PropertyUtil;
import net.sf.ehcache.util.UpdateChecker;

/**
 * A container for {@link Ehcache}s that maintain all aspects of their lifecycle.
 * <p/>
 * CacheManager may be either be a singleton if created with factory methods, or multiple instances may exist,
 * in which case resources required by each must be unique.
 * <p/>
 * A CacheManager holds references to Caches and Ehcaches and manages their creation and lifecycle.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheManager {

    /**
     * Keeps track of all known CacheManagers. Used to check on conflicts.
     * CacheManagers should remove themselves from this list during shut down.
     */
    public static final List<CacheManager> ALL_CACHE_MANAGERS = new CopyOnWriteArrayList<CacheManager>();


    /**
     * System property to enable creation of a shutdown hook for CacheManager.
     */
    public static final String ENABLE_SHUTDOWN_HOOK_PROPERTY = "net.sf.ehcache.enableShutdownHook";

    private static final Logger LOG = LoggerFactory.getLogger(CacheManager.class);

    /**
     * Update check interval - one week in milliseconds
     */
    private static final long EVERY_WEEK = 7 * 24 * 60 * 60 * 1000;

    /**
     * The Singleton Instance.
     */
    private static volatile CacheManager singleton;
    
    /**
     * The factory to use for creating MBeanRegistrationProvider's
     */
    private static MBeanRegistrationProviderFactory mBeanRegistrationProviderFactory = new MBeanRegistrationProviderFactoryImpl();

    /**
     * Ehcaches managed by this manager.
     */
    protected final Map ehcaches = new ConcurrentHashMap();

    /**
     * Caches managed by this manager. A Cache is also an Ehcache.
     * For central managment the cache is also in the ehcaches map.
     */
    protected final Map caches = new ConcurrentHashMap();

    /**
     * A name for this CacheManager to distinguish it from others.
     */
    protected String name;

    /**
     * Status of the Cache Manager
     */
    protected Status status;

    /**
     * The map of providers
     */
    protected Map<String, CacheManagerPeerProvider> cacheManagerPeerProviders = new ConcurrentHashMap<String, CacheManagerPeerProvider>();

    /**
     * The map of listeners
     */
    protected Map<String, CacheManagerPeerListener> cacheManagerPeerListeners = new ConcurrentHashMap<String, CacheManagerPeerListener>();

    /**
     * The listener registry
     */
    protected CacheManagerEventListenerRegistry cacheManagerEventListenerRegistry = new CacheManagerEventListenerRegistry();

    /**
     * The shutdown hook thread for CacheManager. This ensures that the CacheManager and Caches are left in a
     * consistent state on a CTRL-C or kill.
     * <p/>
     * This thread must be unregistered as a shutdown hook, when the CacheManager is disposed.
     * Otherwise the CacheManager is not GC-able.
     * <p/>
     * Of course kill -9 or abrupt termination will not run the shutdown hook. In this case, various
     * sanity checks are made at start up.
     */
    protected Thread shutdownHook;

    /**
     * Default cache cache.
     */
    private Ehcache defaultCache;

    /**
     * The path for the directory in which disk caches are created.
     */
    private String diskStorePath;
    
    private MBeanRegistrationProvider mbeanRegistrationProvider;

    private FailSafeTimer cacheManagerTimer;

    /**
     * Factory for creating terracotta clustered memory store (may be null if this manager has no terracotta caches)
     */
    private volatile StoreFactory terracottaStoreFactory;
    
    /**
     * The {@link TerracottaConfigConfiguration} used for this {@link CacheManager}
     */
    private TerracottaConfigConfiguration terracottaConfigConfiguration;
    
    private AtomicBoolean terracottaStoreFactoryCreated = new AtomicBoolean(false);

    /**
     * An constructor for CacheManager, which takes a configuration object, rather than one created by parsing
     * an ehcache.xml file. This constructor gives complete control over the creation of the CacheManager.
     * <p/>
     * Care should be taken to ensure that, if multiple CacheManages are created, they do now overwrite each others
     * disk store files, as would happend if two were created which used the same diskStore path.
     * <p/>
     * This method does not act as a singleton. Callers must maintain their own reference to it.
     * <p/>
     * Note that if one of the {@link #create()}  methods are called, a new singleton instance will be created,
     * separate from any instances created in this method.
     *
     * @param configuration
     * @throws CacheException
     */
    public CacheManager(Configuration configuration) throws CacheException {
        status = Status.STATUS_UNINITIALISED;
        init(configuration, null, null, null);
    }

    /**
     * An ordinary constructor for CacheManager.
     * This method does not act as a singleton. Callers must maintain a reference to it.
     * Note that if one of the {@link #create()}  methods are called, a new singleton will be created,
     * separate from any instances created in this method.
     *
     * @param configurationFileName an xml configuration file available through a file name. The configuration
     *                              {@link File} is created
     *                              using new <code>File(configurationFileName)</code>
     * @throws CacheException
     * @see #create(String)
     */
    public CacheManager(String configurationFileName) throws CacheException {
        status = Status.STATUS_UNINITIALISED;
        init(null, configurationFileName, null, null);
    }

    /**
     * An ordinary constructor for CacheManager.
     * This method does not act as a singleton. Callers must maintain a reference to it.
     * Note that if one of the {@link #create()}  methods are called, a new singleton will be created,
     * separate from any instances created in this method.
     * <p/>
     * This method can be used to specify a configuration resource in the classpath other
     * than the default of \"/ehcache.xml\":
     * <pre>
     * URL url = this.getClass().getResource("/ehcache-2.xml");
     * </pre>
     * Note that {@link Class#getResource} will look for resources in the same package unless a leading "/"
     * is used, in which case it will look in the root of the classpath.
     * <p/>
     * You can also load a resource using other class loaders. e.g. {@link Thread#getContextClassLoader()}
     *
     * @param configurationURL an xml configuration available through a URL.
     * @throws CacheException
     * @see #create(java.net.URL)
     * @since 1.2
     */
    public CacheManager(URL configurationURL) throws CacheException {
        status = Status.STATUS_UNINITIALISED;
        init(null, null, configurationURL, null);
    }

    /**
     * An ordinary constructor for CacheManager.
     * This method does not act as a singleton. Callers must maintain a reference to it.
     * Note that if one of the {@link #create()}  methods are called, a new singleton will be created,
     * separate from any instances created in this method.
     *
     * @param configurationInputStream an xml configuration file available through an inputstream
     * @throws CacheException
     * @see #create(java.io.InputStream)
     */
    public CacheManager(InputStream configurationInputStream) throws CacheException {
        status = Status.STATUS_UNINITIALISED;
        init(null, null, null, configurationInputStream);
    }

    /**
     * Constructor.
     *
     * @throws CacheException
     */
    public CacheManager() throws CacheException {
        //default config will be done
        status = Status.STATUS_UNINITIALISED;
        init(null, null, null, null);
    }

    /**
     * initialises the CacheManager
     */
    protected void init(Configuration configuration, String configurationFileName, URL configurationURL,
                        InputStream configurationInputStream) {
        Configuration localConfiguration = configuration;
        if (configuration == null) {
            localConfiguration = parseConfiguration(configurationFileName, configurationURL, configurationInputStream);
        } else {
            localConfiguration.setSource("Programmatically configured.");
        }

        if (localConfiguration.getName() != null) {
            this.name = localConfiguration.getName();
        }
        
        this.terracottaConfigConfiguration = localConfiguration.getTerracottaConfiguration();

        Map<String, CacheConfiguration> cacheConfigs = localConfiguration.getCacheConfigurations();
        for (CacheConfiguration config : cacheConfigs.values()) {
            if (config.isTerracottaClustered()) {
                terracottaStoreFactory = TerracottaStoreHelper.newStoreFactory(
                        cacheConfigs, localConfiguration
                                .getTerracottaConfiguration());
                break;
            }
        }
        
        /*
         * May not have any CacheConfigurations yet, so check the default configuration.
         */
        if (terracottaStoreFactory == null) {
            if (localConfiguration.getDefaultCacheConfiguration().isTerracottaClustered()) {
                terracottaStoreFactory = TerracottaStoreHelper.newStoreFactory(cacheConfigs, localConfiguration
                        .getTerracottaConfiguration());
            }
        }
        
        ConfigurationHelper configurationHelper = new ConfigurationHelper(this, localConfiguration);
        configure(configurationHelper);
        status = Status.STATUS_ALIVE;

        for (CacheManagerPeerProvider cacheManagerPeerProvider : cacheManagerPeerProviders.values()) {
            cacheManagerPeerProvider.init();
        }

        cacheManagerEventListenerRegistry.init();
        addShutdownHookIfRequired();

        cacheManagerTimer = new FailSafeTimer(getName());
        checkForUpdateIfNeeded(localConfiguration.getUpdateCheck());
        
        terracottaStoreFactoryCreated.set(terracottaStoreFactory != null);

        //do this last
        addConfiguredCaches(configurationHelper);

        initializeMBeanRegistrationProvider(localConfiguration);
    }

    /**
     * Initialize the {@link MBeanRegistrationProvider} for this {@link CacheManager}
     * @param localConfiguration
     */
    private void initializeMBeanRegistrationProvider(Configuration localConfiguration) {
        mbeanRegistrationProvider = mBeanRegistrationProviderFactory.createMBeanRegistrationProvider(localConfiguration);
        try {
            mbeanRegistrationProvider.initialize(this);
        } catch (MBeanRegistrationProviderException e) {
            LOG.warn("Failed to initialize the MBeanRegistrationProvider - " + mbeanRegistrationProvider.getClass().getName(), e);
        }
    }

    /**
     * Create/access the appropriate terracotta clustered store for the given cache
     *
     * @param cache The cache for which the Store should be created
     * @return a new (or existing) clustered store
     */
    Store createTerracottaStore(Ehcache cache) {
        if (terracottaStoreFactory == null) {
            // adding a cache programmatically when there is no clustered store defined in the configuration
            // at the time this cacheManager was created
            // synchronized so that multiple threads will wait till the store is created
            synchronized (this) {
                // only 1 thread will create the store
                if (!terracottaStoreFactoryCreated.getAndSet(true)) {
                    // use the TerracottaConfigConfiguration of this CacheManager to create a new StoreFactory
                    Map<String, CacheConfiguration> map = new HashMap<String, CacheConfiguration>(1);
                    map.put(cache.getName(), cache.getCacheConfiguration());
                    terracottaStoreFactory = TerracottaStoreHelper.newStoreFactory(map, terracottaConfigConfiguration);
                    try {
                        mbeanRegistrationProvider.reinitialize();
                    } catch (MBeanRegistrationProviderException e) {
                        LOG.warn("Failed to initialize the MBeanRegistrationProvider - "
                                + mbeanRegistrationProvider.getClass().getName(), e);
                    }
                }
            }
        }
        return terracottaStoreFactory.create(cache);
    }

    private void checkForUpdateIfNeeded(boolean updateCheckNeeded) {
        try {
            if (updateCheckNeeded) {
                UpdateChecker updateChecker = new UpdateChecker(terracottaStoreFactory.getTCVersion());
                cacheManagerTimer.scheduleAtFixedRate(updateChecker, 1,
                        EVERY_WEEK);
            }
        } catch (Throwable t) {
            LOG.warn("Failed to set up update checker: " + t.toString());
        }
    }

    /**
     * Loads configuration, either from the supplied {@link ConfigurationHelper} or by creating a new Configuration instance
     * from the configuration file referred to by file, inputstream or URL.
     * <p/>
     * Should only be called once.
     *
     * @param configurationFileName     the file name to parse, or null
     * @param configurationURL          the URL to pass, or null
     * @param configurationInputStream, the InputStream to parse, or null
     * @return the loaded configuration
     * @throws CacheException if the configuration cannot be parsed
     */
    private synchronized Configuration parseConfiguration(String configurationFileName, URL configurationURL,
                                                          InputStream configurationInputStream) throws CacheException {
        reinitialisationCheck();
        Configuration configuration;
        String configurationSource;
        if (configurationFileName != null) {

                LOG.debug("Configuring CacheManager from {}", configurationFileName);
            configuration = ConfigurationFactory.parseConfiguration(new File(configurationFileName));
            configurationSource = "file located at " + configurationFileName;
        } else if (configurationURL != null) {
            configuration = ConfigurationFactory.parseConfiguration(configurationURL);
            configurationSource = "URL of " + configurationURL;
        } else if (configurationInputStream != null) {
            configuration = ConfigurationFactory.parseConfiguration(configurationInputStream);
            configurationSource = "InputStream " + configurationInputStream;
        } else {

                LOG.debug("Configuring ehcache from classpath.");
            configuration = ConfigurationFactory.parseConfiguration();
            configurationSource = "classpath";
        }
        configuration.setSource(configurationSource);
        return configuration;

    }

    private void configure(ConfigurationHelper configurationHelper) {

        diskStorePath = configurationHelper.getDiskStorePath();
        int cachesRequiringDiskStores = configurationHelper.numberOfCachesThatOverflowToDisk().intValue()
                + configurationHelper.numberOfCachesThatAreDiskPersistent().intValue();

        if (diskStorePath == null && cachesRequiringDiskStores > 0) {
            diskStorePath = DiskStoreConfiguration.getDefaultPath();
            LOG.warn("One or more caches require a DiskStore but there is no diskStore element configured." +
                    " Using the default disk store path of " + DiskStoreConfiguration.getDefaultPath() +
                    ". Please explicitly configure the diskStore element in ehcache.xml.");
        }

        detectAndFixDiskStorePathConflict(configurationHelper);

        cacheManagerEventListenerRegistry.registerListener(configurationHelper.createCacheManagerEventListener());

        cacheManagerPeerListeners = configurationHelper.createCachePeerListeners();
        for (CacheManagerPeerListener cacheManagerPeerListener : cacheManagerPeerListeners.values()) {
            cacheManagerEventListenerRegistry.registerListener(cacheManagerPeerListener);
        }

        detectAndFixCacheManagerPeerListenerConflict(configurationHelper);

        ALL_CACHE_MANAGERS.add(this);

        cacheManagerPeerProviders = configurationHelper.createCachePeerProviders();

        defaultCache = configurationHelper.createDefaultCache();

    }

    private void detectAndFixDiskStorePathConflict(ConfigurationHelper configurationHelper) {
        if (diskStorePath == null) {

                LOG.debug("No disk store path defined. Skipping disk store path conflict test.");
            return;
        }

        for (CacheManager cacheManager : ALL_CACHE_MANAGERS) {
            if (diskStorePath.equals(cacheManager.diskStorePath)) {
                String newDiskStorePath = diskStorePath + File.separator + DiskStore.generateUniqueDirectory();
                LOG.warn("Creating a new instance of CacheManager using the diskStorePath \""
                        + diskStorePath + "\" which is already used" +
                        " by an existing CacheManager.\nThe source of the configuration was "
                        + configurationHelper.getConfigurationBean().getConfigurationSource() + ".\n" +
                        "The diskStore path for this CacheManager will be set to " + newDiskStorePath + ".\nTo avoid this" +
                        " warning consider using the CacheManager factory methods to create a singleton CacheManager " +
                        "or specifying a separate ehcache configuration (ehcache.xml) for each CacheManager instance.");
                diskStorePath = newDiskStorePath;
                break;
            }

        }
    }

    private void detectAndFixCacheManagerPeerListenerConflict(ConfigurationHelper configurationHelper) {
        if (cacheManagerPeerListeners == null) {
            return;
        }
        for (CacheManagerPeerListener cacheManagerPeerListener : cacheManagerPeerListeners.values()) {
            String uniqueResourceIdentifier = cacheManagerPeerListener.getUniqueResourceIdentifier();
            for (CacheManager cacheManager : ALL_CACHE_MANAGERS) {
                for (CacheManagerPeerListener otherCacheManagerPeerListener : cacheManager.cacheManagerPeerListeners.values()) {
                    if (otherCacheManagerPeerListener == null) {
                        continue;
                    }
                    String otherUniqueResourceIdentifier = otherCacheManagerPeerListener.getUniqueResourceIdentifier();
                    if (uniqueResourceIdentifier.equals(otherUniqueResourceIdentifier)) {
                        LOG.warn("Creating a new instance of CacheManager with a CacheManagerPeerListener which " +
                                "has a conflict on a resource that must be unique.\n" +
                                "The resource is " + uniqueResourceIdentifier + ".\n" +
                                "Attempting automatic resolution. The source of the configuration was "
                                + configurationHelper.getConfigurationBean().getConfigurationSource() + ".\n"
                                + "To avoid this warning consider using the CacheManager factory methods to create a " +
                                "singleton CacheManager " +
                                "or specifying a separate ehcache configuration (ehcache.xml) for each CacheManager instance.");
                        cacheManagerPeerListener.attemptResolutionOfUniqueResourceConflict();
                        break;
                    }
                }

            }
        }
    }

    private void addConfiguredCaches(ConfigurationHelper configurationHelper) {
        Set unitialisedCaches = configurationHelper.createCaches();
        for (Iterator iterator = unitialisedCaches.iterator(); iterator.hasNext();) {
            Ehcache unitialisedCache = (Ehcache) iterator.next();
            addCacheNoCheck(unitialisedCache);
        }
    }

    private void reinitialisationCheck() throws IllegalStateException {
        if (defaultCache != null || diskStorePath != null || ehcaches.size() != 0
                || status.equals(Status.STATUS_SHUTDOWN)) {
            throw new IllegalStateException("Attempt to reinitialise the CacheManager");
        }
    }

    /**
     * A factory method to create a singleton CacheManager with default config, or return it if it exists.
     * <p/>
     * The configuration will be read, {@link Ehcache}s created and required stores initialized.
     * When the {@link CacheManager} is no longer required, call shutdown to free resources.
     *
     * @return the singleton CacheManager
     * @throws CacheException if the CacheManager cannot be created
     */
    public static CacheManager create() throws CacheException {
        if (singleton != null) {
            return singleton;
        }
        synchronized (CacheManager.class) {
            if (singleton == null) {

                    LOG.debug("Creating new CacheManager with default config");
                singleton = new CacheManager();
            } else {

                    LOG.debug("Attempting to create an existing singleton. Existing singleton returned.");
            }
            return singleton;
        }
    }

    /**
     * A factory method to create a singleton CacheManager with default config, or return it if it exists.
     * <p/>
     * This has the same effect as {@link CacheManager#create}
     * <p/>
     * Same as {@link #create()}
     *
     * @return the singleton CacheManager
     * @throws CacheException if the CacheManager cannot be created
     */
    public static CacheManager getInstance() throws CacheException {
        return CacheManager.create();
    }

    /**
     * A factory method to create a singleton CacheManager with a specified configuration.
     *
     * @param configurationFileName an xml file compliant with the ehcache.xsd schema
     *                              <p/>
     *                              The configuration will be read, {@link Ehcache}s created and required stores initialized.
     *                              When the {@link CacheManager} is no longer required, call shutdown to free resources.
     */
    public static CacheManager create(String configurationFileName) throws CacheException {
        if (singleton != null) {
            return singleton;
        }
        synchronized (CacheManager.class) {
            if (singleton == null) {

                    LOG.debug("Creating new CacheManager with config file: {}", configurationFileName);
                singleton = new CacheManager(configurationFileName);
            }
            return singleton;
        }
    }

    /**
     * A factory method to create a singleton CacheManager from an URL.
     * <p/>
     * This method can be used to specify a configuration resource in the classpath other
     * than the default of \"/ehcache.xml\":
     * This method can be used to specify a configuration resource in the classpath other
     * than the default of \"/ehcache.xml\":
     * <pre>
     * URL url = this.getClass().getResource("/ehcache-2.xml");
     * </pre>
     * Note that {@link Class#getResource} will look for resources in the same package unless a leading "/"
     * is used, in which case it will look in the root of the classpath.
     * <p/>
     * You can also load a resource using other class loaders. e.g. {@link Thread#getContextClassLoader()}
     *
     * @param configurationFileURL an URL to an xml file compliant with the ehcache.xsd schema
     *                             <p/>
     *                             The configuration will be read, {@link Ehcache}s created and required stores initialized.
     *                             When the {@link CacheManager} is no longer required, call shutdown to free resources.
     */
    public static CacheManager create(URL configurationFileURL) throws CacheException {
        if (singleton != null) {
            return singleton;
        }
        synchronized (CacheManager.class) {
            if (singleton == null) {

                    LOG.debug("Creating new CacheManager with config URL: {}", configurationFileURL);
                singleton = new CacheManager(configurationFileURL);

            }
            return singleton;
        }
    }

    /**
     * A factory method to create a singleton CacheManager from a java.io.InputStream.
     * <p/>
     * This method makes it possible to use an inputstream for configuration.
     * Note: it is the clients responsibility to close the inputstream.
     * <p/>
     *
     * @param inputStream InputStream of xml compliant with the ehcache.xsd schema
     *                    <p/>
     *                    The configuration will be read, {@link Ehcache}s created and required stores initialized.
     *                    When the {@link CacheManager} is no longer required, call shutdown to free resources.
     */
    public static CacheManager create(InputStream inputStream) throws CacheException {
        if (singleton != null) {
            return singleton;
        }
        synchronized (CacheManager.class) {
            if (singleton == null) {

                    LOG.debug("Creating new CacheManager with InputStream");
                singleton = new CacheManager(inputStream);
            }
            return singleton;
        }
    }

    /**
     * Returns a concrete implementation of Cache, it it is available in the CacheManager.
     * Consider using getEhcache(String name) instead, which will return decorated caches that are registered.
     * <p/>
     * If a decorated ehcache is registered in CacheManager, an undecorated Cache with the same name will also exist.
     *
     * @return a Cache, if an object of that type exists by that name, else null
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #getEhcache(String)
     */
    public Cache getCache(String name) throws IllegalStateException, ClassCastException {
        checkStatus();
        return (Cache) caches.get(name);
    }

    /**
     * Gets an Ehcache
     * <p/>
     *
     * @return a Cache, if an object of type Cache exists by that name, else null
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public Ehcache getEhcache(String name) throws IllegalStateException {
        checkStatus();
        return (Ehcache) ehcaches.get(name);
    }


    /**
     * Some caches might be persistent, so we want to add a shutdown hook if that is the
     * case, so that the data and index can be written to disk.
     */
    private void addShutdownHookIfRequired() {

        String shutdownHookProperty = System.getProperty(ENABLE_SHUTDOWN_HOOK_PROPERTY);
        boolean enabled = PropertyUtil.parseBoolean(shutdownHookProperty);
        if (!enabled) {
            return;
        } else {
            LOG.info("The CacheManager shutdown hook is enabled because {} is set to true.", ENABLE_SHUTDOWN_HOOK_PROPERTY);

            Thread localShutdownHook = new Thread() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (status.equals(Status.STATUS_ALIVE)) {
                            // clear shutdown hook reference to prevent
                            // removeShutdownHook to remove it during shutdown
                            shutdownHook = null;

                                LOG.info("VM shutting down with the CacheManager still active. Calling shutdown.");
                            shutdown();
                        }
                    }
                }
            };

            Runtime.getRuntime().addShutdownHook(localShutdownHook);
            shutdownHook = localShutdownHook;
        }
    }

    /**
     * Remove the shutdown hook to prevent leaving orphaned CacheManagers around. This
     * is called by {@link #shutdown()}} AFTER the status has been set to shutdown.
     */
    private void removeShutdownHook() {
        if (shutdownHook != null) {
            // remove shutdown hook
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                //This will be thrown if the VM is shutting down. In this case
                //we do not need to worry about leaving references to CacheManagers lying
                //around and the call is ok to fail.

                    LOG.debug("IllegalStateException due to attempt to remove a shutdown" +
                            "hook while the VM is actually shutting down.", e);
            }
            shutdownHook = null;
        }
    }

    /**
     * Adds a {@link Ehcache} based on the defaultCache with the given name.
     * <p/>
     * Memory and Disk stores will be configured for it and it will be added
     * to the map of caches.
     * <p/>
     * Also notifies the CacheManagerEventListener after the cache was initialised and added.
     * <p/>
     * It will be created with the defaultCache attributes specified in ehcache.xml
     *
     * @param cacheName the name for the cache
     * @throws ObjectExistsException if the cache already exists
     * @throws CacheException        if there was an error creating the cache.
     */
    public void addCache(String cacheName) throws IllegalStateException,
            ObjectExistsException, CacheException {
        checkStatus();

        //NPE guard
        if (cacheName == null || cacheName.length() == 0) {
            return;
        }

        if (ehcaches.get(cacheName) != null) {
            throw new ObjectExistsException("Cache " + cacheName + " already exists");
        }
        Ehcache cache = null;
        try {
            cache = (Ehcache) defaultCache.clone();
        } catch (CloneNotSupportedException e) {
            throw new CacheException("Failure adding cache. Initial cause was " + e.getMessage(), e);
        }
        if (cache != null) {
            cache.setName(cacheName);
        }
        addCache(cache);
    }

    /**
     * Adds a {@link Cache} to the CacheManager.
     * <p/>
     * Memory and Disk stores will be configured for it and it will be added to the map of caches.
     * Also notifies the CacheManagerEventListener after the cache was initialised and added.
     *
     * @param cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_UNINITIALISED} before this method is called.
     * @throws ObjectExistsException if the cache already exists in the CacheManager
     * @throws CacheException        if there was an error adding the cache to the CacheManager
     */
    public void addCache(Cache cache) throws IllegalStateException,
            ObjectExistsException, CacheException {
        checkStatus();
        if (cache == null) {
            return;
        }
        addCache((Ehcache) cache);
        caches.put(cache.getName(), cache);
    }


    /**
     * Adds an {@link Ehcache} to the CacheManager.
     * <p/>
     * Memory and Disk stores will be configured for it and it will be added to the map of caches.
     * Also notifies the CacheManagerEventListener after the cache was initialised and added.
     *
     * @param cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_UNINITIALISED} before this method is called.
     * @throws ObjectExistsException if the cache already exists in the CacheManager
     * @throws CacheException        if there was an error adding the cache to the CacheManager
     */
    public void addCache(Ehcache cache) throws IllegalStateException,
            ObjectExistsException, CacheException {
        checkStatus();
        if (cache == null) {
            return;
        }
        addCacheNoCheck(cache);
    }

    private void addCacheNoCheck(Ehcache cache) throws IllegalStateException,
            ObjectExistsException, CacheException {
        if (ehcaches.get(cache.getName()) != null) {
            throw new ObjectExistsException("Cache " + cache.getName() + " already exists");
        }
        cache.setCacheManager(this);
        cache.setDiskStorePath(diskStorePath);
        cache.initialise();
        try {
            cache.bootstrap();
        } catch (CacheException e) {
            LOG.warn("Cache " + cache.getName() + "requested bootstrap but a CacheException occured. " + e.getMessage(), e);
        }
        ehcaches.put(cache.getName(), cache);
        if (cache instanceof Cache) {
            caches.put(cache.getName(), cache);
        }

        //Don't notify initial config. The init method of each listener should take care of this.
        if (status.equals(Status.STATUS_ALIVE)) {
            cacheManagerEventListenerRegistry.notifyCacheAdded(cache.getName());
        }
    }

    /**
     * Checks whether a cache of type ehcache exists.
     * <p/>
     *
     * @param cacheName the cache name to check for
     * @return true if it exists
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public boolean cacheExists(String cacheName) throws IllegalStateException {
        checkStatus();
        return (ehcaches.get(cacheName) != null);
    }

    /**
     * Removes all caches using {@link #removeCache} for each cache.
     */
    public void removalAll() {
        String[] cacheNames = getCacheNames();
        for (String cacheName : cacheNames) {
            removeCache(cacheName);
        }
    }

    /**
     * Remove a cache from the CacheManager. The cache is disposed of.
     *
     * @param cacheName the cache name
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void removeCache(String cacheName) throws IllegalStateException {
        checkStatus();

        //NPE guard
        if (cacheName == null || cacheName.length() == 0) {
            return;
        }
        Ehcache cache = (Ehcache) ehcaches.remove(cacheName);
        if (cache != null && cache.getStatus().equals(Status.STATUS_ALIVE)) {
            cache.dispose();
            cacheManagerEventListenerRegistry.notifyCacheRemoved(cache.getName());
        }
        caches.remove(cacheName);
    }

    /**
     * Shuts down the CacheManager.
     * <p/>
     * If the shutdown occurs on the singleton, then the singleton is removed, so that if a singleton access method
     * is called, a new singleton will be created.
     * <p/>
     * By default there is no shutdown hook (ehcache-1.3-beta2 and higher).
     * <p/>
     * Set the system property net.sf.ehcache.enableShutdownHook=true to turn it on.
     */
    public void shutdown() {
        synchronized (CacheManager.class) {
            if (status.equals(Status.STATUS_SHUTDOWN)) {

                    LOG.debug("CacheManager already shutdown");
                return;
            }
            for (CacheManagerPeerProvider cacheManagerPeerProvider : cacheManagerPeerProviders.values()) {
                if (cacheManagerPeerProvider != null) {
                    cacheManagerPeerProvider.dispose();
                }
            }

            // cancel the cacheManager timer and all tasks
            if (cacheManagerTimer != null) {
                cacheManagerTimer.cancel();
                cacheManagerTimer.purge();
            }

            cacheManagerEventListenerRegistry.dispose();

            synchronized (CacheManager.class) {
                ALL_CACHE_MANAGERS.remove(this);

                Collection cacheSet = ehcaches.values();
                for (Iterator iterator = cacheSet.iterator(); iterator.hasNext();) {
                    Ehcache cache = (Ehcache) iterator.next();
                    if (cache != null) {
                        cache.dispose();
                    }
                }
                defaultCache.dispose();
                status = Status.STATUS_SHUTDOWN;

                //only delete singleton if the singleton is shutting down.
                if (this == singleton) {
                    singleton = null;
                }
                removeShutdownHook();
            }
        }
    }

    /**
     * Returns a list of the current cache names.
     *
     * @return an array of {@link String}s
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public String[] getCacheNames() throws IllegalStateException {
        checkStatus();
        String[] list = new String[ehcaches.size()];
        return (String[]) ehcaches.keySet().toArray(list);
    }


    /**
     * Checks the state of the CacheManager for legal operation
     */
    protected void checkStatus() {
        if (!(status.equals(Status.STATUS_ALIVE))) {
            if (status.equals(Status.STATUS_UNINITIALISED)) {
                throw new IllegalStateException("The CacheManager has not yet been initialised. It cannot be used yet.");
            } else if (status.equals(Status.STATUS_SHUTDOWN)) {
                throw new IllegalStateException("The CacheManager has been shut down. It can no longer be used.");
            }
        }
    }

    /**
     * Gets the status attribute of the Ehcache
     *
     * @return The status value from the Status enum class
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Clears  the contents of all caches in the CacheManager, but without
     * removing any caches.
     * <p/>
     * This method is not synchronized. It only guarantees to clear those elements in a cache
     * at the time that the {@link Ehcache#removeAll()} mehod  on each cache is called.
     */
    public void clearAll() throws CacheException {
        String[] cacheNames = getCacheNames();

            LOG.debug("Clearing all caches");
        for (String cacheName : cacheNames) {
            Ehcache cache = getEhcache(cacheName);
            cache.removeAll();
        }
    }


    /**
     * Gets the <code>CacheManagerPeerProvider</code>, matching the given scheme
     * For distributed caches, the peer provider finds other cache managers and their caches in the same cluster
     *
     * @param scheme the replication scheme to use. Schemes shipped with ehcache are RMI, JGROUPS, JMS
     * @return the provider, or null if one does not exist
     */
    public CacheManagerPeerProvider getCacheManagerPeerProvider(String scheme) {
        return cacheManagerPeerProviders.get(scheme);
    }

    /**
     * When CacheManage is configured as part of a cluster, a CacheManagerPeerListener will
     * be registered in it. Use this to access the individual cache listeners
     *
     * @param scheme the replication scheme to use. Schemes shipped with ehcache are RMI, JGROUPS, JMS
     * @return the listener, or null if one does not exist
     */
    public CacheManagerPeerListener getCachePeerListener(String scheme) {
        return cacheManagerPeerListeners.get(scheme);
    }

    /**
     * Returns the composite listener. A notification sent to this listener will notify all registered
     * listeners.
     *
     * @return null if none
     * @see "getCacheManagerEventListenerRegistry"
     */
    public CacheManagerEventListener getCacheManagerEventListener() {
        return cacheManagerEventListenerRegistry;
    }

    /**
     * Same as getCacheManagerEventListenerRegistry().registerListener(cacheManagerEventListener);
     * Left for backward compatiblity
     *
     * @param cacheManagerEventListener the listener to set.
     * @see "getCacheManagerEventListenerRegistry"
     */
    public void setCacheManagerEventListener(CacheManagerEventListener cacheManagerEventListener) {
        getCacheManagerEventListenerRegistry().registerListener(cacheManagerEventListener);
    }

    /**
     * Gets the CacheManagerEventListenerRegistry. Add and remove listeners here.
     */
    public CacheManagerEventListenerRegistry getCacheManagerEventListenerRegistry() {
        return cacheManagerEventListenerRegistry;
    }


    /**
     * Replaces in the map of Caches managed by this CacheManager an Ehcache with a decorated version of the same
     * Ehcache. CacheManager can operate fully with a decorated Ehcache.
     * <p/>
     * Ehcache Decorators can be used to obtain different behaviour from an Ehcache in a very flexible way. Some examples in
     * ehcache are:
     * <ol>
     * <li>{@link net.sf.ehcache.constructs.blocking.BlockingCache} - A cache that blocks other threads from getting a null element until the first thread
     * has placed a value in it.
     * <li>{@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache} - A BlockingCache that has the additional
     * property of knowing how to load its own entries.
     * </ol>
     * Many other kinds are possible.
     * <p/>
     * It is generally required that a decorated cache, once constructed, is made available to other execution threads.
     * The simplest way of doing this is to substitute the original cache for the decorated one here.
     * <p/>
     * Note that any overwritten Ehcache methods will take on new behaviours without casting. Casting is only required
     * for new methods that the decorator introduces.
     * For more information see the well known Gang of Four Decorator pattern.
     *
     * @param ehcache
     * @param decoratedCache An implementation of Ehcache that wraps the original cache.
     * @throws CacheException if the two caches do not equal each other.
     */
    public void replaceCacheWithDecoratedCache(Ehcache ehcache, Ehcache decoratedCache) throws CacheException {
        if (!ehcache.equals(decoratedCache)) {
            throw new CacheException("Cannot replace " + decoratedCache.getName()
                    + " It does not equal the incumbent cache.");
        } else {
            String cacheName = ehcache.getName();
            ehcaches.remove(cacheName);
            caches.remove(cacheName);
            ehcaches.put(decoratedCache.getName(), decoratedCache);
            if (decoratedCache instanceof Cache) {
                caches.put(decoratedCache.getName(), decoratedCache);
            }
        }

    }


    /**
     * Gets the name of the CacheManager. This is useful for distinguishing multiple CacheManagers
     *
     * @return the name, or the output of toString() if it is not set.
     * @see #toString() which uses either the name or Object.toString()
     */
    public String getName() {
        if (name != null) {
            return name;
        } else {
            return super.toString();
        }
    }
    
    /**
     * Indicate whether the CacheManager is named or not.
     * @return True if named
     */
    public boolean isNamed() {
        return name != null;
    }

    /**
     * Sets the name of the CacheManager. This is useful for distinguishing multiple CacheManagers
     * in a monitoring situation.
     *
     * @param name a name with characters legal in a JMX ObjectName
     */
    public void setName(String name) {
        this.name = name;
        try {
            mbeanRegistrationProvider.reinitialize();
        } catch (MBeanRegistrationProviderException e) {
            throw new CacheException(
                "Problem in reinitializing MBeanRegistrationProvider - "
                        + mbeanRegistrationProvider.getClass().getName(), e);
        }
    }

    /**
     * @return either the name of this CacheManager, or if unset, Object.toString()
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the disk store path. This may be null if no caches need a DiskStore and none was configured.
     * The path cannot be changed after creation of the CacheManager. All caches take the disk store path
     * from this value.
     *
     * @return the disk store path.
     */
    public String getDiskStorePath() {
        return diskStorePath;
    }
    
    /**
     * Returns a {@link FailSafeTimer} associated with this {@link CacheManager}
     * 
     * @return The {@link FailSafeTimer} associated with this cache manager
     * @since 1.7
     */
    public FailSafeTimer getTimer() {
        return cacheManagerTimer;
    }
}

