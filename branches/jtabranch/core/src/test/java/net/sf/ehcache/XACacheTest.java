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

package net.sf.ehcache;

import java.util.concurrent.CyclicBarrier;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class XACacheTest extends TestCase {

    CacheManager manager;

    public void testXACache() throws IllegalStateException, SecurityException, SystemException {
        Cache cache = createTestCache();
        TransactionManager txnManager = cache.getTransactionManagerLookup().getTransactionManager();
        Element element1 = new Element("key1", "value1");
        Element element2 = new Element("key1", "value1");
        CyclicBarrier barrier1 = new CyclicBarrier(2);
        CyclicBarrier barrier2 = new CyclicBarrier(2);
        CyclicBarrier txnBarrier = new CyclicBarrier(2);

        Transaction1Thread thread1 = new Transaction1Thread(cache, element1, element2, txnManager, barrier1, barrier2, txnBarrier);
        Transaction2Thread thread2 = new Transaction2Thread(cache, element1, element2, txnManager, barrier1, barrier2, txnBarrier);
        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static class Transaction1Thread extends AbstractTxnThread {

        public Transaction1Thread(Cache cache, Element element1, Element element2, TransactionManager txnManager, CyclicBarrier barrier1,
                CyclicBarrier barrier2, CyclicBarrier txnBarrier) {
            super(cache, element1, element2, txnManager, barrier1, barrier2, txnBarrier);
        }

        @Override
        public void run() {
            try {
                txnManager.begin();
                cache.put(element1);
                barrier1.await();
                txnManager.commit();
                barrier2.await();
            } catch (Exception e) {

                rollbackQuietly();
            }

            resetForTxn();

        }

    }

    private static class Transaction2Thread extends AbstractTxnThread {

        public Transaction2Thread(Cache cache, Element element1, Element element2, TransactionManager txnManager, CyclicBarrier barrier1,
                CyclicBarrier barrier2, CyclicBarrier txnBarrier) {
            super(cache, element1, element2, txnManager, barrier1, barrier2, txnBarrier);
        }

        @Override
        public void run() {

            try {
                txnManager.begin();
                Element newElement = cache.get(element1.getKey());
                assertNull(newElement);
                barrier1.await();
                barrier2.await();
                newElement = cache.get(element1.getKey());
                assertNotNull(newElement);

                txnManager.commit();

            } catch (Exception e) {
                rollbackQuietly();
            }

            resetForTxn();

        }
    }

    private static abstract class AbstractTxnThread extends Thread {
        final Element element1;
        final Element element2;
        final TransactionManager txnManager;
        final CyclicBarrier barrier1;
        final CyclicBarrier barrier2;
        final CyclicBarrier txnBarrier;
        final Cache         cache;

        public AbstractTxnThread(Cache cache, Element element1, Element element2, TransactionManager txnManager, CyclicBarrier barrier1,
                CyclicBarrier barrier2, CyclicBarrier txnBarrier) {
            this.element1 = element1;
            this.element2 = element2;
            this.txnManager = txnManager;
            this.barrier1 = barrier1;
            this.barrier2 = barrier2;
            this.txnBarrier = txnBarrier;
            this.cache = cache;
        }

        void rollbackQuietly() {
            try {
                txnManager.rollback();
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        void resetForTxn() {
            synchronized (barrier1) {
                if (barrier1.isBroken()) {
                    barrier1.reset();
                }
            }
            synchronized (barrier2) {
                if (barrier2.isBroken()) {
                    barrier2.reset();
                }
            }

            synchronized (txnBarrier) {
                if (txnBarrier.isBroken()) {
                    txnBarrier.reset();
                }
            }
            try {
                txnBarrier.await();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
    }

    @Override
    protected void setUp() throws Exception {
        manager = CacheManager.create();
    }

    /**
     * Creates a cache
     * 
     * @return
     */
    protected Cache createTestCache() {
        Cache cache = new Cache("sampleCache", 1000, MemoryStoreEvictionPolicy.LRU, false, null, false, 0, 0, false, 0, null, null, 0, 0,
                false, false, "SERIALIZATION", true, "xa", "net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup",
                TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION, TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION_PERIOD,
                TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE, TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE_SIZE,
                TerracottaConfiguration.DEFAULT_COPY_ON_READ);
        manager.addCache(cache);
        return cache;
    }

}