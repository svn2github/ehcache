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
package net.sf.ehcache.store;

import java.util.concurrent.Callable;

/**
 * This interface is to be implemented by CachingTier that sit above the {@link AuthoritativeTier}.
 * An important contract here is that a value being faulted in {@see #get} is to be entirely faulted in before it
 * can become an eviction candidate, i.e. this cache can never evict mappings being faulted in
 *
 * @param <K>
 * @param <V>
 * @author Alex Snaps
 */
public interface CachingTier<K, V> {

    /**
     * Returns {@code true} if values should be loaded to this cache on put.
     * <p>
     * This may be a dynamic decision, based for example on the occupancy of the cache.
     *
     * @return {@code true} if values should be loaded on put
     */
    boolean loadOnPut();
    
    /**
     * Returns the value associated with the key, or populates the mapping using the Callable instance
     *
     * @param key the key to look up
     * @param source the source to use, in the case of no mapping present
     * @param updateStats true to update the stats, false otherwise
     * @return the value mapped to the key
     */
    V get(K key, Callable<V> source, boolean updateStats);

    /**
     * Removes the mapping associated to the key passed in
     *
     * @param key the key to the mapping to remove
     * @return the value removed, null if none
     */
    V remove(K key);

    /**
     * Clears the cache...
     * Doesn't notify any listeners
     */
    void clear();

    /**
     * Adds a {@link Listener} to the cache
     *
     * @param listener the listener to add
     */
    void addListener(Listener<K, V> listener);

    /**
     * Can we avoid having this somehow ?
     *
     * @return the count of entries held in heap
     */
    @Deprecated
    int getInMemorySize();

    /**
     * Can we avoid having this somehow ?
     *
     * @return the count of entries held off heap
     */
    @Deprecated
    int getOffHeapSize();

    /**
     * This should go away once the stats are in
     * As the method is only there to know what tier the key is going to be fetched from
     *
     * @param key
     * @return
     */
    @Deprecated
    boolean contains(K key);

    /**
     * CacheTier could keep hold of the PoolAccessors for each tier...
     * But what about non pooled resources ?
     *
     * @return
     */
    @Deprecated
    long getInMemorySizeInBytes();

    /**
     * CacheTier could keep hold of the PoolAccessors for each tier...
     * But what about non pooled resources ?
     *
     * @return
     */
    @Deprecated
    long getOffHeapSizeInBytes();
    
    /**
     * CacheTier could keep hold of the PoolAccessors for each tier...
     * But what about non pooled resources ?
     *
     * @return
     */
    @Deprecated
    long getOnDiskSizeInBytes();

    /**
     * This is evil! Don't call this!
     * @param key the key to perform the recalculation for
     */
    @Deprecated
    void recalculateSize(K key);

    /**
     * queries the potential eviction policy for the heap caching tier
     * @return the policy
     */
    @Deprecated
    Policy getEvictionPolicy();

    /**
     * sets the eviction policy on the heap caching tier
     * @param policy the policy to use
     */
    @Deprecated
    void setEvictionPolicy(Policy policy);

    /**
     * A listener that will be notified when eviction of a mapping happens
     *
     * @param <K>
     * @param <V>
     */
    public interface Listener<K, V> {

        /**
         * Invoked when a mapping is evicted.
         *
         * @param key the key evicted
         * @param value the value evicted
         */
        void evicted(K key, V value);
    }
}
