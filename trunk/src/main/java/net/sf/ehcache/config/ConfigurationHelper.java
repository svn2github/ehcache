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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerListenerFactory;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.CacheManagerEventListenerFactory;
import net.sf.ehcache.util.ClassLoaderUtil;
import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The configuration for ehcache.
 * <p/>
 * This class can be populated through:
 * <ul>
 * <li>introspection by {@link ConfigurationFactory} or
 * <li>programmatically
 * </ul>
 *
 * @author Greg Luck
 * @version $Id: ConfigurationHelper.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class ConfigurationHelper {

    private static final Log LOG = LogFactory.getLog(ConfigurationHelper.class.getName());

    private Configuration configuration;
    private CacheManager cacheManager;

    /**
     * Only Constructor
     *
     * @param cacheManager
     * @param configuration
     */
    public ConfigurationHelper(CacheManager cacheManager, Configuration configuration) {
        if (cacheManager == null || configuration == null) {
            throw new IllegalArgumentException("Cannot have null parameters");
        }
        this.cacheManager = cacheManager;
        this.configuration = configuration;
    }


    /**
     * A factory method to create a RegisteredEventListeners
     */
    protected void registerCacheListeners(Cache cache, CacheConfiguration cacheConfiguration,
                                          RegisteredEventListeners registeredEventListeners) {
        List cacheEventListenerConfigurations = cacheConfiguration.cacheEventListenerConfigurations;
        for (int i = 0; i < cacheEventListenerConfigurations.size(); i++) {
            CacheConfiguration.CacheEventListenerFactoryConfiguration factoryConfiguration =
                    (CacheConfiguration.CacheEventListenerFactoryConfiguration) cacheEventListenerConfigurations.get(i);
            CacheEventListener cacheEventListener = createCacheEventListener(factoryConfiguration);
            registeredEventListeners.registerListener(cacheEventListener);
        }
    }


    /**
     * Tries to load the class specified otherwise defaults to null
     *
     * @param factoryConfiguration
     */
    private CacheEventListener createCacheEventListener(
            CacheConfiguration.CacheEventListenerFactoryConfiguration factoryConfiguration) {
        String className = null;
        CacheEventListener cacheEventListener = null;
        try {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        } catch (Throwable t) {
            //
        }
        if (className == null) {
            LOG.error("CacheEventListener factory not configured. Skipping listener configuration");
        } else {
            CacheEventListenerFactory factory = (CacheEventListenerFactory)
                    ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties());
            cacheEventListener = factory.createCacheEventListener(properties);
        }
        return cacheEventListener;
    }

    /**
     * Tries to load the class specified otherwise defaults to null
     */
    public CacheManagerPeerProvider createCachePeerProvider() {
        String className = null;
        FactoryConfiguration cachePeerProviderFactoryConfiguration =
                configuration.getCacheManagerPeerProviderFactoryConfiguration();
        try {
            className = cachePeerProviderFactoryConfiguration.fullyQualifiedClassPath;
        } catch (Throwable t) {
            //
        }
        if (className == null) {
            LOG.debug("No CachePeerProviderFactoryConfiguration specified. Not configuring a CacheManagerPeerProvider.");
            return null;
        } else {
            CacheManagerPeerProviderFactory cacheManagerPeerProviderFactory =
                    (CacheManagerPeerProviderFactory)
                            ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(cachePeerProviderFactoryConfiguration.properties);
            return cacheManagerPeerProviderFactory.createCachePeerProvider(cacheManager, properties);
        }
    }

    /**
     * Tries to load the class specified otherwise defaults to null
     */
    public CacheManagerPeerListener createCachePeerListener() {
        String className = null;
        FactoryConfiguration cachePeerListenerFactoryConfiguration =
                configuration.getCacheManagerPeerListenerFactoryConfiguration();
        try {
            className = cachePeerListenerFactoryConfiguration.fullyQualifiedClassPath;
        } catch (Throwable t) {
            //
        }
        if (className == null) {
            LOG.debug("No CachePeerListenerFactoryConfiguration specified. Not configuring a CacheManagerPeerListener.");
            return null;
        } else {
            CacheManagerPeerListenerFactory cacheManagerPeerListenerFactory = (CacheManagerPeerListenerFactory)
                    ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(cachePeerListenerFactoryConfiguration.properties);
            return cacheManagerPeerListenerFactory.createCachePeerListener(cacheManager, properties);
        }
    }

    /**
     * Tries to load the class specified.
     *
     * @return If there is none returns null.
     */
    public CacheManagerEventListener createCacheManagerEventListener() throws CacheException {
        String className = null;
        FactoryConfiguration cacheManagerEventListenerFactoryConfiguration =
                configuration.getCacheManagerEventListenerFactoryConfiguration();
        try {
            className = cacheManagerEventListenerFactoryConfiguration.fullyQualifiedClassPath;
        } catch (Throwable t) {
            //No class created because the config was missing
        }
        if (className == null || className.length() == 0) {
            LOG.debug("No CacheManagerEventListenerFactory class specified. Skipping...");
            return null;
        } else {
            CacheManagerEventListenerFactory factory = (CacheManagerEventListenerFactory)
                    ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(cacheManagerEventListenerFactoryConfiguration.properties);
            return factory.createCacheManagerEventListener(properties);
        }
    }

    /**
     * @return the disk store path, or null if not set.
     */
    public String getDiskStorePath() {
        DiskStoreConfiguration diskStoreConfiguration = configuration.getDiskStoreConfiguration();
        if (diskStoreConfiguration == null) {
            return null;
        } else {
            return diskStoreConfiguration.getPath();
        }
    }

    /**
     * @return the Default Cache
     * @throws net.sf.ehcache.CacheException if there is no default cache
     */
    public Cache createDefaultCache() throws CacheException {
        CacheConfiguration cacheConfiguration = configuration.getDefaultCacheConfiguration();
        if (cacheConfiguration == null) {
            throw new CacheException("Illegal configuration. No default cache is configured.");
        } else {
            cacheConfiguration.name = Cache.DEFAULT_CACHE_NAME;
            return createCache(cacheConfiguration);
        }
    }

    /**
     * Creates unitialised caches for each cache configuration found
     *
     * @return an empty set if there are none,
     */
    public Set createCaches() {
        Set caches = new HashSet();
        Set cacheConfigurations = configuration.getCacheConfigurations().entrySet();
        for (Iterator iterator = cacheConfigurations.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            CacheConfiguration cacheConfiguration = (CacheConfiguration) entry.getValue();
            Cache cache = createCache(cacheConfiguration);
            caches.add(cache);
        }
        return caches;
    }

    /**
     * Creates a cache from configuration where the configuration cache name matches the given name
     *
     * @return the cache, or null if there is no match
     */
    Cache createCacheFromName(String name) {
        CacheConfiguration cacheConfiguration = null;
        Set cacheConfigurations = configuration.getCacheConfigurations().entrySet();
        for (Iterator iterator = cacheConfigurations.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            CacheConfiguration cacheConfigurationCandidate = (CacheConfiguration) entry.getValue();
            if (cacheConfigurationCandidate.name.equals(name)) {
                cacheConfiguration = cacheConfigurationCandidate;
                break;
            }
        }
        if (cacheConfiguration == null) {
            return null;
        } else {
            return createCache(cacheConfiguration);
        }
    }

    /**
     * Create a cache given a cache configuration
     *
     * @param cacheConfiguration
     */
    Cache createCache(CacheConfiguration cacheConfiguration) {
        Cache cache = new Cache(cacheConfiguration.name,
                cacheConfiguration.maxElementsInMemory,
                cacheConfiguration.memoryStoreEvictionPolicy,
                cacheConfiguration.overflowToDisk,
                getDiskStorePath(),
                cacheConfiguration.eternal,
                cacheConfiguration.timeToLiveSeconds,
                cacheConfiguration.timeToIdleSeconds,
                cacheConfiguration.diskPersistent,
                cacheConfiguration.diskExpiryThreadIntervalSeconds,
                null);
        RegisteredEventListeners listeners = cache.getCacheEventNotificationService();
        registerCacheListeners(cache, cacheConfiguration, listeners);
        return cache;
    }

    /**
     * @return the Configuration used
     */
    public Configuration getConfigurationBean() {
        return configuration;
    }
}
