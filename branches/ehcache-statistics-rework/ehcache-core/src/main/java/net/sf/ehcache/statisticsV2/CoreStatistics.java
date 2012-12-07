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

public interface CoreStatistics {

    public abstract long calculateInMemorySize();

    public abstract long getMemoryStoreSize();

    public abstract int getDiskStoreSize();

    public abstract long calculateOffHeapSize();

    public abstract long getOffHeapStoreSize();

    public abstract long getInMemoryHits();

    public abstract long getInMemoryMisses();

    public abstract long getOnDiskHits();

    public abstract long getOnDiskMisses();

    public abstract long getEvictionCount();

    public abstract void xaCommit();

    public abstract void xaRecovered(int size);

    public abstract void xaRollback();

    public abstract long getCacheHits();

    public abstract long getCacheMisses();

    public abstract long getLocalHeapSizeInBytes();

    public abstract long getgetCacheHits();

    public abstract long getObjectCount();

    public abstract long getMemoryStoreObjectCount();

    public abstract long getDiskStoreObjectCount();

    public abstract void clearStatistics();

    public abstract String getAssociatedCacheName();

    public abstract long getLocalHeapSize();

    public abstract long getCacheMissCount();

    public abstract long getCacheHitCount();

    public abstract long getMaxGetTimeMillis();

    public abstract long getMinGetTimeMillis();

    public abstract float getAverageGetTimeMillis();

    public abstract long getOffHeapHits();

    public abstract long getOffHeapMisses();

    public abstract long getWriterQueueSize();

    public abstract long getOffHeapStoreObjectCount();

    String getLocalHeapSizeString();

    public abstract long getInMemoryHitCount();

    public abstract long getInMemoryMissCount();

    public abstract Object getOffHeapHitCount();

    public abstract Object getOffHeapMissCount();

    public abstract String getExpiredCount();

}