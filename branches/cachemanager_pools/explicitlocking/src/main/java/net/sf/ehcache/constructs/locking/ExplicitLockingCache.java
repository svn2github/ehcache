/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.constructs.locking;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.loader.CacheLoader;

/**
 * See description in package.html
 * 
 * @author Steve Harris
 * @author Greg Luck
 */
public class ExplicitLockingCache extends EhcacheDecoratorAdapter {

    private CacheLockProvider cacheLockProvider;

    /**
     * Constructor for this decorated cache
     * 
     * @param cache
     *            the backing cache
     * @throws CacheException
     */
    public ExplicitLockingCache(Ehcache cache) throws CacheException {
        super(cache);
        Object context = cache.getInternalContext();
        if (context instanceof CacheLockProvider) {
            this.cacheLockProvider = ((CacheLockProvider) context);
        } else {
            this.cacheLockProvider = new StripedReadWriteLockSync(StripedReadWriteLockSync.DEFAULT_NUMBER_OF_MUTEXES);
        }
    }

    /**
     * Clones a cache. This is only legal if the cache has not been initialized. At that point only primitives have been set and no
     * MemoryStore or DiskStore has been created.
     * <p/>
     * A new, empty, RegisteredEventListeners is created on clone.
     * <p/>
     * 
     * @return an object of type {@link net.sf.ehcache.Cache}
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Synchronized version of getName to test liveness of the object lock.
     * <p/>
     * The time taken for this method to return is a useful measure of runtime contention on the cache.
     * 
     * @return the name of the cache.
     */
    public synchronized String liveness() {
        return getName();
    }

    /**
     * Register a {@link CacheLoader} with the cache. It will then be tied into the cache lifecycle.
     * <p/>
     * If the CacheLoader is not initialised, initialise it.
     * 
     * @param cacheLoader
     *            A Cache Loader to register
     */
    @Override
    public void registerCacheLoader(CacheLoader cacheLoader) {
        throw new CacheException("This method is not appropriate for a blocking cache.");
    }

    /**
     * Unregister a {@link CacheLoader} with the cache. It will then be detached from the cache lifecycle.
     * 
     * @param cacheLoader
     *            A Cache Loader to unregister
     */
    @Override
    public void unregisterCacheLoader(CacheLoader cacheLoader) {
        throw new CacheException("This method is not appropriate for a blocking cache.");
    }

    /**
     * Gets the lock for a given key
     * 
     * @param key
     * @return the lock object for the passed in key
     */
    protected Sync getLockForKey(final Object key) {
        return cacheLockProvider.getSyncForKey(key);
    }

    /**
     * Acquires the proper read lock for a given cache key
     * 
     * @param key
     *            - The key that retrieves a value that you want to protect via locking
     */
    public void acquireReadLockOnKey(Object key) {
        this.acquireLockOnKey(key, LockType.READ);
    }

    /**
     * Acquires the proper write lock for a given cache key
     * 
     * @param key
     *            - The key that retrieves a value that you want to protect via locking
     */
    public void acquireWriteLockOnKey(Object key) {
        this.acquireLockOnKey(key, LockType.WRITE);
    }

    // public void acquireWriteLocksOnKeys(Collection<Object> keys) {
    // acquireLocksOnKeys(keys, LockType.WRITE);
    // }
    //
    // public void acquireReadLocksOnKeys(Collection<Object> keys) {
    // acquireLocksOnKeys(keys, LockType.READ);
    // }
    //
    // private void acquireLocksOnKeys(Collection<Object> keys, LockType type) {
    // SortedSet<Object> sortedKeys = new TreeSet<Object>();
    // sortedKeys.addAll(keys);
    //
    // for (Object key : sortedKeys) {
    // acquireLockOnKey(key, type);
    // }
    // }

    private void acquireLockOnKey(Object key, LockType lockType) {
        Sync s = getLockForKey(key);
        s.lock(lockType);
    }

    private void releaseLockOnKey(Object key, LockType lockType) {
        Sync s = getLockForKey(key);
        s.unlock(lockType);
    }

    /**
     * Release a held read lock for the passed in key
     * 
     * @param key
     *            - The key that retrieves a value that you want to protect via locking
     */
    public void releaseReadLockOnKey(Object key) {
        releaseLockOnKey(key, LockType.READ);
    }

    /**
     * Release a held write lock for the passed in key
     * 
     * @param key
     *            - The key that retrieves a value that you want to protect via locking
     */
    public void releaseWriteLockOnKey(Object key) {
        releaseLockOnKey(key, LockType.WRITE);
    }

    // public void releaseReadLocksOnKeys(Set<Object> keys) {
    // releaseLocksOnKeys(keys, LockType.READ);
    // }
    //
    // public void releaseWriteLocksOnKeys(Set<Object> keys) {
    // releaseLocksOnKeys(keys, LockType.WRITE);
    // }
    //
    // private void releaseLocksOnKeys(Set<Object> keys, LockType lockType) {
    // SortedSet<Object> sortedKeys = new TreeSet<Object>();
    // sortedKeys.addAll(keys);
    //
    // for (Object key : keys) {
    // releaseLockOnKey(key, lockType);
    // }
    // }

    /**
     * Try to get a read lock on a given key. If can't get it in timeout millis then return a boolean telling that it didn't get the lock
     * 
     * @param key
     *            - The key that retrieves a value that you want to protect via locking
     * @param timeout
     *            - millis until giveup on getting the lock
     * @return whether the lock was awarded
     * @throws InterruptedException
     */
    public boolean tryReadLockOnKey(Object key, long timeout) throws InterruptedException {
        Sync s = getLockForKey(key);
        return s.tryLock(LockType.READ, timeout);
    }

    /**
     * Try to get a write lock on a given key. If can't get it in timeout millis then return a boolean telling that it didn't get the lock
     * 
     * @param key
     *            - The key that retrieves a value that you want to protect via locking
     * @param timeout
     *            - millis until giveup on getting the lock
     * @return whether the lock was awarded
     * @throws InterruptedException
     */
    public boolean tryWriteLockOnKey(Object key, long timeout) throws InterruptedException {
        Sync s = getLockForKey(key);
        return s.tryLock(LockType.WRITE, timeout);
    }
}
