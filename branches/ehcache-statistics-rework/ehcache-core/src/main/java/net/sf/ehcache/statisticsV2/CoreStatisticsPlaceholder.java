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
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#xaCommit()
     */
    @Override
    public void xaCommit() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#xaRecovered(int)
     */
    @Override
    public void xaRecovered(int size) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#xaRollback()
     */
    @Override
    public void xaRollback() {
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
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getgetCacheHits()
     */
    @Override
    public long getgetCacheHits() {
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
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getCacheMissCount()
     */
    @Override
    public long getCacheMissCount() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getCacheHitCount()
     */
    @Override
    public long getCacheHitCount() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getMaxGetTimeMillis()
     */
    @Override
    public long getMaxGetTimeMillis() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getMinGetTimeMillis()
     */
    @Override
    public long getMinGetTimeMillis() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.CoreStatistics#getAverageGetTimeMillis()
     */
    @Override
    public float getAverageGetTimeMillis() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLocalHeapSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getOffHeapHits() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getOffHeapMisses() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getWriterQueueSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getOffHeapStoreObjectCount() {
        // TODO Auto-generated method stub
        return 0;
    }

}
