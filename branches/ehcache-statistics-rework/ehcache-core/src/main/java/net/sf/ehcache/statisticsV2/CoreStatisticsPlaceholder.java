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

package net.sf.ehcache.statisticsV2;

public class CoreStatisticsPlaceholder implements CoreStatistics {

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#calculateInMemorySize()
     */
    @Override
    public long calculateInMemorySize() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getMemoryStoreSize()
     */
    @Override
    public long getMemoryStoreSize() {
        throw new UnsupportedOperationException();
   }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getDiskStoreSize()
     */
    @Override
    public int getDiskStoreSize() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#calculateOffHeapSize()
     */
    @Override
    public long calculateOffHeapSize() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getOffHeapStoreSize()
     */
    @Override
    public long getOffHeapStoreSize() {
        throw new UnsupportedOperationException();
   }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getInMemoryHits()
     */
    @Override
    public long getInMemoryHits() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getInMemoryMisses()
     */
    @Override
    public long getInMemoryMisses() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getOnDiskHits()
     */
    @Override
    public long getOnDiskHits() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getOnDiskMisses()
     */
    @Override
    public long getOnDiskMisses() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getEvictionCount()
     */
    @Override
    public long getEvictionCount() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getCacheHits()
     */
    @Override
    public long getCacheHits() {
        throw new UnsupportedOperationException();

    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getCacheMisses()
     */
    @Override
    public long getCacheMisses() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getLocalHeapSizeInBytes()
     */
    @Override
    public long getLocalHeapSizeInBytes() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getObjectCount()
     */
    @Override
    public long getObjectCount() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getMemoryStoreObjectCount()
     */
    @Override
    public long getMemoryStoreObjectCount() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getDiskStoreObjectCount()
     */
    @Override
    public long getDiskStoreObjectCount() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#clearStatistics()
     */
    @Override
    public void clearStatistics() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getAssociatedCacheName()
     */
    @Override
    public String getAssociatedCacheName() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getLocalHeapSize()
     */
    @Override
    public String getLocalHeapSizeString() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getMaxGetTimeMillis()
     */
    @Override
    public long getMaxGetTimeMillis() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getMinGetTimeMillis()
     */
    @Override
    public long getMinGetTimeMillis() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getAverageGetTimeMillis()
     */
    @Override
    public float getAverageGetTimeMillis() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalHeapSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOffHeapHits() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOffHeapMisses() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getWriterQueueSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOffHeapStoreObjectCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getInMemoryHitCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getInMemoryMissCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOffHeapHitCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOffHeapMissCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getExpiredCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getWriterQueueLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getXaCommitCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getXaRollbackCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getXaRecoveredCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOnDiskMissCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheMissCountExpired() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getEvictedCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOnDiskHitCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalDiskSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalOffHeapSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalDiskSizeInBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalOffHeapSizeInBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getPutCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getRemovedCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getUpdateCount() {
        throw new UnsupportedOperationException();
    }


}
