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

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.hibernate.management.impl.BaseEmitterBean;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;

/**
 * An implementation of {@link SampledCacheManagerMBean}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheManager extends BaseEmitterBean implements SampledCacheManagerMBean {
    private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

    private final CacheManager cacheManager;
    private String mbeanRegisteredName;
    private volatile boolean mbeanRegisteredNameSet;

    static {
        final String[] notifTypes = new String[] {CACHES_ENABLED, CACHES_CLEARED, STATISTICS_ENABLED, STATISTICS_RESET, };
        final String name = Notification.class.getName();
        final String description = "Ehcache SampledCacheManager Event";
        NOTIFICATION_INFO = new MBeanNotificationInfo[] {new MBeanNotificationInfo(notifTypes, name, description), };
    }
    
    /**
     * Constructor taking the backing {@link CacheManager}
     * 
     * @param cacheManager
     */
    public SampledCacheManager(CacheManager cacheManager) throws NotCompliantMBeanException {
        super(SampledCacheManagerMBean.class);
        this.cacheManager = cacheManager;
        cacheManager.setCacheManagerEventListener(new EventListener());   
    }

    /**
     * Listen for caches coming and going so that we can add a PropertyChangeListener.
     */
    private class EventListener implements CacheManagerEventListener {
        private Status status = Status.STATUS_UNINITIALISED;
        
        public void dispose() throws CacheException {
            status = Status.STATUS_SHUTDOWN;
        }

        public Status getStatus() {
            return status;
        }

        public void init() throws CacheException {
            status = Status.STATUS_ALIVE;
        }

        public void notifyCacheAdded(String cacheName) {
            /**/
        }

        public void notifyCacheRemoved(String cacheName) {
            /**/
        }
    }
    
    /**
     * Set the name used to register this mbean. Can be called only once.
     * Package protected method
     */
    void setMBeanRegisteredName(String name) {
        if (mbeanRegisteredNameSet) {
            throw new IllegalStateException("Name used for registering this mbean is already set");
        }
        mbeanRegisteredNameSet = true;
        mbeanRegisteredName = name;
    }

    /**
     * {@inheritDoc}
     */
    public void clearAll() {
        cacheManager.clearAll();
        sendNotification(CACHES_CLEARED);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getCacheNames() throws IllegalStateException {
        return cacheManager.getCacheNames();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cacheManager.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        // no-op
    }

    /**
     * @return map of cache metrics (hits, misses)
     */
    public Map<String, long[]> getCacheMetrics() {
        Map<String, long[]> result = new HashMap<String, long[]>();
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            SampledCacheStatistics stats = cache.getSampledCacheStatistics();
            result.put(cacheName, new long[] {stats.getCacheHitMostRecentSample(), stats.getCacheMissMostRecentSample(),
                    stats.getCacheElementPutMostRecentSample(), });
        }
        return result;
    }

    /**
     * @return aggregate hit rate
     */
    public long getCacheHitRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            SampledCacheStatistics stats = cache.getSampledCacheStatistics();
            result += stats.getCacheHitMostRecentSample();
        }
        return result;
    }

    /**
     * @return aggregate miss rate
     */
    public long getCacheMissRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            SampledCacheStatistics stats = cache.getSampledCacheStatistics();
            result += stats.getCacheMissMostRecentSample();
        }
        return result;
    }

    /**
     * @return aggregate put rate
     */
    public long getCachePutRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            SampledCacheStatistics stats = cache.getSampledCacheStatistics();
            result += stats.getCacheElementPutMostRecentSample();
        }
        return result;
    }

    /**
     * @return aggregate update rate
     */
    public long getCacheUpdateRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            SampledCacheStatistics stats = cache.getSampledCacheStatistics();
            result += stats.getCacheElementUpdatedMostRecentSample();
        }
        return result;
    }

    /**
     * @return aggregate eviction rate
     */
    public long getCacheEvictionRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            SampledCacheStatistics stats = cache.getSampledCacheStatistics();
            result += stats.getCacheElementEvictedMostRecentSample();
        }
        return result;
    }

    /**
     * @return aggregate expiration rate
     */
    public long getCacheExpirationRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            SampledCacheStatistics stats = cache.getSampledCacheStatistics();
            result += stats.getCacheElementExpiredMostRecentSample();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheManagerMBean#getName()
     */
    public String getName() {
        return cacheManager.getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheManagerMBean#getName()
     */
    public String getMBeanRegisteredName() {
        return this.mbeanRegisteredName;
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clearStatistics();
            }
        }
        sendNotification(STATISTICS_RESET);
    }

    /**
     * {@inheritDoc}
     */
    public void enableStatistics() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // enables regular statistics also
                cache.setSampledStatisticsEnabled(true);
            }
        }
        sendNotification(STATISTICS_ENABLED, Boolean.TRUE);
    }
    
    /**
     * {@inheritDoc}
     */
    public void disableStatistics() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // disables regular statistics also
                cache.setStatisticsEnabled(false);
            }
        }
        sendNotification(STATISTICS_ENABLED, Boolean.FALSE);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean enabled) {
        if (enabled) {
            enableStatistics();
        } else {
            disableStatistics();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                if (!cache.isSampledStatisticsEnabled()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * @return is each cache's statistics enabled
     */
    private boolean determineStatisticsEnabled() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                if (!cache.isStatisticsEnabled()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * generateActiveConfigDeclaration
     * 
     * @return CacheManager configuration as String
     */
    public String generateActiveConfigDeclaration() {
        return this.cacheManager.getActiveConfigurationText();
    }

    /**
     * generateActiveConfigDeclaration
     * 
     * @return Cache configuration as String
     */
    public String generateActiveConfigDeclaration(String cacheName) {
        return this.cacheManager.getActiveConfigurationText(cacheName);
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommittedCount() {
        return this.cacheManager.getTransactionController().getTransactionCommittedCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRolledBackCount() {
        return this.cacheManager.getTransactionController().getTransactionRolledBackCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionTimedOutCount() {
        return this.cacheManager.getTransactionController().getTransactionTimedOutCount();
    }

    /**
     * Returns if each contained cache is enabled.
     */
    public boolean isEnabled() throws CacheException {
        String[] cacheNames = cacheManager.getCacheNames();

        for (String cacheName : cacheNames) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.isDisabled()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Enables/disables each of the contained caches.
     */
    public void setEnabled(boolean enabled) {
        String[] cacheNames = cacheManager.getCacheNames();

        for (String cacheName : cacheNames) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                cache.setDisabled(!enabled);
            }
        }
        sendNotification(CACHES_ENABLED, Boolean.valueOf(enabled));
    }
    
    /**
     * @return is each cache enabled
     */
    private boolean determineEnabled() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                if (cache.isDisabled()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * @see BaseEmitterBean#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return NOTIFICATION_INFO;
    }
}
