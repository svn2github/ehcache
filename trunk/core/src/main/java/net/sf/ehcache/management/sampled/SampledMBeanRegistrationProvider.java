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

package net.sf.ehcache.management.sampled;

import java.lang.management.ManagementFactory;
import java.util.Set;

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
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SampledMBeanRegistrationProvider implements MBeanRegistrationProvider, CacheManagerEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(SampledMBeanRegistrationProvider.class.getName());

    private static final int MAX_MBEAN_REGISTRATION_RETRIES = 50;

    private Status status = Status.STATUS_UNINITIALISED;
    private CacheManager cacheManager;
    private String clientUUID;
    private final MBeanServer mBeanServer;

    // name of the cacheManager when the mbeans are registered.
    // On cacheManager.dispose(), need to remove
    // the mbean with the name used while registering the mbean.
    // Avoid leaking mbeans when user changes name of the cacheManager after
    // construction
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
    public synchronized void initialize(CacheManager cacheManagerParam, ClusteredInstanceFactory clusteredInstanceFactory) {
        if (isAlive()) {
            return;
        }
        status = Status.STATUS_ALIVE;
        this.cacheManager = cacheManagerParam;
        this.clientUUID = clusteredInstanceFactory == null ? "" : clusteredInstanceFactory.getUUID();
        try {
            registerCacheManagerMBean(new SampledCacheManager(cacheManager));
        } catch (Exception e) {
            status = Status.STATUS_UNINITIALISED;
            throw new CacheException(e);
        }

        // setup event listener so that addition of new caches registers
        // corresponding Cache MBeans
        cacheManager.getCacheManagerEventListenerRegistry().registerListener(this);
    }

    private void registerCacheManagerMBean(SampledCacheManager cacheManagerMBean) throws Exception {
        int tries = 0;
        boolean success = false;
        Exception exception = null;
        do {
            this.registeredCacheManagerName = cacheManager.getName();
            if (tries != 0) {
                registeredCacheManagerName += "_" + tries;
            }
            try {
                // register the CacheManager MBean
                mBeanServer.registerMBean(cacheManagerMBean,
                        SampledEhcacheMBeans.getCacheManagerObjectName(clientUUID, registeredCacheManagerName));
                success = true;
                cacheManagerMBean.setMBeanRegisteredName(registeredCacheManagerName);
                break;
            } catch (InstanceAlreadyExistsException e) {
                success = false;
                exception = e;
            }
            tries++;
        } while (tries < MAX_MBEAN_REGISTRATION_RETRIES);
        if (!success) {
            throw new Exception("Cannot register mbean for CacheManager with name" + cacheManager.getName() + " after "
                    + MAX_MBEAN_REGISTRATION_RETRIES + " retries. Last tried name=" + registeredCacheManagerName, exception);
        }

        // register Cache MBeans for the caches
        String[] caches = cacheManager.getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            registerCacheMBean(cache);
            registerStoreMBean(cache);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void reinitialize(ClusteredInstanceFactory clusteredInstanceFactory) throws MBeanRegistrationProviderException {
        dispose();
        initialize(this.cacheManager, clusteredInstanceFactory);
    }

    /**
     * CacheManagerEventListener.init() - no need to do anything here
     */
    public void init() throws CacheException {
        // no-op
    }

    // no need to worry about duplicate cache names
    // cache manager does not allow duplicate cache names
    // and as cache manager mbeans names are unique, the mbeans for the caches
    // will also be unique
    private void registerCacheMBean(Ehcache cache) throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {
        // enable sampled stats
        if (cache.isStatisticsEnabled()) {
          cache.setSampledStatisticsEnabled(true);
        }
        SampledCache terracottaCacheMBean = new SampledCache(cache);
        try {
            mBeanServer.registerMBean(terracottaCacheMBean,
                    SampledEhcacheMBeans.getCacheObjectName(clientUUID, registeredCacheManagerName,
                            terracottaCacheMBean.getImmutableCacheName()));
        } catch (MalformedObjectNameException e) {
            throw new MBeanRegistrationException(e);
        }
    }

    private void registerStoreMBean(Ehcache cache) throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {
        // enable sampled stats
        Object bean;
        if (cache instanceof net.sf.ehcache.Cache) {
          bean = ((net.sf.ehcache.Cache) cache).getStoreMBean();
          if (bean != null) {
             try {
                 mBeanServer.registerMBean(bean,
                         SampledEhcacheMBeans.getStoreObjectName(clientUUID, registeredCacheManagerName,
                                 cache.getName()));
             } catch (MalformedObjectNameException e) {
                 throw new MBeanRegistrationException(e);
             }
          }
       }
    }

    /**
     * Returns the listener status.
     *
     * @return the status at the point in time the method is called
     */
    public synchronized Status getStatus() {
        return status;
    }

    /**
     * Stop the listener and free any resources. Removes registered ObjectNames
     *
     * @throws net.sf.ehcache.CacheException
     *             - all exceptions are wrapped in CacheException
     */
    public synchronized void dispose() throws CacheException {
        if (!isAlive()) {
            return;
        }
        Set<ObjectName> registeredObjectNames = null;

        try {
            // CacheManager MBean
            registeredObjectNames = mBeanServer
                    .queryNames(SampledEhcacheMBeans.getCacheManagerObjectName(clientUUID, registeredCacheManagerName), null);
            // Other MBeans for this CacheManager
            registeredObjectNames.addAll(mBeanServer.queryNames(SampledEhcacheMBeans
                    .getQueryCacheManagerObjectName(clientUUID, registeredCacheManagerName), null));
        } catch (MalformedObjectNameException e) {
            LOG.warn("Error querying MBeanServer. Error was " + e.getMessage(), e);
        }
        for (ObjectName objectName : registeredObjectNames) {
            try {
                mBeanServer.unregisterMBean(objectName);
            } catch (Exception e) {
                LOG.warn("Error unregistering object instance " + objectName + " . Error was " + e.getMessage(), e);
            }
        }
        status = Status.STATUS_SHUTDOWN;
    }

    /**
     * Returns true if this {@link SampledMBeanRegistrationProvider} is alive
     *
     * @return true if alive otherwise false
     */
    public synchronized boolean isAlive() {
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
            registerStoreMBean(cache);
        } catch (Exception e) {
            LOG.warn("Error registering cache for management for " + cacheName + " . Error was " + e.getMessage(), e);
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
            objectName = SampledEhcacheMBeans.getCacheObjectName(clientUUID, registeredCacheManagerName, cacheName);
            if (mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            LOG.warn("Error unregistering cache for management for " + objectName + " . Error was " + e.getMessage(), e);
        }
    }
}
