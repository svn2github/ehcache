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

package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.TransactionController;

/**
 * A collection of utility methods helping controlling transactions for managed operations which may require them.
 *
 * @author Ludovic Orban
 */
public class CacheTransactionHelper {

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
            e.printStackTrace();
            throw new CacheException("error committing transaction: " + e);
        }
    }

}
