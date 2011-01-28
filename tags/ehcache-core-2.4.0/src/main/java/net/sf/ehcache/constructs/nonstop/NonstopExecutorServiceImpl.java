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

package net.sf.ehcache.constructs.nonstop;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.CacheException;

/**
 * Class used by NonStopCache for executing tasks within a timeout limit.
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopExecutorServiceImpl implements NonstopExecutorService {

    private final NonstopThreadPool nonstopThreadPool;

    /**
     * Constructor accepting a {@link ThreadFactory} that will be used to create threads for the pool
     *
     * @param threadFactory
     */
    public NonstopExecutorServiceImpl(final ThreadFactory threadFactory) {
        this.nonstopThreadPool = new NonstopThreadPool(threadFactory);
    }

    /**
     * {@inheritDoc}
     */
    public <V> V execute(final Callable<V> callable, final long timeoutValueInMillis) throws TimeoutException, CacheException,
            InterruptedException {
        int attempt = 0;
        V result = null;
        long startTime = System.nanoTime();
        while (true) {
            try {
                attempt++;
                result = nonstopThreadPool.submit(callable).get(timeoutValueInMillis, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException e) {
                // XXX: do something here?
                throw e;
            } catch (RejectedExecutionException e) {
                // if the executor rejects (too many tasks executing), try until timed out
                long now = System.nanoTime();
                if (now - startTime > TimeUnit.NANOSECONDS.convert(timeoutValueInMillis, TimeUnit.MILLISECONDS)) {
                    throw new TaskNotSubmittedTimeoutException(attempt);
                } else {
                    continue;
                }
            } catch (ExecutionException e) {
                Throwable rootCause = getRootCause(e);
                if (rootCause.getClass().getSimpleName().equals("TCNotRunningException")) {
                    throw new TimeoutException(rootCause.getMessage());
                }

                if (e.getCause() instanceof CacheException) {
                    throw (CacheException) e.getCause();
                }

                throw new CacheException(e.getCause());
            } catch (TimeoutException e) {
                // rethrow timeout exception
                throw e;
            }
        }
        return result;
    }

    private Throwable getRootCause(final Throwable exception) {
        Throwable e = exception;
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        nonstopThreadPool.shutdownNow();
    }
}
