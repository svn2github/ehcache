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
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;
import net.sf.ehcache.constructs.nonstop.store.NonstopTimeoutBehaviorType;

/**
 * Class implementing {@link Sync} and that can be executed without getting stuck.
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopSync implements Sync {

    private final NonstopStore nonstopStore;
    private final Sync delegateSync;

    /**
     * Constructor accepting the {@link NonstopStore} and the actual {@link Sync}
     *
     * @param nonstopStore
     * @param delegateSync
     */
    public NonstopSync(NonstopStore nonstopStore, Sync delegateSync) {
        this.nonstopStore = nonstopStore;
        this.delegateSync = delegateSync;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHeldByCurrentThread(final LockType type) {
        return nonstopStore.executeClusterOperation(new ClusterOperation<Boolean>() {

            public Boolean performClusterOperation() {
                return delegateSync.isHeldByCurrentThread(type);
            }

            public Boolean performClusterOperationTimedOut(final NonstopTimeoutBehaviorType configuredTimeoutBehavior) {
                switch (configuredTimeoutBehavior) {
                    case EXCEPTION_ON_TIMEOUT:
                        throw new NonStopCacheException("isHeldByCurrentThread timed out");
                    case LOCAL_READS_ON_TIMEOUT:
                    case NO_OP_ON_TIMEOUT:
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
        nonstopStore.executeClusterOperation(new ClusterOperation<Void>() {

            public Void performClusterOperation() {
                delegateSync.lock(type);
                return null;
            }

            public Void performClusterOperationTimedOut(final NonstopTimeoutBehaviorType configuredTimeoutBehavior) {
                switch (configuredTimeoutBehavior) {
                    case EXCEPTION_ON_TIMEOUT:
                        throw new NonStopCacheException("lock timed out");
                    case LOCAL_READS_ON_TIMEOUT:
                    case NO_OP_ON_TIMEOUT:
                        // no-op
                        return null;
                    default:
                        throw new NonStopCacheException("unknown nonstop timeout behavior type: " + configuredTimeoutBehavior);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public boolean tryLock(final LockType type, final long msec) throws InterruptedException {
        return nonstopStore.executeClusterOperation(new ClusterOperation<Boolean>() {

            public Boolean performClusterOperation() throws Exception {
                return delegateSync.tryLock(type, msec);
            }

            public Boolean performClusterOperationTimedOut(final NonstopTimeoutBehaviorType configuredTimeoutBehavior) {
                switch (configuredTimeoutBehavior) {
                    case EXCEPTION_ON_TIMEOUT:
                        throw new NonStopCacheException("tryLock timed out");
                    case LOCAL_READS_ON_TIMEOUT:
                    case NO_OP_ON_TIMEOUT:
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
    public void unlock(final LockType type) {
        nonstopStore.executeClusterOperation(new ClusterOperation<Void>() {

            public Void performClusterOperation() {
                delegateSync.unlock(type);
                return null;
            }

            public Void performClusterOperationTimedOut(final NonstopTimeoutBehaviorType configuredTimeoutBehavior) {
                switch (configuredTimeoutBehavior) {
                    case EXCEPTION_ON_TIMEOUT:
                        throw new NonStopCacheException("unlock timed out");
                    case LOCAL_READS_ON_TIMEOUT:
                    case NO_OP_ON_TIMEOUT:
                        // no-op
                        return null;
                    default:
                        throw new NonStopCacheException("unknown nonstop timeout behavior type: " + configuredTimeoutBehavior);
                }
            }
        });
    }

}
