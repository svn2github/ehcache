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

package net.sf.ehcache.constructs.nonstop;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;

public class NonStopCacheExecutorService {

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAXIMUM_POOL_SIZE = 1000;
    private static final long KEEP_ALIVE_TIME_SECS = 10;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final NonStopCacheConfig timeoutCacheConfig;

    public NonStopCacheExecutorService(final Ehcache cache, NonStopCacheConfig timeoutCacheConfig) {
        /**
         * Use direct handoff queue -- SynchronousQueue, that hands off tasks to threads without otherwise
         * holding them. Here, an attempt to queue a task will fail if no threads are immediately available to run it, so a new thread will
         * be constructed. This policy avoids lockups when handling sets of requests that might have internal dependencies. Direct handoffs
         * generally require unbounded maximumPoolSizes to avoid rejection of new submitted tasks. This in turn admits the possibility of
         * unbounded thread growth when commands continue to arrive on average faster than they can be processed.
         */
        threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {

                    public Thread newThread(Runnable runnable) {
                        return new Thread(runnable, NonStopCacheExecutorService.class.getName() + " " + cache.getName() + " Thread");
                    }
                });
        this.timeoutCacheConfig = timeoutCacheConfig;
    }

    public <V> V execute(Callable<V> callable) throws TimeoutException, CacheException, InterruptedException {
        return execute(callable, timeoutCacheConfig.getTimeoutValueInMillis());
    }

    public <V> V execute(Callable<V> callable, long timeoutValueInMillis) throws TimeoutException, CacheException, InterruptedException {
        V result = null;
        long startTime = System.currentTimeMillis();
        while (true) {
            try {
                result = threadPoolExecutor.submit(callable).get(timeoutValueInMillis, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException e) {
                // XXX: do something here?
                throw e;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RejectedExecutionException) {
                    // if the executor rejects (too many tasks executing), try until timed out
                    long now = System.currentTimeMillis();
                    if (now - startTime > timeoutValueInMillis) {
                        throw new TimeoutException();
                    } else {
                        continue;
                    }
                } else {
                    throw new CacheException(e.getCause());
                }
            } catch (TimeoutException e) {
                // rethrow timeout exception
                throw e;
            }
        }
        return result;
    }

}
