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

package net.sf.ehcache.terracotta;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.util.ClassLoaderUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class encapsulating the idea of a Terracotta client. Provides access to the {@link ClusteredInstanceFactory} for the cluster
 *
 * @author Abhishek Sanoujam
 *
 */
public class TerracottaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerracottaClient.class);
    private static final int REJOIN_SLEEP_MILLIS_ON_EXCEPTION = Integer.getInteger("net.sf.ehcache.rejoin.sleepMillisOnException", 5000);

    private static final String CUSTOM_SECRET_PROVIDER_SYSTEM_PROPERTY = "com.terracotta.express.SecretProvider";
    private static final String CUSTOM_SECRET_PROVIDER_WRAPPER_CLASSNAME = "net.sf.ehcache.terracotta.security.SingletonSecretProviderWrapper";

    private final TerracottaClientConfiguration terracottaClientConfiguration;
    private volatile ClusteredInstanceFactoryWrapper clusteredInstanceFactory;
    private final TerracottaCacheCluster cacheCluster = new TerracottaCacheCluster();
    private final CacheManager cacheManager;
    private ExecutorService l1TerminatorThreadPool;

    /**
     * Constructor accepting the {@link TerracottaClientRejoinListener} and the {@link TerracottaClientConfiguration}
     *
     * @param cacheManager
     * @param rejoinAction
     * @param terracottaClientConfiguration
     */
    public TerracottaClient(CacheManager cacheManager, TerracottaClientConfiguration terracottaClientConfiguration) {
        this.cacheManager = cacheManager;
        this.terracottaClientConfiguration = terracottaClientConfiguration;
        if (terracottaClientConfiguration != null) {
            terracottaClientConfiguration.freezeConfig();

            // If we're going clustered and secured and a secret provider is configured, it's time to wrap the
            // secret provider before the L1 can use it.
            // We must set this ehcache secret provider as both the L1 and the agent will use a different instance
            // and will want to fetch the password. In case of a console fetcher, the password would be asked twice.
            String secretProviderClassname = System.getProperty(CUSTOM_SECRET_PROVIDER_SYSTEM_PROPERTY);
            String tcUrl = terracottaClientConfiguration.getUrl();
            if (tcUrl != null && tcUrl.contains("@") && secretProviderClassname != null) {
                try {
                    System.setProperty(CUSTOM_SECRET_PROVIDER_SYSTEM_PROPERTY, CUSTOM_SECRET_PROVIDER_WRAPPER_CLASSNAME);
                    Class<?> secretProviderWrapperClass = ClassLoaderUtil.loadClass(CUSTOM_SECRET_PROVIDER_WRAPPER_CLASSNAME);
                    secretProviderWrapperClass.getMethod("useAsDelegate", String.class).invoke(secretProviderWrapperClass, secretProviderClassname);
                } catch (Exception e) {
                    throw new CacheException("Unable to initialize " + CUSTOM_SECRET_PROVIDER_WRAPPER_CLASSNAME, e);
                }
            }
        }
    }

    /*
     * --------- THIS METHOD IS NOT FOR PUBLIC USE ----------
     * private method, used in unit-tests using reflection
     *
     * @param testHelper the mock TerracottaClusteredInstanceHelper for testing
     */
    private static void setTestMode(TerracottaClusteredInstanceHelper testHelper) {
        try {
            Method method = TerracottaClusteredInstanceHelper.class.getDeclaredMethod("setTestMode",
                    TerracottaClusteredInstanceHelper.class);
            method.setAccessible(true);
            method.invoke(null, testHelper);
        } catch (Exception e) {
            // just print a stack trace and ignore
            e.printStackTrace();
        }
    }

    /**
     * Returns the {@link ClusteredInstanceFactory} associated with this client
     *
     * @return The ClusteredInstanceFactory
     */
    public ClusteredInstanceFactory getClusteredInstanceFactory() {
        return clusteredInstanceFactory;
    }

    /**
     * Returns true if the clusteredInstanceFactory was created, otherwise returns false.
     * Multiple threads calling this method block and only one of them creates the factory.
     *
     * @param cacheConfigs
     * @return true if the clusteredInstanceFactory was created, otherwise returns false
     */
    public boolean createClusteredInstanceFactory(Map<String, CacheConfiguration> cacheConfigs) {
        if (clusteredInstanceFactory != null) {
            return false;
        }
        final boolean created;
        synchronized (this) {
            if (clusteredInstanceFactory == null) {
                clusteredInstanceFactory = createNewClusteredInstanceFactory(cacheConfigs);
                created = true;
            } else {
                created = false;
            }
        }
        return created;
    }

    /**
     * Get the {@link CacheCluster} associated with this client
     *
     * @return the {@link CacheCluster} associated with this client
     */
    public TerracottaCacheCluster getCacheCluster() {
        if (clusteredInstanceFactory == null) {
            throw new CacheException("Cannot get CacheCluster as ClusteredInstanceFactory has not been initialized yet.");
        }
        return cacheCluster;
    }

    /**
     * Shuts down the client
     */
    public synchronized void shutdown() {
        if (clusteredInstanceFactory != null) {
            shutdownClusteredInstanceFactoryWrapper(clusteredInstanceFactory);
        }
    }

    private void shutdownClusteredInstanceFactoryWrapper(ClusteredInstanceFactoryWrapper clusteredInstanceFactory) {
        clusteredInstanceFactory.getActualFactory().getTopology().removeAllListeners();
        clusteredInstanceFactory.shutdown();
    }

    private synchronized ClusteredInstanceFactoryWrapper createNewClusteredInstanceFactory(Map<String, CacheConfiguration> cacheConfigs) {
        // shut down the old factory
        if (clusteredInstanceFactory != null) {
            info("Shutting down old ClusteredInstanceFactory...");
            shutdownClusteredInstanceFactoryWrapper(clusteredInstanceFactory);
        }
        info("Creating new ClusteredInstanceFactory");
        ClusteredInstanceFactory factory;
        factory = TerracottaClusteredInstanceHelper.getInstance().newClusteredInstanceFactory(cacheConfigs, terracottaClientConfiguration);
        CacheCluster underlyingCacheCluster = factory.getTopology();

        cacheCluster.setUnderlyingCacheCluster(underlyingCacheCluster);

        return new ClusteredInstanceFactoryWrapper(this, factory);
    }

    private synchronized ExecutorService getL1TerminatorThreadPool() {
        if (l1TerminatorThreadPool == null) {
            l1TerminatorThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
                private final ThreadGroup threadGroup = new ThreadGroup("Rejoin Terminator Thread Group");

                public Thread newThread(Runnable runnable) {
                    Thread t = new Thread(threadGroup, runnable, "L1 Terminator");
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return l1TerminatorThreadPool;
    }

    private void info(String msg) {
        info(msg, null);
    }

    private void info(String msg, Throwable t) {
        if (t == null) {
            LOGGER.info(getLogPrefix() + msg);
        } else {
            LOGGER.info(getLogPrefix() + msg, t);
        }
    }

    private String getLogPrefix() {
        return "Thread [" + Thread.currentThread().getName() + "] [cacheManager: " + getCacheManagerName() + "]: ";
    }

    private void debug(String msg) {
        LOGGER.debug(getLogPrefix() + msg);
    }

    private void warn(String msg) {
        LOGGER.warn(getLogPrefix() + msg);
    }

    private String getCacheManagerName() {
        if (cacheManager.isNamed()) {
            return "'" + cacheManager.getName() + "'";
        } else {
            return "no name";
        }
    }

}
