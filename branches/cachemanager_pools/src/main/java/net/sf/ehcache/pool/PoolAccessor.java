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
 * @author Ludovic Orban
 */
public interface PoolAccessor {

    /**
     * @return how many bytes have been added to the pool or -1 if add failed.
     */
    long add(Object key, Object value, Object container, boolean force);

    /**
     * @return how many bytes have been freed from the pool.
     */
    long delete(Object key, Object value, Object container);

    /**
     * @return how many bytes have been freed from the pool, may be negative. Long.MAX_VALUE is returned if replace failed.
     */
    long replace(Role role, Object current, Object replacement, boolean force);

    /**
     * @return how many bytes this accessor consumes from the pool.
     */
    long getSize();

    /**
     * unlink this PoolAccessor from its pool.
     */
    void unlink();

    /**
     * Free memory used by this accessor.
     */
    void clear();
}
