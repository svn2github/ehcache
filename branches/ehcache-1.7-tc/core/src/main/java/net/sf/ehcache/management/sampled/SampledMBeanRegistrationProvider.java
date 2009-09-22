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

package net.sf.ehcache.management.sampled;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.management.provider.MBeanRegistrationProvider;
import net.sf.ehcache.management.provider.MBeanRegistrationProviderException;

/**
 * An implementation of {@link MBeanRegistrationProvider} which registers
 * sampled MBeans for the CacheManager and its Caches.
 * This also implements {@link CacheManagerEventListener} to add/remove/cleanup
 * MBeans for new caches added or removed
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledMBeanRegistrationProvider implements
        MBeanRegistrationProvider, CacheManagerEventListener {

    private static final Logger LOG = Logger
            .getLogger(SampledMBeanRegistrationProvider.class.getName());

    private volatile Status status = Status.STATUS_UNINITIALISED;
    private CacheManager cacheManager;
    private final MBeanServer mBeanServer;

    // name of the cacheManager when the mbeans are registered.
    // On cacheManager.dispose(), need to remove
    // the mbean with the name used while registering the mbean.
    // Avoid leaking mbeans when user changes name of the cacheManager after construction
    // by doing setName()
    private volatile String registeredCacheManagerName;

    /**
     * Default constructor
     */
    public SampledMBeanRegistrationProvider() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(CacheManager cacheManagerParam) {
        if (isAlive()) {
            return;
        }
        status = Status.STATUS_ALIVE;
        this.cacheManager = cacheManagerParam;
        SampledCacheManager cacheManagerMBean = new SampledCacheManager(
                cacheManager);
        try {

            this.registeredCacheManagerName = cacheManager.getName();
            // register the CacheManager MBean
            mBeanServer.registerMBean(cacheManagerMBean, SampledEhcacheMBeans
                    .getCacheManagerObjectName(registeredCacheManagerName));

            // register Cache MBeans for the caches
            String[] caches = cacheManager.getCacheNames();
            for (String cacheName : caches) {
                Ehcache cache = cacheManager.getEhcache(cacheName);
                registerCacheMBean(cache);
            }
        } catch (Exception e) {
            status = Status.STATUS_UNINITIALISED;
            throw new CacheException(e);
        }

        // setup event listener so that addition of new caches registers
        // corresponding Cache MBeans
        cacheManager.getCacheManagerEventListenerRegistry().registerListener(
                this);
    }
    
    /**
     * {@inheritDoc}
     */
    public void reinitialize()
            throws MBeanRegistrationProviderException {
        dispose();
        initialize(this.cacheManager);
    }

    /**
     * CacheManagerEventListener.init() - no need to do anything here
     */
    public void init() throws CacheException {
        // no-op
    }

    private void registerCacheMBean(Ehcache cache)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {
        // enable sampled stats
        cache.setSampledStatisticsEnabled(true);
        SampledCache terracottaCacheMBean = new SampledCache(cache);
        try {
            this.mBeanServer.registerMBean(terracottaCacheMBean,
                    SampledEhcacheMBeans.getCacheObjectName(
                            registeredCacheManagerName, terracottaCacheMBean
                                    .getImmutableCacheName()));
        } catch (MalformedObjectNameException e) {
            throw new MBeanRegistrationException(e);
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
     * Stop the listener and free any resources. Removes registered ObjectNames
     * 
     * @throws net.sf.ehcache.CacheException
     *             - all exceptions are wrapped in CacheException
     */
    public void dispose() throws CacheException {
        if (!isAlive()) {
            return;
        }
        Set<ObjectName> registeredObjectNames = null;

        try {
            // CacheManager MBean
            registeredObjectNames = mBeanServer
                    .queryNames(
                            SampledEhcacheMBeans
                                    .getCacheManagerObjectName(registeredCacheManagerName),
                            null);
            // Other MBeans for this CacheManager
            registeredObjectNames
                    .addAll(mBeanServer
                            .queryNames(
                                    SampledEhcacheMBeans
                                            .getQueryObjectNameForCacheManager(registeredCacheManagerName),
                                    null));
        } catch (MalformedObjectNameException e) {
            LOG.log(Level.WARNING, "Error querying MBeanServer. Error was "
                    + e.getMessage(), e);
        }
        for (ObjectName objectName : registeredObjectNames) {
            try {
                mBeanServer.unregisterMBean(objectName);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error unregistering object instance "
                        + objectName + " . Error was " + e.getMessage(), e);
            }
        }
        status = Status.STATUS_SHUTDOWN;
    }

    private boolean isAlive() {
        return status == Status.STATUS_ALIVE;
    }

    /**
     * Called immediately after a cache has been added and activated.
     */
    public void notifyCacheAdded(String cacheName) {
        if (!isAlive()) {
            return;
        }
        try {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            registerCacheMBean(cache);
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Error registering cache for management for " + cacheName
                            + " . Error was " + e.getMessage(), e);
        }
    }

    /**
     * Called immediately after a cache has been disposed and removed. The
     * calling method will block until this method
     * returns.
     * 
     * @param cacheName
     *            the name of the <code>Cache</code> the operation relates to
     */
    public void notifyCacheRemoved(String cacheName) {
        if (!isAlive()) {
            return;
        }
        ObjectName objectName = null;
        try {
            objectName = SampledEhcacheMBeans.getCacheObjectName(
                    registeredCacheManagerName, cacheName);
            mBeanServer.unregisterMBean(objectName);

        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Error unregistering cache for management for "
                            + objectName + " . Error was " + e.getMessage(), e);
        }

    }
}
