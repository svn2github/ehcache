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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;

/**
 * Provides a number of Sync which allow fine-grained concurrency. Rather than locking a cache or a store,
 * the individual elements or constituent objects can be locked. This dramatically increases
 * the possible concurrency.
 * <p/>
 * The more stripes, the higher the concurrency. To be threadsafe, the instance of CacheLockProvider needs to be
 * maintained for the entire life of the cache or store, so there is some added memory use.
 * <p/>
 * Though a new class, this code has been refactored from <code>BlockingCache</code>, where it has been in use
 * in highly concurrent production environments for years.
 * <p/>
 * Based on the lock striping concept from Brian Goetz. See Java Concurrency in Practice 11.4.3
 * @author Alex Snaps
 */
public class StripedReadWriteLockSync implements CacheLockProvider {

    /**
     * The default number of locks to use. Must be a power of 2.
     * <p/>
     * The choice of 2048 enables 2048 concurrent operations per cache or cache store, which should be enough for most
     * uses.
     */
    public  static final int    DEFAULT_NUMBER_OF_MUTEXES = 2048;

    private final ReadWriteLockSync[] mutexes;
    private final int                 numberOfStripes;

    /**
     * Constructs a striped mutex with the default 2048 stripes.
     */
    public StripedReadWriteLockSync() {
        this(DEFAULT_NUMBER_OF_MUTEXES);
    }

    /**
     * Constructs a striped mutex with the default 2048 stripes.
     * <p/>
     * The number of stripes determines the number of concurrent operations per cache or cache store.
     * @param numberOfStripes - must be a factor of two
     */
    public StripedReadWriteLockSync(int numberOfStripes) {
        if (numberOfStripes % 2 != 0) {
            throw new CacheException("Cannot create a CacheLockProvider with an odd number of stripes");
        }

        if (numberOfStripes == 0) {
            throw new CacheException("A zero size CacheLockProvider does not have useful semantics.");
        }

        this.numberOfStripes = numberOfStripes;
        mutexes = new ReadWriteLockSync[numberOfStripes];

        for (int i = 0; i < numberOfStripes; i++) {
            mutexes[i] = new ReadWriteLockSync();
        }
    }

    /**
     * Gets the Sync Stripe to use for a given key.
     * <p/>
     * This lookup must always return the same Sync for a given key.
     * <p/>
     * @param key the key
     * @return one of a limited number of Sync's.
     */
    public ReadWriteLockSync getSyncForKey(final Object key) {
        int lockNumber = ConcurrencyUtil.selectLock(key, numberOfStripes);
        return mutexes[lockNumber];
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(Object... keys) {
        SortedMap<ReadWriteLockSync, AtomicInteger> locks = getLockMap(keys);

        Sync[] syncs = new Sync[locks.size()];
        int i = 0;
        for (Map.Entry<ReadWriteLockSync, AtomicInteger> entry : locks.entrySet()) {
            while (entry.getValue().getAndDecrement() > 0) {
                entry.getKey().lock(LockType.WRITE);
            }
            syncs[i++] = entry.getKey();
        }
        return syncs;
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(long timeout, Object... keys) throws TimeoutException {
        SortedMap<ReadWriteLockSync, AtomicInteger> locks = getLockMap(keys);

        boolean lockHeld;
        List<ReadWriteLockSync> heldLocks = new ArrayList<ReadWriteLockSync>();

        Sync[] syncs = new Sync[locks.size()];
        int i = 0;
        for (Map.Entry<ReadWriteLockSync, AtomicInteger> entry : locks.entrySet()) {
            while (entry.getValue().getAndDecrement() > 0) {
                try {
                    ReadWriteLockSync writeLockSync = entry.getKey();
                    lockHeld = writeLockSync.tryLock(LockType.WRITE, timeout);
                    if (lockHeld) {
                        heldLocks.add(writeLockSync);
                    }
                } catch (InterruptedException e) {
                    lockHeld = false;
                }

                if (!lockHeld) {
                    for (int j = heldLocks.size() - 1; j >= 0; j--) {
                        ReadWriteLockSync readWriteLockSync = heldLocks.get(j);
                        readWriteLockSync.unlock(LockType.WRITE);
                    }
                    throw new TimeoutException("could not acquire all locks in " + timeout + " ms");
                }
            }
            syncs[i++] = entry.getKey();
        }
        return syncs;
    }

    /**
     * {@inheritDoc}
     */
    public void unlockWriteLockForAllKeys(Object... keys) {
        SortedMap<ReadWriteLockSync, AtomicInteger> locks = getLockMap(keys);

        for (Map.Entry<ReadWriteLockSync, AtomicInteger> entry : locks.entrySet()) {
            while (entry.getValue().getAndDecrement() > 0) {
                entry.getKey().unlock(LockType.WRITE);
            }
        }
    }

    private SortedMap<ReadWriteLockSync, AtomicInteger> getLockMap(final Object... keys) {
        SortedMap<ReadWriteLockSync, AtomicInteger> locks = new TreeMap<ReadWriteLockSync, AtomicInteger>();
        for (Object key : keys) {
            ReadWriteLockSync syncForKey = getSyncForKey(key);
            if (locks.containsKey(syncForKey)) {
                locks.get(syncForKey).incrementAndGet();
            } else {
                locks.put(syncForKey, new AtomicInteger(1));
            }
        }
        return locks;
    }
}
