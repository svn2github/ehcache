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

package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.PoolableStore;

public class BalancedAccessOnHeapPoolEvictor extends AbstractBalancedAccessEvictor<PoolableStore> {

    @Override
    protected boolean evict(PoolableStore store, int count, long size) {
        return store.evictFromOnHeap(count, size);
    }

    @Override
    protected long countSize(PoolableStore store) {
        return store.getInMemorySize();
    }

    @Override
    protected long byteSize(PoolableStore store) {
        return store.getInMemorySizeInBytes();
    }

    @Override
    protected float hitRate(PoolableStore store) {
        return store.getApproximateHeapHitRate();
    }

    @Override
    protected float missRate(PoolableStore store) {
        return store.getApproximateHeapMissRate();
    }
}
