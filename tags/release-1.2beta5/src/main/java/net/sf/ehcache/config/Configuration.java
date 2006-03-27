/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.config;

import net.sf.ehcache.ObjectExistsException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A bean, used by BeanUtils, to set configuration from an XML configuration file
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: Configuration.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class Configuration {

    private DiskStoreConfiguration diskStoreConfiguration;
    private CacheConfiguration defaultCacheConfiguration;
    private FactoryConfiguration cacheManagerPeerProviderFactoryConfiguration;
    private FactoryConfiguration cacheManagerPeerListenerFactoryConfiguration;
    private FactoryConfiguration cacheManagerEventListenerFactoryConfiguration;
    private Map cacheConfigurations = new HashMap();
    private String configurationSource;

    /**
     * Package protected empty constructor for use by {@link ConfigurationFactory}. This is not usable programmatically.
     */
    Configuration() { }


    /**
     * Allows {@link BeanHandler} to add disk store location to the configuration
     */
    public void addDiskStore(DiskStoreConfiguration diskStoreConfigurationParameter) throws ObjectExistsException {
        if (diskStoreConfiguration != null) {
            throw new ObjectExistsException("The Disk Store has already been configured");
        }
        diskStoreConfiguration = diskStoreConfigurationParameter;
    }

    /**
     * Allows {@link BeanHandler} to add the CacheManagerEventListener to the configuration
     */
    public void addCacheManagerEventListenerFactory(FactoryConfiguration
            cacheManagerEventListenerFactoryConfiguration) throws ObjectExistsException {
        if (this.cacheManagerEventListenerFactoryConfiguration == null) {
            this.cacheManagerEventListenerFactoryConfiguration = cacheManagerEventListenerFactoryConfiguration;
        }
    }

    /**
     * Adds a CachePeerProviderFactoryConfiguration
     */
    public void addCacheManagerPeerProviderFactory(FactoryConfiguration factory) {
        if (cacheManagerPeerProviderFactoryConfiguration == null) {
            cacheManagerPeerProviderFactoryConfiguration = factory;
        }
    }

    /**
     * Adds a CachePeerProviderFactoryConfiguration
     * cachePeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory"
     * properties="hostName=localhost, port=5000"
     */
    public void addCacheManagerPeerListenerFactory(FactoryConfiguration factory) {
        if (cacheManagerPeerListenerFactoryConfiguration == null) {
            cacheManagerPeerListenerFactoryConfiguration = factory;
        }
    }


    /**
     * Allows {@link BeanHandler} to add a default configuration to the configuration
     */
    public void addDefaultCache(CacheConfiguration defaultCacheConfiguration) throws ObjectExistsException {
        if (this.defaultCacheConfiguration != null) {
            throw new ObjectExistsException("The Default Cache has already been configured");
        }
        this.defaultCacheConfiguration = defaultCacheConfiguration;
    }

    /**
     * Allows {@link BeanHandler} to add Cache Configurations to the configuration
     */
    public void addCache(CacheConfiguration cacheConfiguration) throws ObjectExistsException {
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
     * Gets a Map of cacheConfigurations
     */
    public Set getCacheConfigurationsKeySet() {
        return cacheConfigurations.keySet();
    }

    /**
     * @return the configuration's default cache configuration
     */
    public CacheConfiguration getDefaultCacheConfiguration() {
        return defaultCacheConfiguration;
    }

    /**
     *
     * @param defaultCacheConfiguration
     */
    public void setDefaultCacheConfiguration(CacheConfiguration defaultCacheConfiguration) {
        this.defaultCacheConfiguration = defaultCacheConfiguration;
    }


    /**
     * Gets the disk store configuration
     */
    public DiskStoreConfiguration getDiskStoreConfiguration() {
        return diskStoreConfiguration;
    }

    /**
     * Gets the CacheManagerPeerProvider factory configuration
     */
    public FactoryConfiguration getCacheManagerPeerProviderFactoryConfiguration() {
        return cacheManagerPeerProviderFactoryConfiguration;
    }

    /**
     * Gets the CacheManagerPeerListener factory configuration
     */
    public FactoryConfiguration getCacheManagerPeerListenerFactoryConfiguration() {
        return cacheManagerPeerListenerFactoryConfiguration;
    }

    /**
     * Gets the CacheManagerEventListener factory configuration
     */
    public FactoryConfiguration getCacheManagerEventListenerFactoryConfiguration() {
        return cacheManagerEventListenerFactoryConfiguration;
    }

    /**
     * Gets a Map of cache configurations, keyed by name
     */
    public Map getCacheConfigurations() {
        return cacheConfigurations;
    }

    /**
     * Sets the configuration source
     * @param configurationSource  an informative description of the source, preferably
     * including the resource name and location.
     */
    public void setSource(String configurationSource) {
        this.configurationSource = configurationSource;
    }

    /**
     * Gets a description of the source from which this configuration was created.
     */
    public String getConfigurationSource() {
        return configurationSource;
    }
}
