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

import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;

/**
 * Class implementing {@link Sync} and that can be executed without getting stuck.
 *
 * @author Abhishek Sanoujam
 *
 */
class NonstopSync implements Sync {

    private final NonstopStore nonstopStore;
    // private final Sync delegateSync;
    private final ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal = ExplicitLockingContextThreadLocal.getInstance();
    private final Object key;
    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;

    /**
     * Constructor accepting the {@link NonstopStore} and the actual {@link Sync}
     *
     * @param nonstopStore
     * @param nonstopActiveDelegateHolder
     * @param key
     */
    public NonstopSync(NonstopStore nonstopStore, NonstopActiveDelegateHolder nonstopActiveDelegateHolder, Object key) {
        this.nonstopStore = nonstopStore;
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
        this.key = key;
    }

    /**
     * Return the key associated with this {@link Sync}
     *
     * @return the key associated with this {@link Sync}
     */
    public Object getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHeldByCurrentThread(final LockType type) {
        return nonstopStore.executeClusterOperation(new ExplicitLockingClusterOperation<Boolean>() {

            public Boolean performClusterOperation() {
                return nonstopActiveDelegateHolder.getUnderlyingCacheLockProvider().getSyncForKey(key).isHeldByCurrentThread(type);
            }

            public Boolean performClusterOperationTimedOut(final TimeoutBehaviorType configuredTimeoutBehavior) {
                switch (configuredTimeoutBehavior) {
                    case EXCEPTION:
                        throw new NonStopCacheException("isHeldByCurrentThread timed out");
                    case LOCAL_READS:
                    case NOOP:
                        return false;
                    default:
                        throw new NonStopCacheException("unknown nonstop timeout behavior type: " + configuredTimeoutBehavior);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void lock(final LockType type) {
        final ExplicitLockingContext appThreadLockContext = explicitLockingContextThreadLocal.getCurrentThreadLockContext();
        nonstopStore.executeClusterOperation(new ExplicitLockingClusterOperation<Void>() {

            public Void performClusterOperation() {
                nonstopActiveDelegateHolder.getUnderlyingCacheLockProvider().getSyncForKey(key).lock(type);
                appThreadLockContext.lockAcquired(NonstopThreadUniqueIdProvider.getCurrentNonstopThreadUniqueId());
                return null;
            }

            public Void performClusterOperationTimedOut(final TimeoutBehaviorType configuredTimeoutBehavior) {
                // throw exception for all behaviors
                throw new NonStopCacheException("lock() timed out");
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public boolean tryLock(final LockType type, final long msec) throws InterruptedException {
        final ExplicitLockingContext appThreadLockContext = explicitLockingContextThreadLocal.getCurrentThreadLockContext();
        return nonstopStore.executeClusterOperation(new ExplicitLockingClusterOperation<Boolean>() {

            public Boolean performClusterOperation() throws Exception {
                final boolean lockAcquired = nonstopActiveDelegateHolder.getUnderlyingCacheLockProvider().getSyncForKey(key)
                        .tryLock(type, msec);
                if (lockAcquired) {
                    appThreadLockContext.lockAcquired(NonstopThreadUniqueIdProvider.getCurrentNonstopThreadUniqueId());
                }
                return lockAcquired;
            }

            public Boolean performClusterOperationTimedOut(final TimeoutBehaviorType configuredTimeoutBehavior) {
                // throw exception for all behaviors
                throw new NonStopCacheException("tryLock() timed out");
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(final LockType type) {
        final ExplicitLockingContext appThreadLockContext = explicitLockingContextThreadLocal.getCurrentThreadLockContext();
        nonstopStore.executeClusterOperation(new ExplicitLockingClusterOperation<Void>() {

            public Void performClusterOperation() {
                try {
                    if (appThreadLockContext.areLocksAcquiredByOtherThreads(NonstopThreadUniqueIdProvider.getCurrentNonstopThreadUniqueId())) {
                        // some other thread has acquired locks other than this current nonstop thread
                        // this means rejoin has happened, lock acquired by previous nonstop thread
                        throw new InvalidLockStateAfterRejoinException();
                    } else {
                        nonstopActiveDelegateHolder.getUnderlyingCacheLockProvider().getSyncForKey(key).unlock(type);
                    }
                } finally {
                    // clean up the lock stack anyway
                    appThreadLockContext.lockReleased();
                }
                return null;
            }

            public Void performClusterOperationTimedOut(final TimeoutBehaviorType configuredTimeoutBehavior) {
                // always clean up lock stack for unlock even on timeouts
                appThreadLockContext.lockReleased();
                // throw exception for all behaviors
                throw new NonStopCacheException("unlock() timed out");
            }
        });
    }

}
