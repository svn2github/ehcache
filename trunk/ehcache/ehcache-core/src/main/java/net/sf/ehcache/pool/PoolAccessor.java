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
 * PoolAccessors are used by stores to tell the pools about their resource consumption
 *
 * @param <T> Type representing this "other" side of this accessor (i.e. a store), so the evictor can interact with it
 *
 * @author Ludovic Orban
 * @author Alex Snaps
 *
 */
public interface PoolAccessor<T extends PoolParticipant> {

    /**
     * Add an element to the pool.
     *
     * @param key the key of the element
     * @param value the value of the element
     * @param container the element-container object
     * @param force true if the pool should accept adding the element, even if it's out of resources
     * @return how many bytes have been added to the pool or -1 if add failed.
     */
    long add(Object key, Object value, Object container, boolean force);

    /**
     * Check if there is enough room in the pool to add an element without provoking any eviction
     * @param key the key of the element
     * @param value the value of the element
     * @param container the element-container object
     * @return true if there is enough room left
     */
    boolean canAddWithoutEvicting(Object key, Object value, Object container);

    /**
     * Delete a fixed number of bytes from the pool.
     *
     * @param size number of bytes
     * @return how many bytes have been freed from the pool.
     * @throws IllegalArgumentException when sizeOf is negative
     */
    long delete(long size) throws IllegalArgumentException;

    /**
     * Delete a fixed number of bytes from the pool with the given objects.
     *
     * @param currentSize the size of the object(s) being replaced
     * @param key the key of the element
     * @param value the value of the element
     * @param container the element-container object
     * @param force true if the pool should accept replacing the element, even if it's out of resources
     * @return the change in size of the pool, or {@link Long#MIN_VALUE} if replace failed.
     */
    long replace(long currentSize, Object key, Object value, Object container, boolean force);

    /**
     * Return how many bytes this accessor consumes from the pool.
     *
     * @return how many bytes this accessor consumes from the pool.
     */
    long getSize();

    /**
     * unlink this PoolAccessor from its pool.
     */
    void unlink();

    /**
     * Free resources used by this accessor.
     */
    void clear();

    /**
     * Return the store that uses this accessor
     *
     * @return store using this accessor
     */
    T getParticipant();

    /**
     * Sets the max size for this pool
     *
     * @param newValue the value in bytes
     */
    void setMaxSize(long newValue);

    /**
     * Returns the occupied size for this pool.
     * 
     * @return occupied pool size
     */
    long getPoolOccupancy();

    /**
     * Returns the size of this pool.
     * 
     * @return pool size
     */
    long getPoolSize();

    /**
     * Check if the store may contain elements which the SizeOf engine could not fully size.
     *
     * @return true if the store may contain partially sized objects
     */
    boolean hasAbortedSizeOf();

}
