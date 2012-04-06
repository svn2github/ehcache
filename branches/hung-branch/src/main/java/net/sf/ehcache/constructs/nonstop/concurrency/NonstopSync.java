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

import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
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
    private final ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal;
    private final Object key;
    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;
    private final NonstopConfiguration nonstopConfiguration;

    /**
     * Constructor accepting the {@link NonstopStore} and the actual {@link Sync}
     *
     * @param nonstopStore
     * @param nonstopActiveDelegateHolder
     * @param key
     * @param key2
     */
    public NonstopSync(NonstopStore nonstopStore, NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
            ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal, Object key, NonstopConfiguration nonstopConfiguration) {
        this.nonstopStore = nonstopStore;
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
        this.explicitLockingContextThreadLocal = explicitLockingContextThreadLocal;
        this.key = key;
        this.nonstopConfiguration = nonstopConfiguration;
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
                throw new LockOperationTimedOutNonstopException("isHeldByCurrentThread() timed out");
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void lock(final LockType type) {
        boolean acquired = false;
        try {
            acquired = tryLock(type, nonstopConfiguration.getTimeoutMillis());
        } catch (InterruptedException e) {
            // NonStop exception would be thrown automatically as acquired would be false
        }

        if (!acquired) {
            throw new LockOperationTimedOutNonstopException("Lock timed out");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean tryLock(final LockType type, final long msec) throws InterruptedException {
        final ExplicitLockingContext appThreadLockContext = explicitLockingContextThreadLocal.getCurrentThreadLockContext();
        return nonstopStore.executeClusterOperation(new ExplicitLockingClusterOperationImpl(type, msec, appThreadLockContext,
                LockOperationType.TRY_LOCK));
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(final LockType type) {
        final ExplicitLockingContext appThreadLockContext = explicitLockingContextThreadLocal.getCurrentThreadLockContext();
        nonstopStore.executeClusterOperation(new ExplicitLockingClusterOperationImpl(type, -1, appThreadLockContext,
                LockOperationType.UNLOCK));
    }

    /**
     * For try lock and unlock non stop cases
     *
     * @author npurwar
     */
    private class ExplicitLockingClusterOperationImpl implements ExplicitLockingClusterOperation<Boolean> {
        private final AtomicBoolean operationCompleted = new AtomicBoolean(false);
        private final LockType type;
        private final long timeout;
        private final LockOperationType lockOperationType;
        private final ExplicitLockingContext appThreadLockContext;
        private volatile OperationState state = OperationState.EXECUTING;

        public ExplicitLockingClusterOperationImpl(LockType type, long timeout, ExplicitLockingContext appThreadLockContext,
                LockOperationType lockOperationType) {
            this.type = type;
            this.timeout = timeout;
            this.appThreadLockContext = appThreadLockContext;
            this.lockOperationType = lockOperationType;
        }

        public Boolean performClusterOperation() throws Exception {
            final boolean success = lockOperationType.performOperation(appThreadLockContext, nonstopActiveDelegateHolder, key, timeout,
                    type, nonstopConfiguration);
            executionComplete();

            if (!isExecutionComplete()) {
                lockOperationType.rollback(appThreadLockContext, nonstopActiveDelegateHolder, key, type, success);
            }

            return isExecutionComplete() && success;
        }

        public Boolean performClusterOperationTimedOut(final TimeoutBehaviorType configuredTimeoutBehavior) {
            operationTimedOut();

            if (isOperationTimedOut()) {
                // throw exception for all behaviors
                throw new LockOperationTimedOutNonstopException("tryLock() timed out");
            }
            return true;
        }

        private synchronized void executionComplete() {
            this.state = state.executionComplete();
        }

        private synchronized void operationTimedOut() {
            this.state = state.operationTimedOut();
        }

        private synchronized boolean isOperationTimedOut() {
            return state.isOperationTimedOut();
        }

        private synchronized boolean isExecutionComplete() {
            return state.isExecutionComplete();
        }

    }

    /**
     * enum for try lock and unlock opertation types
     *
     * @author npurwar
     */
    private enum LockOperationType {
        TRY_LOCK {
            @Override
            public boolean performOperation(ExplicitLockingContext appThreadLockContext,
                    NonstopActiveDelegateHolder nonstopActiveDelegateHolder, Object key, long timeout, LockType type,
                    NonstopConfiguration config) throws Exception {
                timeout = Math.min(config.getTimeoutMillis(), timeout);
                boolean success = nonstopActiveDelegateHolder.getUnderlyingCacheLockProvider().getSyncForKey(key).tryLock(type, timeout);
                if (success) {
                    appThreadLockContext.lockAcquired(NonstopThreadUniqueIdProvider.getCurrentNonstopThreadUniqueId());
                }
                return success;
            }

            @Override
            public void rollback(ExplicitLockingContext appThreadLockContext, NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
                    Object key, LockType type, boolean success) {
                if (success) {
                    nonstopActiveDelegateHolder.getUnderlyingCacheLockProvider().getSyncForKey(key).unlock(type);
                    appThreadLockContext.lockReleased();
                }
            }
        },
        UNLOCK {
            @Override
            public boolean performOperation(ExplicitLockingContext appThreadLockContext,
                    NonstopActiveDelegateHolder nonstopActiveDelegateHolder, Object key, long timeout, LockType type,
                    NonstopConfiguration config) throws Exception {
                try {
                    if (appThreadLockContext
                            .areLocksAcquiredByOtherThreads(NonstopThreadUniqueIdProvider.getCurrentNonstopThreadUniqueId())) {
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
                return true;
            }

            @Override
            public void rollback(ExplicitLockingContext appThreadLockContext, NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
                    Object key, LockType type, boolean success) {
                // NO OP
            }
        };

        public abstract boolean performOperation(ExplicitLockingContext appThreadLockContext,
                NonstopActiveDelegateHolder nonstopActiveDelegateHolder, Object key, long timeout, LockType type,
                NonstopConfiguration config) throws Exception;

        public abstract void rollback(ExplicitLockingContext appThreadLockContext, NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
                Object key, LockType type, boolean success);
    }

    /**
     * enum for lock state diagram
     *
     * @author npurwar
     *
     */
    private enum OperationState {
        EXECUTING {
            @Override
            OperationState executionComplete() {
                return EXECUTION_COMPLETE;
            }

            @Override
            OperationState operationTimedOut() {
                return OPERATION_TIMED_OUT;
            }
        },
        EXECUTION_COMPLETE {
            @Override
            OperationState executionComplete() {
                throw new UnsupportedOperationException();
            }

            @Override
            OperationState operationTimedOut() {
                return EXECUTION_COMPLETE;
            }

            @Override
            boolean isExecutionComplete() {
                return true;
            }
        },
        OPERATION_TIMED_OUT {
            @Override
            OperationState executionComplete() {
                return OPERATION_TIMED_OUT;
            }

            @Override
            OperationState operationTimedOut() {
                throw new UnsupportedOperationException();
            }

            @Override
            boolean isOperationTimedOut() {
                return true;
            }
        };

        abstract OperationState executionComplete();

        abstract OperationState operationTimedOut();

        boolean isOperationTimedOut() {
            return false;
        }

        boolean isExecutionComplete() {
            return false;
        }
    }

}
