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

package net.sf.ehcache.pool;

/**
 * A poolable store reports its resource usage to a {@link Pool}.
 *
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public interface PoolParticipant {

    /**
     * Perform eviction to release resources
     *
     * @param count the number of elements to evict
     * @param size the size in bytes to free (hint)
     * @return true if the requested number of elements could be evicted
     */
    boolean evict(int count, long size);

    /**
     * Return the approximate hit rate
     *
     * @return the approximate hit rate
     */
    float getApproximateHitRate();

    /**
     * Return the approximate miss rate
     *
     * @return the approximate miss rate
     */
    float getApproximateMissRate();

    /**
     * Return the approximate size
     *
     * @return the approximate size
     */
    long getApproximateCountSize();
    
}
