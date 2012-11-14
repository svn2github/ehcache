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
 */
public interface PoolParticipant {

    /**
     * Perform eviction to release on-heap resources
     *
     * @param count the number of elements to evict
     * @param size the size in bytes to free (hint)
     * @return true if the requested number of elements could be evicted
     */
    boolean evictFromOnHeap(int count, long size);

    /**
     * Perform eviction to release on-disk resources
     *
     * @param count the number of elements to evict
     * @param size the size in bytes to free (hint)
     * @return true if the requested number of elements could be evicted
     */
    boolean evictFromOnDisk(int count, long size);

    /**
     * Gets the size of the on-disk portion of the store, in bytes.
     *
     * @return the on-disk size of the store in bytes
     * @deprecated Revisit once Montreal design is in
     */
    @Deprecated
    long getOnDiskSizeInBytes();

    /**
     * Gets the size of the in-memory portion of the store, in bytes.
     * <p/>
     * This method may be expensive to run, depending on implementation. Implementers may choose to return
     * an approximate size.
     *
     * @return the approximate in-memory size of the store in bytes
     * @deprecated Revisit once Montreal design is in
     */
    @Deprecated
    long getInMemorySizeInBytes();

    /**
     * Return the approximate disk hit rate
     *
     * @return the approximate disk hit rate
     */
    float getApproximateDiskHitRate();

    /**
     * Return the approximate disk miss rate
     *
     * @return the approximate disk miss rate
     */
    float getApproximateDiskMissRate();

    /**
     * Return the approximate disk size
     *
     * @return the approximate disk size
     */
    long getApproximateDiskCountSize();
    
    /**
     * Return the approximate disk size in bytes
     *
     * @return the approximate disk size in bytes
     */
    long getApproximateDiskByteSize();
    
    /**
     * Return the approximate heap hit rate
     *
     * @return the approximate heap hit rate
     */
    float getApproximateHeapHitRate();

    /**
     * Return the approximate heap miss rate
     *
     * @return the approximate heap miss rate
     */
    float getApproximateHeapMissRate();

    /**
     * Return the approximate heap size
     *
     * @return the approximate heap size
     */
    long getApproximateHeapCountSize();

    /**
     * Return the approximate heap size in bytes
     *
     * @return the approximate heap size in bytes
     */
    long getApproximateHeapByteSize();
}
