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

package net.sf.ehcache.constructs.nonstop.concurrency;

import java.util.concurrent.Callable;

import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.ClusterOperationCallable;
import net.sf.ehcache.constructs.nonstop.ThrowTimeoutException;

/**
 *
 * Callable used with nonstop when explicit lock has been acquired
 *
 * @author Abhishek Sanoujam
 *
 * @param <V>
 */
public class CacheOperationUnderExplicitLockCallable<V> implements Callable<V> {

    private final Callable<V> cacheOperationCallable;
    private final ExplicitLockingContext appThreadLockContext;
    private final NonstopConfiguration nonstopConfiguration;

    /**
     * public constructor
     *
     * @param currentThreadLockContext
     * @param nonstopConfiguration
     * @param callable
     */
    public CacheOperationUnderExplicitLockCallable(ExplicitLockingContext currentThreadLockContext,
            NonstopConfiguration nonstopConfiguration, Callable<V> callable) {
        appThreadLockContext = currentThreadLockContext;
        this.nonstopConfiguration = nonstopConfiguration;
        cacheOperationCallable = callable;
    }

    /**
     * {@inheritDoc}
     */
    public V call() throws Exception {
        if (isExplicitLockApi()) {
            // always invoke directly the explicit locking apis
            return cacheOperationCallable.call();
        }
        if (appThreadLockContext.areLocksAcquiredByOtherThreads(NonstopThreadUniqueIdProvider.getCurrentNonstopThreadUniqueId())) {
            // invalid lock state, some other thread has acquired explicit locks for the app thread corresponding
            // to this nonstop thread, which can only be another nonstop thread before rejoin.
            // Meaning rejoin happened and as locks are invalid after rejoin, throw exception if configured
            switch (nonstopConfiguration.getTimeoutBehavior().getTimeoutBehaviorType()) {
                case NOOP:
                case LOCAL_READS:
                    // for noop and localReads, make the executor service rethrow as TimeoutException,
                    // so that appropriate action according to behavior can be done
                    throw new ThrowTimeoutException();
                default:
                    // always throw InvalidLockStateAfterRejoinException for exception-on-timeout behavior
                    throw new InvalidLockStateAfterRejoinException();
            }
        } else {
            return cacheOperationCallable.call();
        }
    }

    private boolean isExplicitLockApi() {
        return cacheOperationCallable instanceof ClusterOperationCallable
                && ((ClusterOperationCallable) cacheOperationCallable).getClusterOperation() instanceof ExplicitLockingClusterOperation;
    }

}
