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

package net.sf.ehcache.constructs.nonstop.store;

import java.util.concurrent.Callable;

import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Callable} implementation that accepts another callable delegate for executing it in nonstop+rejoin context.
 * Executing the {@link #call()} operation will execute the delegate callable and block until it returns. On rejoin, the delegate callable
 * is executed again.
 *
 * @author Abhishek Sanoujam
 *
 * @param <V>
 */
public class RejoinAwareBlockingOperation<V> implements Callable<V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejoinAwareBlockingOperation.class);

    private final Callable<V> delegateCallable;
    private final ExecutorServiceStore executorServiceStore;
    private volatile Thread executingThread;
    private volatile boolean rejoinHappened;

    /**
     * Public constructor
     *
     * @param executorServiceStore
     * @param callable
     */
    public RejoinAwareBlockingOperation(ExecutorServiceStore executorServiceStore, Callable<V> callable) {
        this.executorServiceStore = executorServiceStore;
        this.delegateCallable = callable;
    }

    /**
     * {@inheritDoc}.
     * <p />
     * Throws {@link InterruptedException} if the executing thread is interrupted before the call returns
     */
    public V call() throws Exception {
        executingThread = Thread.currentThread();
        return executeUntilComplete();
    }

    private V executeUntilComplete() throws Exception {
        while (true) {
            try {
                rejoinHappened = false;
                executorServiceStore.executeClusterOperationNoTimeout(new ClusterOperation<V>() {

                    public V performClusterOperation() throws Exception {
                        return delegateCallable.call();
                    }

                    public V performClusterOperationTimedOut(TimeoutBehaviorType configuredTimeoutBehavior) {
                        throw new AssertionError("This should never happen as executed with no-timeout");
                    }

                });
                return delegateCallable.call();
            } catch (InterruptedException e) {
                if (rejoinHappened) {
                    LOGGER.debug("Caught InterruptedException caused by rejoin. Executing callable again.");
                    continue;
                } else {
                    throw e;
                }

            }
        }
    }

    /**
     * Called when cluster rejoin happens
     */
    public void clusterRejoined() {
        rejoinHappened = true;
        // interrupt the executing thread so that it can retry again
        if (executingThread != null) {
            LOGGER.debug("Interrupting executing thread (id=" + executingThread.getId() + ", name='" + executingThread.getName()
                    + "') as rejoin happened");
            executingThread.interrupt();
        }
    }

}
