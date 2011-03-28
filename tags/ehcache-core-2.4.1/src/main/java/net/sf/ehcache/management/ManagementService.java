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

package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.hibernate.management.impl.EhcacheHibernateMbeanNames;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ehcache CacheManagers and Caches have lifecycles. Often normal use of a CacheManager
 * will be to shut it down and create a new one from within a running JVM. For example,
 * in Java EE environments, applications are often undeployed and then redeployed. A
 * servlet listener available in the web module, <code>net.sf.ehcache.constructs.web.ShutdownListener</code>}
 * enables this to be detected and the CacheManager shutdown.
 * <p/>
 * When a CacheManager is shut down we need to ensure there is no memory, resource or
 * thread leakage. An MBeanServer, particularly a platform MBeanServer, can be expected
 * to exist for the lifespan of the JVM. Accordingly, we need to deregister them when
 * needed without creating a leakage.
 * <p/>
 * The second purpose of this class (and this package) is to keep management concerns away
 * from the core ehcache packages. That way, JMX is not a required dependency, but rather
 * an optional one.
 * <p/>
 * This class is constructable as of 1.5 to support injection via IoC containers.
 *
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public class ManagementService implements CacheManagerEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ManagementService.class.getName());

    private MBeanServer mBeanServer;
    private net.sf.ehcache.CacheManager backingCacheManager;
    private boolean registerCacheManager;
    private boolean registerCaches;
    private boolean registerCacheConfigurations;
    private boolean registerCacheStatistics;
    private boolean registerCacheStores;
    private Status status;




    /**
     * A constructor for a management service for a range of possible MBeans.
     * <p/>
     * The {@link #init()} method needs to be called after construction which causes
     * the selected monitoring options to be be registered
     * with the provided MBeanServer for caches in the given CacheManager.
     * <p/>
     * While registering the CacheManager enables traversal to all of the other
     * items,
     * this requires programmatic traversal. The other options allow entry points closer
     * to an item of interest and are more accessible from JMX management tools like JConsole.
     * Moreover CacheManager and Cache are not serializable, so remote monitoring is not possible
     * for CacheManager or Cache, while CacheStatistics and CacheConfiguration are. Finally
     * CacheManager and Cache enable management operations to be performed.
     * <p/>
     * Once monitoring is enabled caches will automatically added and removed from the MBeanServer
     * as they are added and disposed of from the CacheManager. When the CacheManager itself
     * shutsdown all registered MBeans will be unregistered.
     *
     * @param cacheManager                the CacheManager to listen to
     * @param mBeanServer                 the MBeanServer to register MBeans to
     * @param registerCacheManager        Whether to register the CacheManager MBean
     * @param registerCaches              Whether to register the Cache MBeans
     * @param registerCacheConfigurations Whether to register the CacheConfiguration MBeans
     * @param registerCacheStatistics     Whether to register the CacheStatistics MBeans
     * @throws net.sf.ehcache.CacheException if something goes wrong with init()
     * @since 2.2
     */
    public ManagementService(net.sf.ehcache.CacheManager cacheManager,
                             MBeanServer mBeanServer,
                             boolean registerCacheManager,
                             boolean registerCaches,
                             boolean registerCacheConfigurations,
                             boolean registerCacheStatistics,
                             boolean registerCacheStores) throws CacheException {

        status = Status.STATUS_UNINITIALISED;
        backingCacheManager = cacheManager;
        this.mBeanServer = mBeanServer;
        this.registerCacheManager = registerCacheManager;
        this.registerCaches = registerCaches;
        this.registerCacheConfigurations = registerCacheConfigurations;
        this.registerCacheStatistics = registerCacheStatistics;
        this.registerCacheStores = registerCacheStores;
    }

    /**
         * A constructor for a management service for a range of possible MBeans.
         * <p/>
         * The {@link #init()} method needs to be called after construction which causes
         * the selected monitoring options to be be registered
         * with the provided MBeanServer for caches in the given CacheManager.
         * <p/>
         * While registering the CacheManager enables traversal to all of the other
         * items,
         * this requires programmatic traversal. The other options allow entry points closer
         * to an item of interest and are more accessible from JMX management tools like JConsole.
         * Moreover CacheManager and Cache are not serializable, so remote monitoring is not possible
         * for CacheManager or Cache, while CacheStatistics and CacheConfiguration are. Finally
         * CacheManager and Cache enable management operations to be performed.
         * <p/>
         * Once monitoring is enabled caches will automatically added and removed from the MBeanServer
         * as they are added and disposed of from the CacheManager. When the CacheManager itself
         * shutsdown all registered MBeans will be unregistered.
         *
         * @param cacheManager                the CacheManager to listen to
         * @param mBeanServer                 the MBeanServer to register MBeans to
         * @param registerCacheManager        Whether to register the CacheManager MBean
         * @param registerCaches              Whether to register the Cache MBeans
         * @param registerCacheConfigurations Whether to register the CacheConfiguration MBeans
         * @param registerCacheStatistics     Whether to register the CacheStatistics MBeans
         * @throws net.sf.ehcache.CacheException if something goes wrong with init()
         */
        public ManagementService(net.sf.ehcache.CacheManager cacheManager,
                                 MBeanServer mBeanServer,
                                 boolean registerCacheManager,
                                 boolean registerCaches,
                                 boolean registerCacheConfigurations,
                                 boolean registerCacheStatistics) throws CacheException {

            this(cacheManager, mBeanServer, registerCacheManager, registerCaches,
                    registerCacheConfigurations, registerCacheStatistics, false);
        }


    /**
     * A convenience static method which creates a ManagementService and initialises it with the
     * supplied parameters.
     *
     * @param cacheManager                the CacheManager to listen to
     * @param mBeanServer                 the MBeanServer to register MBeans to
     * @param registerCacheManager        Whether to register the CacheManager MBean
     * @param registerCaches              Whether to register the Cache MBeans
     * @param registerCacheConfigurations Whether to register the CacheConfiguration MBeans
     * @param registerCacheStatistics     Whether to register the CacheStatistics MBeans
     * @see ManagementService#ManagementService(net.sf.ehcache.CacheManager, javax.management.MBeanServer, boolean, boolean, boolean, boolean, boolean)
     * @since 2.2
     */
    public static void registerMBeans(
            net.sf.ehcache.CacheManager cacheManager,
            MBeanServer mBeanServer,
            boolean registerCacheManager,
            boolean registerCaches,
            boolean registerCacheConfigurations,
            boolean registerCacheStatistics,
            boolean registerCacheStores) throws CacheException {

        ManagementService registry = new ManagementService(cacheManager,
                mBeanServer,
                registerCacheManager,
                registerCaches,
                registerCacheConfigurations,
                registerCacheStatistics,
                registerCacheStores);

        registry.init();
    }

    /**
     * A convenience static method which creates a ManagementService and initialises it with the
     * supplied parameters.
     * <p/>
     * This one is provided for backward compatibility
     *
     * @param cacheManager                the CacheManager to listen to
     * @param mBeanServer                 the MBeanServer to register MBeans to
     * @param registerCacheManager        Whether to register the CacheManager MBean
     * @param registerCaches              Whether to register the Cache MBeans
     * @param registerCacheConfigurations Whether to register the CacheConfiguration MBeans
     * @param registerCacheStatistics     Whether to register the CacheStatistics MBeans
     * @see ManagementService#ManagementService(net.sf.ehcache.CacheManager, javax.management.MBeanServer, boolean, boolean, boolean, boolean)
     */
    public static void registerMBeans(
            net.sf.ehcache.CacheManager cacheManager,
            MBeanServer mBeanServer,
            boolean registerCacheManager,
            boolean registerCaches,
            boolean registerCacheConfigurations,
            boolean registerCacheStatistics) throws CacheException {

        registerMBeans(cacheManager, mBeanServer, registerCacheManager, registerCaches, registerCacheConfigurations,
                registerCacheStatistics, false);
    }


    /**
     * Call to register the mbeans in the mbean server and start the event listeners and do any other required initialisation.
     * Once intialised, it registers itself as a CacheManageEvenListener with the backing CacheManager, so
     * that it can participate in lifecycle and other events.
     *
     * @throws net.sf.ehcache.CacheException - all exceptions are wrapped in CacheException
     */
    public void init() throws CacheException {
        CacheManager cacheManager = new CacheManager(backingCacheManager);
        try {
            registerCacheManager(cacheManager);

            registerPeerProviders();

            List caches = cacheManager.getCaches();
            for (int i = 0; i < caches.size(); i++) {
                Cache cache = (Cache) caches.get(i);
                registerCachesIfRequired(cache);
                registerCacheStatisticsIfRequired(cache);
                registerCacheConfigurationIfRequired(cache);
                registerCacheStoreIfRequired(cache);
            }
        } catch (Exception e) {
            throw new CacheException(e);
        }
        status = Status.STATUS_ALIVE;
        backingCacheManager.getCacheManagerEventListenerRegistry().registerListener(this);
    }


    private void registerPeerProviders() {
        final Map<String, CacheManagerPeerProvider> cacheManagerPeerProviders = this.backingCacheManager.getCacheManagerPeerProviders();
        for (final CacheManagerPeerProvider cacheManagerPeerProvider : cacheManagerPeerProviders.values()) {
            if (cacheManagerPeerProvider instanceof ManagedCacheManagerPeerProvider) {
                ((ManagedCacheManagerPeerProvider) cacheManagerPeerProvider).register(this.mBeanServer);
            }
        }
    }

    private void registerCacheManager(CacheManager cacheManager) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCacheManager) {
            mBeanServer.registerMBean(cacheManager, cacheManager.getObjectName());
        }
    }

    private void registerCacheConfigurationIfRequired(Cache cache) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCacheConfigurations) {
            CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
            mBeanServer.registerMBean(cacheConfiguration, cacheConfiguration.getObjectName());
        }
    }

    private void registerCacheStatisticsIfRequired(Cache cache) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCacheStatistics) {
            CacheStatistics cacheStatistics = cache.getStatistics();
            mBeanServer.registerMBean(cacheStatistics, cacheStatistics.getObjectName());
        }
    }

    private void registerCachesIfRequired(Cache cache) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCaches) {
            mBeanServer.registerMBean(cache, cache.getObjectName());
        }
    }

    private void registerCacheStoreIfRequired(Cache cache) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCacheStores) {
            Store cacheStore = cache.getStore();
            if (cacheStore != null) {
                mBeanServer.registerMBean(cacheStore, cacheStore.getObjectName());
            }
        }
    }

    /**
     * Returns the listener status.
     *
     * @return the status at the point in time the method is called
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Stop the listener and free any resources.
     * Removes registered ObjectNames
     *
     * @throws net.sf.ehcache.CacheException - all exceptions are wrapped in CacheException
     */
    public void dispose() throws CacheException {
        Set registeredObjectNames = null;

        try {
            // CacheManager MBean
            registeredObjectNames = mBeanServer.queryNames(CacheManager.createObjectName(backingCacheManager), null);
            // Other MBeans for this CacheManager
            registeredObjectNames.addAll(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*,CacheManager="
                    + EhcacheHibernateMbeanNames.mbeanSafe(backingCacheManager.toString())), null));
        } catch (MalformedObjectNameException e) {
            // this should not happen
            LOG.error("Error querying MBeanServer. Error was " + e.getMessage(), e);
        }
        for (Iterator iterator = registeredObjectNames.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            try {
                mBeanServer.unregisterMBean(objectName);
            } catch (Exception e) {
                LOG.error("Error unregistering object instance " + objectName + " . Error was " + e.getMessage(), e);
            }
        }
        status = Status.STATUS_SHUTDOWN;
    }

    /**
     * Called immediately after a cache has been added and activated.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to
     * call a synchronized method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that activation will also cause a CacheEventListener status change notification
     * from {@link net.sf.ehcache.Status#STATUS_UNINITIALISED} to
     * {@link net.sf.ehcache.Status#STATUS_ALIVE}. Care should be taken on processing that
     * notification because:
     * <ul>
     * <li>the cache will not yet be accessible from the CacheManager.
     * <li>the addCaches methods which cause this notification are synchronized on the
     * CacheManager. An attempt to call {@link net.sf.ehcache.CacheManager#getEhcache(String)}
     * will cause a deadlock.
     * </ul>
     * The calling method will block until this method returns.
     * <p/>
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     * @see net.sf.ehcache.event.CacheEventListener
     */
    public void notifyCacheAdded(String cacheName) {
        if (registerCaches || registerCacheStatistics || registerCacheConfigurations) {
            Cache cache = new Cache(backingCacheManager.getCache(cacheName));
            try {
                registerCachesIfRequired(cache);
                registerCacheStatisticsIfRequired(cache);
                registerCacheConfigurationIfRequired(cache);
                registerCacheStoreIfRequired(cache);
            } catch (Exception e) {
                LOG.error("Error registering cache for management for " + cache.getObjectName()
                        + " . Error was " + e.getMessage(), e);
            }
        }
    }

    /**
     * Called immediately after a cache has been disposed and removed. The calling method will
     * block until this method returns.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to
     * call a synchronized method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that a {@link net.sf.ehcache.event.CacheEventListener} status changed will also be triggered. Any
     * attempt from that notification to access CacheManager will also result in a deadlock.
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     */
    public void notifyCacheRemoved(String cacheName) {

        ObjectName objectName = null;
        try {
            if (registerCaches) {
                objectName = Cache.createObjectName(backingCacheManager.toString(), cacheName);
                mBeanServer.unregisterMBean(objectName);
            }
            if (registerCacheConfigurations) {
                objectName = CacheConfiguration.createObjectName(backingCacheManager.toString(), cacheName);
                mBeanServer.unregisterMBean(objectName);
            }
            if (registerCacheStatistics) {
                objectName = CacheStatistics.createObjectName(backingCacheManager.toString(), cacheName);
                mBeanServer.unregisterMBean(objectName);
            }
            if (registerCacheStores) {
                objectName = Store.createObjectName(backingCacheManager.toString(), cacheName);
                if (mBeanServer.isRegistered(objectName)) {
                    mBeanServer.unregisterMBean(objectName);
                }
            }
        } catch (Exception e) {
            LOG.error("Error unregistering cache for management for " + objectName
                    + " . Error was " + e.getMessage(), e);
        }

    }

}
