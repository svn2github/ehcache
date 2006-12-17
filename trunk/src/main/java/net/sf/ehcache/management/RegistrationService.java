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

package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ehcache CacheManagers and Caches have lifecycles. Often normal use of a CacheManager will be
 * to shut it down and create a new one from within a running JVM. For example, in JEE environments,
 * applications are often undeployed and then redeployed. A servlet listener, {@link net.sf.ehcache.constructs.web.ShutdownListener}
 * enables this to be detected and the CacheManager shutdown.
 * <p/>
 * When a CacheManager is shut down we need to ensure there is no memory, resource or thread leakage.
 * An MBeanServer, particularly a platform MBeanServer, can be expected to exist for the lifespan of the JVM.
 * Accordingly, we need a mechansim for keeping track of what MBeans we have registered so that we can
 * deregister them when needed, without creating a leakage.
 * <p/>
 * The second purpose of this class (and this package) is to keep management concerns away from the core ehcache packages.
 * That way, JMX is not a required dependency, but rather an optional one.
 *
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public final class RegistrationService implements CacheManagerEventListener {

    private static final Log LOG = LogFactory.getLog(RegistrationService.class.getName());

    private List registeredObjectNames = new ArrayList();
    private MBeanServer mBeanServer;
    private net.sf.ehcache.CacheManager backingCacheManager;
    private boolean registerCacheManager;
    private boolean registerCaches;
    private boolean registerCacheConfigurations;
    private boolean registerCacheStatistics;
    private Status status;

    /**
     * Require use of the factory method
     */
    private RegistrationService() {

    }

    /**
     * This method causes the selected monitoring options to be be registered
     * with the provided MBeanServer for caches in the given CacheManager.
     * <p/>
     * While registering the CacheManager enables traversal to all of the other items,
     * this requires programmatic traversal. The other options allow entry points closer
     * to an item of interest and are more accessible from JMX management tools like JConsole.
     * Moreover CacheManager and Cache are not serializable, so remote monitoring is not possible for CacheManager
     * or Cache, while CacheStatistics and CacheConfiguration are. Finally CacheManager and Cache enable
     * management operations to be performed.
     * <p/>
     * Once monitoring is enabled caches will automatically added and removed from the MBeanServer
     * as they are added and disposed of from the CacheManager. When the CacheManager itself shutsdown
     * all MBeans registered will be unregistered.
     *
     * @param cacheManager
     * @param mBeanServer
     * @param registerCacheManager
     * @param registerCaches
     * @param registerCacheConfigurations
     * @param registerCacheStatistics
     */
    public static void registerMBeans(
            net.sf.ehcache.CacheManager cacheManager,
            MBeanServer mBeanServer,
            boolean registerCacheManager,
            boolean registerCaches,
            boolean registerCacheConfigurations,
            boolean registerCacheStatistics) throws CacheException {

        RegistrationService registry = new RegistrationService();
        registry.status = Status.STATUS_UNINITIALISED;
        registry.backingCacheManager = cacheManager;
        registry.mBeanServer = mBeanServer;
        registry.registerCacheManager = registerCacheManager;
        registry.registerCaches = registerCaches;
        registry.registerCacheConfigurations = registerCacheConfigurations;
        registry.registerCacheStatistics = registerCacheStatistics;

        registry.init();
    }


    /**
     * Call to start the listeners and do any other required initialisation.
     * Once intialised, it registers itself as a CacheManageEvenListener with the backing CacheManager, so
     * that it can participate in lifecycle and other events.
     *
     * @throws net.sf.ehcache.CacheException - all exceptions are wrapped in CacheException
     */
    public void init() throws CacheException {
        CacheManager cacheManager = new CacheManager(backingCacheManager);
        try {
            registerCacheManager(cacheManager);

            List caches = cacheManager.getCaches();
            for (int i = 0; i < caches.size(); i++) {
                Cache cache = (Cache) caches.get(i);
                registerCachesIfRequired(cache);
                registerCacheStatisticsIfRequired(cache);
                registerCacheConfigurationIfRequired(cache);
            }
        } catch (Exception e) {
            throw new CacheException(e);
        }
        status = Status.STATUS_ALIVE;
        backingCacheManager.getCacheManagerEventListenerRegistry().registerListener(this);
    }

    private void registerCacheManager(CacheManager cacheManager) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCacheManager) {
            mBeanServer.registerMBean(cacheManager, cacheManager.getObjectName());
            registeredObjectNames.add(cacheManager.getObjectName());
        }
    }

    private void registerCacheConfigurationIfRequired(Cache cache) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCacheConfigurations) {
            CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
            mBeanServer.registerMBean(cacheConfiguration, cacheConfiguration.getObjectName());
            registeredObjectNames.add(cacheConfiguration.getObjectName());
        }
    }

    private void registerCacheStatisticsIfRequired(Cache cache) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCacheStatistics) {
            CacheStatistics cacheStatistics = cache.getStatistics();
            mBeanServer.registerMBean(cacheStatistics, cacheStatistics.getObjectName());
            registeredObjectNames.add(cacheStatistics.getObjectName());
        }
    }

    private void registerCachesIfRequired(Cache cache) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        if (registerCaches) {
            mBeanServer.registerMBean(cache, cache.getObjectName());
            registeredObjectNames.add(cache.getObjectName());
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
     *
     * @throws net.sf.ehcache.CacheException - all exceptions are wrapped in CacheException
     */
    public void dispose() throws CacheException {
        for (int i = 0; i < registeredObjectNames.size(); i++) {
            ObjectName objectName = (ObjectName) registeredObjectNames.get(i);
            try {
                mBeanServer.unregisterMBean(objectName);
            } catch (Exception e) {
                LOG.error("Error unregistering object instance " + objectName
                        + " . Error was " + e.getMessage(), e);
            }
        }
        registeredObjectNames.clear();
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
                registeredObjectNames.remove(objectName);
            }
            if (registerCacheConfigurations) {
                objectName = CacheConfiguration.createObjectName(backingCacheManager.toString(), cacheName);
                mBeanServer.unregisterMBean(objectName);
                registeredObjectNames.remove(objectName);
            }
            if (registerCacheStatistics) {
                objectName = CacheStatistics.createObjectName(backingCacheManager.toString(), cacheName);
                mBeanServer.unregisterMBean(objectName);
                registeredObjectNames.remove(objectName);
            }
        } catch (Exception e) {
            LOG.error("Error unregistering cache for management for " + objectName
                    + " . Error was " + e.getMessage(), e);
        }

    }

    /**
     * Gets a list of the object names that this service knows about. These will be removed
     * on CacheManager shutdown and as caches are added and removed.
     * @return the live list
     */
    public List getRegisteredObjectNames() {
        return registeredObjectNames;
    }
}
