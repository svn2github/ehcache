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

public class CoreStatisticsPlaceholder {

    public long calculateInMemorySize() {
        throw new UnsupportedOperationException();
    }

    public long getMemoryStoreSize() {
        throw new UnsupportedOperationException();
   }

    public int getDiskStoreSize() {
        throw new UnsupportedOperationException();
    }

    public long calculateOffHeapSize() {
        throw new UnsupportedOperationException();
    }

    public long getOffHeapStoreSize() {
        throw new UnsupportedOperationException();
   }

    public long getInMemoryHits() {
        throw new UnsupportedOperationException();
    }

    public long getInMemoryMisses() {
        throw new UnsupportedOperationException();
    }

    public long getOnDiskHits() {
        throw new UnsupportedOperationException();
    }

    public long getOnDiskMisses() {
        throw new UnsupportedOperationException();
    }

    public long getEvictionCount() {
        throw new UnsupportedOperationException();
    }

    public void xaCommit() {
        throw new UnsupportedOperationException();
    }

    public void xaRecovered(int size) {
        throw new UnsupportedOperationException();
    }

    public void xaRollback() {
        throw new UnsupportedOperationException();
    }

    public Object getCacheHits() {
        throw new UnsupportedOperationException();

    }

    public Object getCacheMisses() {
        throw new UnsupportedOperationException();
    }

}
