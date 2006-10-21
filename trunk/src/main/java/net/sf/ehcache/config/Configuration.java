/**
 *  Copyright 2003-2006 Greg Luck
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A bean, used by BeanUtils, to set configuration from an XML configuration file.
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class Configuration {

    private DiskStoreConfiguration diskStoreConfiguration;
    private CacheConfiguration defaultCacheConfiguration;
    private FactoryConfiguration cacheManagerPeerProviderFactoryConfiguration;
    private FactoryConfiguration cacheManagerPeerListenerFactoryConfiguration;
    private FactoryConfiguration cacheManagerEventListenerFactoryConfiguration;
    private final Map poolConfigurations = new HashMap();
    private final Map cacheConfigurations = new HashMap();
    private String configurationSource;

    /**
     * Empty constructor, which is used by {@link ConfigurationFactory}, and can be also sued programmatically.
     * <p/>
     * If you are using it programmtically you need to call the relevant add and setter methods in this class to
     * populate everything.
     */
    public Configuration() { }


    /**
     * Allows {@link BeanHandler} to add disk store location to the configuration.
     */
    public final void addDiskStore(DiskStoreConfiguration diskStoreConfiguration) throws ObjectExistsException {
        if (this.diskStoreConfiguration != null) {
            throw new ObjectExistsException("The Disk Store has already been configured");
        }
        this.diskStoreConfiguration = diskStoreConfiguration;
    }

    

    /**
     * Allows {@link BeanHandler} to add the CacheManagerEventListener to the configuration.
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
        if (cacheManagerPeerProviderFactoryConfiguration == null) {
            cacheManagerPeerProviderFactoryConfiguration = factory;
        }
    }

    /**
     * Adds a CachePeerProviderFactoryConfiguration.
     * cachePeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory"
     * properties="hostName=localhost, port=5000"
     */
    public final void addCacheManagerPeerListenerFactory(FactoryConfiguration factory) {
        if (cacheManagerPeerListenerFactoryConfiguration == null) {
            cacheManagerPeerListenerFactoryConfiguration = factory;
        }
    }

    /**
     * Allows {@link BeanHandler} to add a default configuration to the configuration.
     */
    public final void addDefaultCache(CacheConfiguration defaultCacheConfiguration) throws ObjectExistsException {
        if (this.defaultCacheConfiguration != null) {
            throw new ObjectExistsException("The Default Cache has already been configured");
        }
        this.defaultCacheConfiguration = defaultCacheConfiguration;
    }

    /**
     * Allows {@link BeanHandler} to add Cache Configurations to the configuration.
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
    public final Set getCacheConfigurationsKeySet() {
        return cacheConfigurations.keySet();
    }

    /**
     * @return the configuration's default cache configuration
     */
    public final CacheConfiguration getDefaultCacheConfiguration() {
        return defaultCacheConfiguration;
    }

    /**
     *
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
     * Gets the CacheManagerPeerProvider factory configuration.
     */
    public final FactoryConfiguration getCacheManagerPeerProviderFactoryConfiguration() {
        return cacheManagerPeerProviderFactoryConfiguration;
    }

    /**
     * Gets the CacheManagerPeerListener factory configuration.
     */
    public final FactoryConfiguration getCacheManagerPeerListenerFactoryConfiguration() {
        return cacheManagerPeerListenerFactoryConfiguration;
    }

    /**
     * Gets the CacheManagerEventListener factory configuration.
     */
    public final FactoryConfiguration getCacheManagerEventListenerFactoryConfiguration() {
        return cacheManagerEventListenerFactoryConfiguration;
    }

    /**
     * Gets a Map of cache configurations, keyed by name.
     */
    public final Map getCacheConfigurations() {
        return cacheConfigurations;
    }

    /**
     * Sets the configuration source.
     * @param configurationSource  an informative description of the source, preferably
     * including the resource name and location.
     */
    public final void setSource(String configurationSource) {
        this.configurationSource = configurationSource;
    }

    /**
     * Gets a description of the source from which this configuration was created.
     */
    public final String getConfigurationSource() {
        return configurationSource;
    }
}
