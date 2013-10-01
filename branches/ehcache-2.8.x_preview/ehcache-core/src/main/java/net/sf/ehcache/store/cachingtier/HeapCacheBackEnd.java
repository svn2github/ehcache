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
package net.sf.ehcache.store.cachingtier;

import net.sf.ehcache.store.Policy;

import java.util.Map;
import java.util.Set;

/**
 * A backend to a OnHeapCachingTier
 *
 * It's responsibility, beyond being the actual storage ({@link java.util.concurrent.ConcurrentHashMap CHM} like),
 * is to evict when required.
 *
 * @param <K>
 * @param <V>
 *
 * @see java.util.concurrent.ConcurrentHashMap
 *
 * @author Alex Snaps
 */
public interface HeapCacheBackEnd<K, V> {

    /**
     * Return {@code true} if this tier has enough space for more entries.
     * 
     * @return {@code true} if there is space for more entries.
     */
    boolean hasSpace();
    
    /**
     * Access a key,
     * basically {@link java.util.concurrent.ConcurrentMap#get(Object) CHM.get()}
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    V get(K key);

    /**
     * Basically {@link java.util.concurrent.ConcurrentMap#putIfAbsent(Object, Object)} CHM.putIfAbsent(Object, Object)}, but
     * will evict if required (on successful put)
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         <tt>null</tt> if there was no mapping for the key.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with the key,
     *         if the implementation supports null values.)
     */
    V putIfAbsent(K key, V value);

    /**
     * Basically {@link java.util.concurrent.ConcurrentMap#remove(Object, Object)} CHM.remove(Object, Object)}
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return <tt>true</tt> if the value was removed
     */
    boolean remove(K key, V value);

    /**
     * Basically {@link java.util.concurrent.ConcurrentMap#replace(Object, Object, Object) CHM.remove(Object, Object, Object)}
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return <tt>true</tt> if the value was replaced
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Basically {@link java.util.Map#remove(Object) CHM.remove(Object)}
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    V remove(K key);

    /**
     * Basically {@link java.util.Map#clear() CHM.clear()}
     */
    void clear();

    /**
     * This should go away with the new stats I believe
     * @return the amount of mappings installed
     */
    @Deprecated
    int size();

    /**
     * This should go away with the new stats I believe
     * @return the Set of entries
     */
    @Deprecated
    Set<Map.Entry<K, V>> entrySet();

    /**
     * Let's you register a single callback for evictions
     * @param callback the thing to call back on
     */
    void registerEvictionCallback(EvictionCallback<K, V> callback);

    /**
     * This is evil! Don't call this!
     * @param key
     */
    @Deprecated
    void recalculateSize(K key);

    /**
     * queries the potential eviction policy for the heap caching tier
     * @return the policy
     */
    @Deprecated
    Policy getPolicy();

    /**
     * sets the eviction policy on the heap caching tier
     * @param policy the policy to use
     */
    @Deprecated
    void setPolicy(Policy policy);

    /**
     * An eviction callback
     * @param <K> the key type
     * @param <V> the value type
     */
    public interface EvictionCallback<K, V> {

        /**
         * Called upon eviction (the mapping is gone already)
         * @param key the evicted key
         * @param value the evicted value
         */
        void evicted(K key, V value);
    }
}
