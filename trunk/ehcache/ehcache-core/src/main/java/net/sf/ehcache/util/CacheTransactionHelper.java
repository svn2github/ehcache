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

package net.sf.ehcache.util;

import java.lang.reflect.InvocationTargetException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.TransactionController;

/**
 * A collection of utility methods helping controlling transactions for managed operations which may require them.
 *
 * @author Ludovic Orban
 */
public class CacheTransactionHelper {

    private static final int XA_STATUS_NO_TRANSACTION = 6;

    /**
     * Begin a transaction on the current thread if the cache is configured as transactional,
     * otherwise this method does nothing.
     *
     * @param cache the cache to begin a transaction for
     * @throws CacheException if anything wrong happens
     */
    public static void beginTransactionIfNeeded(Ehcache cache) throws CacheException {
        try {
            switch (cache.getCacheConfiguration().getTransactionalMode()) {
                case LOCAL:
                    TransactionController ctrl = cache.getCacheManager().getTransactionController();
                    ctrl.begin();
                    break;

                case XA:
                case XA_STRICT:
                    Object tm = ((net.sf.ehcache.Cache) cache).getTransactionManagerLookup().getTransactionManager();
                    tm.getClass().getMethod("begin").invoke(tm);
                    break;

                case OFF:
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CacheException("error beginning transaction:" + e);
        }
    }

    /**
     * Commit a transaction previously begun on the current thread if the cache is configured as
     * transactional, otherwise this method does nothing.
     *
     * @param cache the cache to commit a transaction for
     * @throws CacheException if anything wrong happens
     */
    public static void commitTransactionIfNeeded(Ehcache cache) throws CacheException {
        try {
            switch (cache.getCacheConfiguration().getTransactionalMode()) {
                case LOCAL:
                    TransactionController ctrl = cache.getCacheManager().getTransactionController();
                    ctrl.commit();
                    break;

                case XA:
                case XA_STRICT:
                    Object tm = ((net.sf.ehcache.Cache) cache).getTransactionManagerLookup().getTransactionManager();
                    tm.getClass().getMethod("commit").invoke(tm);
                    break;

                case OFF:
                default:
                    break;
            }
        } catch (Exception e) {
            Throwable t = e;
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException)e).getCause();
            }
            throw new CacheException("error committing transaction: " + t);
        }
    }

    /**
     * Check if a transaction has begun on the current thread if the cache is configured as
     * transactional, otherwise always return false.
     * @param cache the cache to check if a transaction started for
     * @return true if the cache is transactional and a transaction started, false otherwise
     * @throws CacheException if anything wrong happens
     */
    public static boolean isTransactionStarted(Ehcache cache) throws CacheException {
        try {
            switch (cache.getCacheConfiguration().getTransactionalMode()) {
                case LOCAL:
                    TransactionController ctrl = cache.getCacheManager().getTransactionController();
                    return ctrl.getCurrentTransactionContext() != null;

                case XA:
                case XA_STRICT:
                    Object tm = ((net.sf.ehcache.Cache) cache).getTransactionManagerLookup().getTransactionManager();
                    return ((Integer) tm.getClass().getMethod("getStatus").invoke(tm)) != XA_STATUS_NO_TRANSACTION;

                case OFF:
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CacheException("error checking if transaction started: " + e);
        }
    }

}
