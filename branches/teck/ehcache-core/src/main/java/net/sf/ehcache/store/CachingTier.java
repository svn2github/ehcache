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
 * @param <K>
 * @param <V>
 * @author Alex Snaps
 */
public interface CachingTier<K, V> {

    /**
     * Document me
     *
     * @param key
     * @param source
     * @param updateStats
     * @return
     */
    V get(K key, Callable<V> source, boolean updateStats);

    /**
     * Document me
     *
     * @param key
     */
    V remove(K key);

    /**
     * Clears the cache...
     * todo should this notify listeners ever ?
     */
    void clear();

//  V putIfAbsent(K key, V value);

//  boolean replace(K key, V oldValue, V newValue);

    /**
     * Document me
     *
     * @param listener
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
    long getOnDiskSizeInBytes();

    /**
     * Document me
     *
     * @param <K>
     * @param <V>
     */
    public interface Listener<K, V> {

        /**
         * Document me
         *
         * @param key
         * @param value
         */
        void evicted(K key, V value);
    }
}
