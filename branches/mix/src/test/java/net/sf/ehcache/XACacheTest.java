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

package net.sf.ehcache;

import bitronix.tm.TransactionManagerServices;
import net.sf.ehcache.config.CacheConfiguration;

import java.util.concurrent.CyclicBarrier;

import javax.transaction.*;

import junit.framework.TestCase;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;

public class XACacheTest extends TestCase {

    private Cache cache;
    private DefaultTransactionManagerLookup transactionManagerLookup;

    private DefaultTransactionManagerLookup getTransactionManagerLookup() {
        if (transactionManagerLookup == null) {
            this.transactionManagerLookup = new DefaultTransactionManagerLookup();
            TransactionManagerServices.getConfiguration().setJournal("null");
        }
        return this.transactionManagerLookup;
    }

    public void testXACache() throws Throwable {
        TransactionManager txnManager = getTransactionManagerLookup().getTransactionManager();
        Element element1 = new Element("key1", "value1");
        Element element2 = new Element("key2", "value2");
        CyclicBarrier barrier = new CyclicBarrier(2);

        Transaction1Thread thread1 = new Transaction1Thread(cache, element1, element2, txnManager, barrier);
        Transaction2Thread thread2 = new Transaction2Thread(cache, element1, element2, txnManager, barrier);
        thread1.start();
        thread2.start();
        try {
            thread1.join(10000);
            thread2.join(10000);
        } catch (InterruptedException e) {
            fail("Interrupted!");
        }

        thread1.check();
        thread2.check();
    }

    private static class Transaction1Thread extends AbstractTxnThread {

        public Transaction1Thread(Cache cache, Element element1, Element element2, TransactionManager txnManager, CyclicBarrier barrier) {
            super(cache, element1, element2, txnManager, barrier);
        }

        @Override
        public void run() {
            try {
                txnManager.begin();
                cache.put(element1);
                barrier.await();
                barrier.await();
                txnManager.commit();
                barrier.await();
            } catch (Throwable e) {
                exception = e;
                barrier.reset();
                rollbackQuietly();
            }
        }
    }

    private static class Transaction2Thread extends AbstractTxnThread {

        public Transaction2Thread(Cache cache, Element element1, Element element2, TransactionManager txnManager, CyclicBarrier barrier) {
            super(cache, element1, element2, txnManager, barrier);
        }

        @Override
        public void run() {
            try {
                txnManager.begin();
                barrier.await();
                Element newElement = cache.get(element1.getKey());
                assertNull(newElement);
                barrier.await();
                barrier.await();
                newElement = cache.get(element1.getKey());
                assertNotNull(newElement);
                txnManager.commit();
            } catch (Throwable e) {
                exception = e;
                barrier.reset();
                rollbackQuietly();
            }
        }
    }

    private static abstract class AbstractTxnThread extends Thread {

        protected volatile Throwable exception;

        final Element element1;
        final Element element2;
        final TransactionManager txnManager;
        final CyclicBarrier barrier;
        final Cache cache;

        public AbstractTxnThread(Cache cache, Element element1, Element element2, TransactionManager txnManager, CyclicBarrier barrier) {
            this.element1 = element1;
            this.element2 = element2;
            this.txnManager = txnManager;
            this.barrier = barrier;
            this.cache = cache;
        }

        void rollbackQuietly() {
            try {
                if (txnManager.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    txnManager.rollback();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        public void check() throws Throwable {
            if(exception != null) {
                throw exception;
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        final CacheManager manager = CacheManager.create();
        cache = new Cache(new CacheConfiguration("sampleCache", 1000).transactionalMode(CacheConfiguration.TransactionalMode.XA_STRICT));
        cache.setTransactionManagerLookup(getTransactionManagerLookup());
        manager.addCache(cache);
    }
}
