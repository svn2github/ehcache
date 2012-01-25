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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.concurrency.ExplicitLockingContextThreadLocal;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * This implementation is identical to TransactionalExecutorServiceStore except that it ensures the transactional context
 * gets propagated to the executor thread.
 * <p/>
 *
 * @see ExecutorServiceStore
 * @author Ludovic Orban
 *
 */
public class TransactionalExecutorServiceStore extends ExecutorServiceStore {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionalExecutorServiceStore.class.getName());

    private final TransactionManager transactionManager;

    /**
     * Constructor
     * @param explicitLockingContextThreadLocal
     */
    public TransactionalExecutorServiceStore(final NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
            final NonstopConfiguration nonstopConfiguration, final NonstopTimeoutBehaviorStoreResolver timeoutBehaviorResolver,
            CacheCluster cacheCluster, TransactionManagerLookup transactionManagerLookup,
            ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal) {
        super(nonstopActiveDelegateHolder, nonstopConfiguration, timeoutBehaviorResolver, cacheCluster, explicitLockingContextThreadLocal);
        this.transactionManager = transactionManagerLookup.getTransactionManager();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean put(final Element element) throws CacheException {
        boolean rv = false;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().put(element);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().put(element);
        } finally {
            resumeCaller(tx);
        }

        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        boolean rv = false;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().putWithWriter(element, writerManager);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().putWithWriter(element, writerManager);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Element get(final Object key) {
        Element rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().get(key);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().get(key);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Element getQuiet(final Object key) {
        Element rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().getQuiet(key);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().getQuiet(key);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public List getKeys() {
        List rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<List>() {
                public List call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().getKeys();
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().getKeys();
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Element remove(final Object key) {
        Element rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().remove(key);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().remove(key);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        Element rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().removeWithWriter(key, writerManager);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().removeWithWriter(key, writerManager);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     * The timeout used by this method is {@link net.sf.ehcache.config.NonstopConfiguration#getBulkOpsTimeoutMultiplyFactor()} times the timeout value in the
     * config.
     */
    @Override
    public void removeAll() throws CacheException {
        final Transaction tx = suspendCaller();
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    resumeCallee(tx);
                    try {
                        underlyingTerracottaStore().removeAll();
                        return null;
                    } finally {
                        suspendCallee();
                    }
                }
            }, nonstopConfiguration.getTimeoutMillis() * nonstopConfiguration.getBulkOpsTimeoutMultiplyFactor());
        } catch (TimeoutException e) {
            resolveTimeoutBehaviorStore().removeAll();
        } finally {
            resumeCaller(tx);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Element putIfAbsent(final Element element) throws NullPointerException {
        Element rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().putIfAbsent(element);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().putIfAbsent(element);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Element removeElement(final Element element, final ElementValueComparator comparator) throws NullPointerException {
        Element rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().removeElement(element, comparator);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().removeElement(element, comparator);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean replace(final Element old, final Element element, final ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        boolean rv = false;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().replace(old, element, comparator);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().replace(old, element, comparator);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Element replace(final Element element) throws NullPointerException {
        Element rv = null;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().replace(element);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().replace(element);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getSize() {
        int rv = 0;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().getSize();
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().getSize();
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }


    /**
     * {@inheritDoc}.
     */
    @Override
    public int getTerracottaClusteredSize() {
        int rv = 0;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().getTerracottaClusteredSize();
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().getTerracottaClusteredSize();
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean containsKey(final Object key) {
        boolean rv = false;
        final Transaction tx = suspendCaller();
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    resumeCallee(tx);
                    try {
                        return underlyingTerracottaStore().containsKey(key);
                    } finally {
                        suspendCallee();
                    }
                }
            });
        } catch (TimeoutException e) {
            return resolveTimeoutBehaviorStore().containsKey(key);
        } finally {
            resumeCaller(tx);
        }
        return rv;
    }



    private void resumeCaller(Transaction tx) {
        if (tx == null) {
            return;
        }

        try {
            transactionManager.resume(tx);
        } catch (IllegalStateException ise) {
            LOG.warn("error resuming JTA transaction context on caller thread", ise);
        } catch (InvalidTransactionException ite) {
            LOG.warn("error resuming JTA transaction context on caller thread", ite);
        } catch (SystemException se) {
            LOG.warn("error resuming JTA transaction context on caller thread", se);
        }
    }

    private Transaction suspendCaller() {
        try {
            return transactionManager.suspend();
        } catch (SystemException se) {
            throw new CacheException("error suspending JTA transaction context", se);
        }
    }

    private void resumeCallee(Transaction tx) {
        if (tx == null) {
            return;
        }

        try {
            transactionManager.resume(tx);
        } catch (IllegalStateException ise) {
            throw new CacheException("error resuming JTA transaction context on caller thread", ise);
        } catch (InvalidTransactionException ite) {
            throw new CacheException("error resuming JTA transaction context on caller thread", ite);
        } catch (SystemException se) {
            throw new CacheException("error resuming JTA transaction context on caller thread", se);
        }
    }

    private Transaction suspendCallee() {
        try {
            return transactionManager.suspend();
        } catch (SystemException se) {
            LOG.warn("error suspending JTA transaction context", se);
        }
        return null;
    }

}
