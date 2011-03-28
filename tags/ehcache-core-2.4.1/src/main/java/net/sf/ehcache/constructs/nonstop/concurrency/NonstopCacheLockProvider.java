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

package net.sf.ehcache.constructs.nonstop.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;

/**
 *
 * Class implementing {@link CacheLockProvider} with nonstop feature
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopCacheLockProvider implements CacheLockProvider {

    private final NonstopStore nonstopStore;
    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;
    private final ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal;

    /**
     * Public constructor
     *
     * @param nonstopStore
     * @param nonstopActiveDelegateHolder
     * @param explicitLockingContextThreadLocal
     */
    public NonstopCacheLockProvider(NonstopStore nonstopStore, NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
            ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal) {
        this.nonstopStore = nonstopStore;
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
        this.explicitLockingContextThreadLocal = explicitLockingContextThreadLocal;
    }

    /**
     * {@inheritDoc}
     */
    public Sync getSyncForKey(Object key) {
        return new NonstopSync(nonstopStore, nonstopActiveDelegateHolder, explicitLockingContextThreadLocal, key);
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(Object... keys) {
        final NonstopSync[] syncs = new NonstopSync[keys.length];
        for (int i = 0; i < keys.length; i++) {
            syncs[i] = new NonstopSync(nonstopStore, nonstopActiveDelegateHolder, explicitLockingContextThreadLocal, keys[i]);
        }
        InvalidLockStateAfterRejoinException invalidLockStateException = null;
        final List<Object> lockedKeys = new ArrayList<Object>();
        for (NonstopSync nonstopSync : syncs) {
            try {
                nonstopSync.lock(LockType.WRITE);
                lockedKeys.add(nonstopSync.getKey());
            } catch (NonStopCacheException e) {
                // release all acquired locks and throw exception
                try {
                    this.unlockWriteLockForAllKeys(lockedKeys.toArray());
                } catch (Exception ignored) {
                    // ignore any exceptions on unlock, as going to throw exception anyway
                }
                throw new NonStopCacheException("WRITE lock could not be acquired for all keys, couldn't acquire lock starting at : "
                        + nonstopSync.getKey(), e);
            }
        }
        return syncs;
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(long timeout, Object... keys) throws TimeoutException {
        final NonstopSync[] syncs = new NonstopSync[keys.length];
        for (int i = 0; i < keys.length; i++) {
            syncs[i] = new NonstopSync(nonstopStore, nonstopActiveDelegateHolder, explicitLockingContextThreadLocal, keys[i]);
        }
        final List<Object> lockedKeys = new ArrayList<Object>();
        for (NonstopSync nonstopSync : syncs) {
            try {
                nonstopSync.tryLock(LockType.WRITE, timeout);
                lockedKeys.add(nonstopSync.getKey());

            } catch (InterruptedException e) {
                try {
                    this.unlockWriteLockForAllKeys(lockedKeys.toArray());
                } catch (Exception ignored) {
                    // ignore any exceptions on unlock, as going to throw exception anyway
                }
                throw new TimeoutException("Caught InterruptedException while trying to acquire lock for key: " + nonstopSync.getKey());
            } catch (NonStopCacheException e) {
                try {
                    this.unlockWriteLockForAllKeys(lockedKeys.toArray());
                } catch (Exception ignored) {
                    // ignore any exceptions on unlock, as going to throw exception anyway
                }
                throw new NonStopCacheException("WRITE lock could not be acquired for all keys, couldn't acquire lock starting at : "
                        + nonstopSync.getKey(), e);
            }
        }
        return syncs;
    }

    /**
     * {@inheritDoc}
     */
    public void unlockWriteLockForAllKeys(Object... keys) {
        final NonstopSync[] syncs = new NonstopSync[keys.length];
        for (int i = 0; i < keys.length; i++) {
            syncs[i] = new NonstopSync(nonstopStore, nonstopActiveDelegateHolder, explicitLockingContextThreadLocal, keys[i]);
        }
        InvalidLockStateAfterRejoinException invalidLockStateException = null;
        final List<Object> invalidStateKeys = new ArrayList<Object>();
        for (NonstopSync nonstopSync : syncs) {
            try {
                nonstopSync.unlock(LockType.WRITE);
            } catch (InvalidLockStateAfterRejoinException e) {
                // keep unlocking all locks to clean up state even after exception
                invalidLockStateException = e;
                invalidStateKeys.add(nonstopSync.getKey());
            }
        }
        if (invalidLockStateException != null) {
            throw new InvalidLockStateAfterRejoinException("Some locks were invalid on unlock - " + invalidStateKeys,
                    invalidLockStateException);
        }
    }
}
