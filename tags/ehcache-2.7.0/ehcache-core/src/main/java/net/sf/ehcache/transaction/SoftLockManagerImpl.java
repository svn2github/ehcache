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
package net.sf.ehcache.transaction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A SoftLockFactory implementation which creates soft locks with Read-Committed isolation level
 *
 * @author Ludovic Orban
 */
public class SoftLockManagerImpl extends AbstractSoftLockManager {

    // actually all we need would be a ConcurrentSet...
    private final ConcurrentMap<SoftLockID, Boolean> newKeyLocks = new ConcurrentHashMap<SoftLockID, Boolean>();

    private final ConcurrentMap<SoftLockID, SoftLock> allLocks = new ConcurrentHashMap<SoftLockID, SoftLock>();

    /**
     * Create a new ReadCommittedSoftLockFactoryImpl instance for a cache
     * @param cacheName the name of the cache
     */
    public SoftLockManagerImpl(String cacheName, SoftLockFactory lockFactory) {
        super(cacheName, lockFactory);
    }

    @Override
    protected ConcurrentMap<SoftLockID, SoftLock> getAllLocks() {
        return allLocks;
    }

    @Override
    protected ConcurrentMap<SoftLockID, Boolean> getNewKeyLocks() {
        return newKeyLocks;
    }
}
