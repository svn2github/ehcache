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

import java.util.Collection;

/**
 * PoolEvictors are responsible for finding the best candidates in a collection of resources using a shared
 * resource pool and performing eviction on them.
 *
 * @param <T> The type of the resources to free space on.
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public interface PoolEvictor<T extends PoolParticipant> {

    /**
     * Free at least N bytes from a collection of resources
     *
     * @param from a collection of resources to free from
     * @param bytes the number of bytes to free up
     * @return true if at least N bytes could be freed
     */
    boolean freeSpace(Collection<PoolAccessor<T>> from, long bytes);

}
