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

/**
 * Pools are used to track shared resource consumption. Each store participating in a pool creates an accessor
 * which it uses to tell the pool about its consumption. A SizeOf engine is used to calculate the size of the
 * objects added to the pool.
 *
 * @author Ludovic Orban
 */
public interface Pool {

    /**
     * Return the total size of the pool.
     *
     * @return total size of the pool.
     */
    long getSize();

    /**
     * Return a PoolAccessor whose consumption is tracked by this pool, using a default SizeOf engine.
     *
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor createPoolAccessor(PoolableStore store);

    /**
     * Return a PoolAccessor whose consumption is tracked by this pool, using a specific SizeOf engine.
     *
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor createPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine);

}
