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
package net.sf.ehcache.concurrent;

/**
 * @author Alex Snaps
 */
public interface CacheLockProvider {

    /**
     * Gets the Sync Stripe to use for a given key.
     * <p/>
     * This lookup must always return the same Sync for a given key.
     * <p/>
     * @param key the key
     * @return one of a limited number of Sync's.
     */
    Sync getSyncForKey(Object key);

    /**
     * Gets and write lock the Sync Stripes to use for the given keys.
     * <p/>
     * This lookup must always return the same Sync for a given key.
     * For keys.length > 0, it will return anything between 1 and keys.length Sync's
     * <p/>
     * @param keys the keys to lock and get syncs for
     * @return limited number of write locked Sync's matching the keys.
     */
    Sync[] getAndWriteLockAllSyncForKeys(Object... keys);

    /**
     * Gets and write lock the Sync Stripes to use for the given keys.
     * <p/>
     * This lookup must always return the same Sync for a given key.
     * For keys.length > 0, it will return anything between 1 and keys.length Sync's
     * <p/>
     * If all locks cannot be acquired within the specified timeout they are all
     * released and an exception is thrown.
     * <p/>
     * @param timeout amount of milliseconds before timeout occurs
     * @param keys the keys to lock and get syncs for
     * @return limited number of write locked Sync's matching the keys.
     * @throws LocksAcquisitionException thrown when locks could not be acquired within
     *         specified timeout.
     */
    Sync[] getAndWriteLockAllSyncForKeys(long timeout, Object... keys) throws LocksAcquisitionException;

    /**
     * write unlock the Sync Stripes to use for the given keys.
     * @param keys the keys to unlock
     */
    void unlockWriteLockForAllKeys(Object... keys);
}
