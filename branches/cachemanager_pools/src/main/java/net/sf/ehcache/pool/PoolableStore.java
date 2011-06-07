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

package net.sf.ehcache.pool;

import net.sf.ehcache.store.Store;

/**
 * @author Ludovic Orban
 */
public interface PoolableStore extends Store {

    boolean evictFromOnHeap(int count, long size);

    boolean evictFromOffHeap(int count, long size);

    boolean evictFromOnDisk(int count, long size);

    float getApproximateDiskHitRate();

    float getApproximateDiskMissRate();

    float getApproximateHeapHitRate();

    float getApproximateHeapMissRate();
}
