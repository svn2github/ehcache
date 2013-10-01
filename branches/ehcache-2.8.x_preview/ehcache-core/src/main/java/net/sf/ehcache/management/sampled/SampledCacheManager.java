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

package net.sf.ehcache.management.sampled;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.management.impl.BaseEmitterBean;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

/**
 * An implementation of {@link SampledCacheManagerMBean}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public class SampledCacheManager extends BaseEmitterBean implements SampledCacheManagerMBean {
    private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

    private final CacheManagerSampler sampledCacheManagerDelegate;
    private String mbeanRegisteredName;
    private volatile boolean mbeanRegisteredNameSet;

    static {
        final String[] notifTypes = new String[] {CACHES_ENABLED, CACHES_CLEARED, STATISTICS_ENABLED, STATISTICS_RESET};
        final String name = Notification.class.getName();
        final String description = "Ehcache SampledCacheManager Event";
        NOTIFICATION_INFO = new MBeanNotificationInfo[] {new MBeanNotificationInfo(notifTypes, name, description)};
    }

    /**
     * Constructor taking the backing {@link CacheManager}
     *
     * @param cacheManager the cacheManager to wrap
     * @throws javax.management.NotCompliantMBeanException
     *          if invalid object is registered
     */
    public SampledCacheManager(CacheManager cacheManager) throws NotCompliantMBeanException {
        super(SampledCacheManagerMBean.class);
        sampledCacheManagerDelegate = new CacheManagerSamplerImpl(cacheManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDispose() {

    }

    /**
     * Set the name used to register this mbean. Can be called only once.
     * Package protected method
     *
     * @param name the MBean name to be registered.
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
        sampledCacheManagerDelegate.clearAll();
        sendNotification(CACHES_CLEARED);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getCacheNames() throws IllegalStateException {
        return sampledCacheManagerDelegate.getCacheNames();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return sampledCacheManagerDelegate.getStatus();
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
        return sampledCacheManagerDelegate.getCacheMetrics();
    }

    /**
     * @return aggregate hit rate
     */
    public long getCacheHitRate() {
        return sampledCacheManagerDelegate.getCacheHitRate();
    }

    /**
     * @return aggregate in-memory hit rate
     */
    public long getCacheInMemoryHitRate() {
        return sampledCacheManagerDelegate.getCacheInMemoryHitRate();
    }

    /**
     * @return aggregate off-heap hit rate
     */
    public long getCacheOffHeapHitRate() {
        return sampledCacheManagerDelegate.getCacheOffHeapHitRate();
    }

    /**
     * @return aggregate on-disk hit rate
     */
    public long getCacheOnDiskHitRate() {
        return sampledCacheManagerDelegate.getCacheOnDiskHitRate();
    }

    /**
     * @return aggregate miss rate
     */
    public long getCacheMissRate() {
        return sampledCacheManagerDelegate.getCacheMissRate();
    }

    /**
     * @return aggregate in-memory miss rate
     */
    public long getCacheInMemoryMissRate() {
        return sampledCacheManagerDelegate.getCacheInMemoryMissRate();
    }

    /**
     * @return aggregate off-heap miss rate
     */
    public long getCacheOffHeapMissRate() {
        return sampledCacheManagerDelegate.getCacheOffHeapMissRate();
    }

    /**
     * @return aggregate on-disk miss rate
     */
    public long getCacheOnDiskMissRate() {
        return sampledCacheManagerDelegate.getCacheOnDiskMissRate();
    }

    /**
     * @return aggregate put rate
     */
    public long getCachePutRate() {
        return sampledCacheManagerDelegate.getCachePutRate();
    }

    /**
     * @return aggregate update rate
     */
    public long getCacheUpdateRate() {
        return sampledCacheManagerDelegate.getCacheUpdateRate();
    }

    /**
     * @return aggregate remove rate
     */
    public long getCacheRemoveRate() {
        return sampledCacheManagerDelegate.getCacheRemoveRate();
    }

    /**
     * @return aggregate eviction rate
     */
    public long getCacheEvictionRate() {
        return sampledCacheManagerDelegate.getCacheEvictionRate();
    }

    /**
     * @return aggregate expiration rate
     */
    public long getCacheExpirationRate() {
        return sampledCacheManagerDelegate.getCacheExpirationRate();
    }

    /**
     * @return aggregate average get time (ms.)
     */
    public float getCacheAverageGetTime() {
        return sampledCacheManagerDelegate.getCacheAverageGetTime();
    }

    /**
     * @return aggregate search rate
     */
    public long getCacheSearchRate() {
        return sampledCacheManagerDelegate.getCacheSearchRate();
    }

    /**
     * @return aggregate search time
     */
    public long getCacheAverageSearchTime() {
        return sampledCacheManagerDelegate.getCacheAverageSearchTime();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getHasWriteBehindWriter() {
        return sampledCacheManagerDelegate.getHasWriteBehindWriter();
    }

    /**
     * @return aggregate writer queue length
     */
    public long getWriterQueueLength() {
        return sampledCacheManagerDelegate.getWriterQueueLength();
    }

    /**
     * {@inheritDoc}
     */
    public int getWriterMaxQueueSize() {
        return sampledCacheManagerDelegate.getWriterMaxQueueSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalDisk() {
        return sampledCacheManagerDelegate.getMaxBytesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalDiskAsString() {
        return sampledCacheManagerDelegate.getMaxBytesLocalDiskAsString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDisk(long maxBytes) {
        sampledCacheManagerDelegate.setMaxBytesLocalDisk(maxBytes);
        sendNotification(CACHE_MANAGER_CHANGED, getCacheManagerAttributes(), getName());
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDiskAsString(String maxBytes) {
        sampledCacheManagerDelegate.setMaxBytesLocalDiskAsString(maxBytes);
        sendNotification(CACHE_MANAGER_CHANGED, getCacheManagerAttributes(), getName());
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalHeap() {
        return sampledCacheManagerDelegate.getMaxBytesLocalHeap();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalHeapAsString() {
        return sampledCacheManagerDelegate.getMaxBytesLocalHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeap(long maxBytes) {
        sampledCacheManagerDelegate.setMaxBytesLocalHeap(maxBytes);
        sendNotification(CACHE_MANAGER_CHANGED, getCacheManagerAttributes(), getName());
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeapAsString(String maxBytes) {
        sampledCacheManagerDelegate.setMaxBytesLocalHeapAsString(maxBytes);
        sendNotification(CACHE_MANAGER_CHANGED, getCacheManagerAttributes(), getName());
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalOffHeap() {
        return sampledCacheManagerDelegate.getMaxBytesLocalOffHeap();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalOffHeapAsString() {
        return sampledCacheManagerDelegate.getMaxBytesLocalOffHeapAsString();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheManager#getName()
     */
    public String getName() {
        return sampledCacheManagerDelegate.getName();
    }

    /**
     * @see net.sf.ehcache.management.sampled.SampledCacheManager#getClusterUUID()
     */
    public String getClusterUUID() {
        return sampledCacheManagerDelegate.getClusterUUID();
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
     * generateActiveConfigDeclaration
     *
     * @return CacheManager configuration as String
     */
    public String generateActiveConfigDeclaration() {
        return sampledCacheManagerDelegate.generateActiveConfigDeclaration();
    }

    /**
     * generateActiveConfigDeclaration
     *
     * @return Cache configuration as String
     */
    public String generateActiveConfigDeclaration(String cacheName) {
        return sampledCacheManagerDelegate.generateActiveConfigDeclaration(cacheName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean getTransactional() {
        return sampledCacheManagerDelegate.getTransactional();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getSearchable() {
        return sampledCacheManagerDelegate.getSearchable();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommittedCount() {
        return sampledCacheManagerDelegate.getTransactionCommittedCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommitRate() {
        return sampledCacheManagerDelegate.getTransactionCommitRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRolledBackCount() {
        return sampledCacheManagerDelegate.getTransactionRolledBackCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRollbackRate() {
        return sampledCacheManagerDelegate.getTransactionRollbackRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionTimedOutCount() {
        return sampledCacheManagerDelegate.getTransactionTimedOutCount();
    }

    /**
     * Returns if each contained cache is enabled.
     */
    public boolean isEnabled() throws CacheException {
        return sampledCacheManagerDelegate.isEnabled();
    }

    /**
     * Enables/disables each of the contained caches.
     */
    public void setEnabled(boolean enabled) {
        sampledCacheManagerDelegate.setEnabled(enabled);
        sendNotification(CACHES_ENABLED, enabled);
    }

    /**
     * @see BaseEmitterBean#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return NOTIFICATION_INFO;
    }

    private Map<String, Object> getCacheManagerAttributes() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("MaxBytesLocalHeapAsString", getMaxBytesLocalHeapAsString());
        result.put("MaxBytesLocalOffHeapAsString", getMaxBytesLocalOffHeapAsString());
        result.put("MaxBytesLocalDiskAsString", getMaxBytesLocalDiskAsString());
        result.put("MaxBytesLocalHeap", getMaxBytesLocalHeap());
        result.put("MaxBytesLocalOffHeap", getMaxBytesLocalOffHeap());
        result.put("MaxBytesLocalDisk", getMaxBytesLocalDisk());
        return result;
    }
}
