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

package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolParticipant;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * A pool which strictly obeys to its bound: it will never allow the accessors to consume more bytes than what
 * has been configured.
 *
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public class StrictlyBoundedPool extends AbstractPool {

    /**
     * Create a StrictlyBoundedPool instance
     *
     * @param maximumPoolSize the maximum size of the pool, in bytes.
     * @param evictor the pool evictor, for cross-store eviction.
     * @param defaultSizeOfEngine the default SizeOf engine used by the accessors.
     */
    public StrictlyBoundedPool(long maximumPoolSize, PoolEvictor evictor, SizeOfEngine defaultSizeOfEngine) {
        super(maximumPoolSize, evictor, defaultSizeOfEngine);
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolParticipant participant, SizeOfEngine sizeOfEngine) {
        LockedPoolAccessor accessor = new LockedPoolAccessor(this, participant, sizeOfEngine, 0);
        registerPoolAccessor(accessor);
        return accessor;
    }
}
