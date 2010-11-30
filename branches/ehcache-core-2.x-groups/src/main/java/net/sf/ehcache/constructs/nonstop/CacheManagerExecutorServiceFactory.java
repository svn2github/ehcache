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
import net.sf.ehcache.Ehcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NonStopCacheExecutorServiceFactory} that creates and maintains one per CacheManager
 *
 * @author Abhishek Sanoujam
 *
 */
public final class CacheManagerExecutorServiceFactory implements NonStopCacheExecutorServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManagerExecutorServiceFactory.class);

    private static final String MAX_THREAD_POOL_SIZE_PROPERTY_PREFIX = "net.sf.ehcache.constructs.nonstop.maxThreadPoolSize.";
    private static final String CORE_THREAD_POOL_SIZE_PROPERTY_PREFIX = "net.sf.ehcache.constructs.nonstop.coreThreadPoolSize.";

    private static final CacheManagerExecutorServiceFactory SINGLETON = new CacheManagerExecutorServiceFactory();

    private final Map<String, NonStopCacheExecutorService> executorServiceMap = new HashMap<String, NonStopCacheExecutorService>();

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
    public NonStopCacheExecutorService getOrCreateNonStopCacheExecutorService(Ehcache cache) {
        final String cacheManagerName = cache.getCacheManager().getName();
        synchronized (executorServiceMap) {
            NonStopCacheExecutorService rv = executorServiceMap.get(cacheManagerName);
            if (rv == null) {
                int corePoolSize = getCoreThreadPoolSize(cache.getCacheManager());
                int maximumPoolSize = getMaxThreadPoolSize(cache.getCacheManager());
                if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
                    throw new IllegalArgumentException("Invalid coreThreadPoolSize=" + corePoolSize + ", maxThreadPoolSize="
                            + maximumPoolSize);
                }
                rv = new NonStopCacheExecutorService(corePoolSize, maximumPoolSize, new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger();

                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "NonStopCache [" + cacheManagerName + "] "
                                + NonStopCacheExecutorService.EXECUTOR_THREAD_NAME_PREFIX + "-" + count.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                });
                executorServiceMap.put(cacheManagerName, rv);
            }
            return rv;
        }
    }

    private int getCoreThreadPoolSize(CacheManager cacheManager) {
        return getProperty(CORE_THREAD_POOL_SIZE_PROPERTY_PREFIX + cacheManager.getName(),
                NonStopCacheExecutorService.DEFAULT_CORE_THREAD_POOL_SIZE);
    }

    private int getMaxThreadPoolSize(CacheManager cacheManager) {
        return getProperty(MAX_THREAD_POOL_SIZE_PROPERTY_PREFIX + cacheManager.getName(),
                NonStopCacheExecutorService.DEFAULT_MAX_THREAD_POOL_SIZE);
    }

    private static int getProperty(String propertyName, int defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue == null || "".equals(propertyValue.trim())) {
            return defaultValue;
        }
        int value = 0;
        try {
            value = Integer.parseInt(propertyValue);
        } catch (NumberFormatException e) {
            value = defaultValue;
            LOGGER.warn("Invalid value specified for property \"" + propertyName + "\"=" + propertyValue + ", using default value: "
                    + defaultValue);
        }
        return value;
    }

}
