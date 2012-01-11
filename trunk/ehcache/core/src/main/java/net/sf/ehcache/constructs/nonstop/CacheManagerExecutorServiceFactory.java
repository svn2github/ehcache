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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheManager;

/**
 * {@link NonstopExecutorServiceFactory} that creates and maintains one per CacheManager
 *
 * @author Abhishek Sanoujam
 *
 */
public final class CacheManagerExecutorServiceFactory implements NonstopExecutorServiceFactory {

    /**
     * A string that is a part of the thread name created by the default thread factory.
     */
    private static final String EXECUTOR_THREAD_NAME_PREFIX = "Executor Thread";

    private static final CacheManagerExecutorServiceFactory SINGLETON = new CacheManagerExecutorServiceFactory();

    private final Map<String, NonstopExecutorService> executorServiceMap = new HashMap<String, NonstopExecutorService>();

    /**
     * private constructor
     */
    private CacheManagerExecutorServiceFactory() {
        //
    }

    /**
     * Returns the singleton instance
     *
     * @return the singleton instance
     */
    public static CacheManagerExecutorServiceFactory getInstance() {
        return SINGLETON;
    }

    /**
     * {@inheritDoc}
     */
    public NonstopExecutorService getOrCreateNonstopExecutorService(final CacheManager cacheManager) {
        final String cacheManagerName = cacheManager.getName();
        synchronized (executorServiceMap) {
            NonstopExecutorService rv = executorServiceMap.get(cacheManagerName);
            if (rv == null) {
                rv = new NonstopExecutorServiceImpl(new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger();

                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "NonStopCache [" + cacheManagerName + "] " + EXECUTOR_THREAD_NAME_PREFIX + "-"
                                + count.incrementAndGet() + " for '" + Thread.currentThread().getName() + "'");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
                executorServiceMap.put(cacheManagerName, rv);
            }
            return rv;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown(final CacheManager cacheManager) {
        synchronized (executorServiceMap) {
            NonstopExecutorService executorService = executorServiceMap.remove(cacheManager.getName());
            if (executorService != null) {
                executorService.shutdown();
            }
        }

    }

}
