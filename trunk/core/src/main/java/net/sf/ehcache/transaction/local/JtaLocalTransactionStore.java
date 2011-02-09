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
package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.transaction.AbstractTransactionStore;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.List;

/**
 * A Store implementation with support for local transactions driven by a JTA transaction manager
 *
 * @author Ludovic Orban
 */
public class JtaLocalTransactionStore extends AbstractTransactionStore {

    private static final Logger LOG = LoggerFactory.getLogger(JtaLocalTransactionStore.class.getName());

    private static final ThreadLocal<Transaction> BOUND_JTA_TRANSACTIONS = new ThreadLocal<Transaction>();

    private final TransactionController transactionController;
    private final TransactionManager transactionManager;
    private final Ehcache cache;

    /**
     * Create a new JtaLocalTransactionStore instance
     * @param underlyingStore the underlying LocalTransactionStore
     * @param transactionManagerLookup the TransactionManagerLookup
     * @param transactionController the TransactionController
     */
    public JtaLocalTransactionStore(LocalTransactionStore underlyingStore, TransactionManagerLookup transactionManagerLookup,
                                    TransactionController transactionController) {
        super(underlyingStore);
        this.transactionController = transactionController;
        this.transactionManager = transactionManagerLookup.getTransactionManager();
        if (this.transactionManager == null) {
            throw new TransactionException("no JTA transaction manager could be located");
        }
        this.cache = underlyingStore.getCache();
    }

    private void registerInJtaContext() {
        try {
            if (transactionController.getCurrentTransactionContext() != null) {
                // already started local TX and registered in JTA

                // make sure the JTA transaction hasn't changed (happens when TM.suspend() is called)
                Transaction tx = transactionManager.getTransaction();
                if (!BOUND_JTA_TRANSACTIONS.get().equals(tx)) {
                    throw new TransactionException("Invalid JTA transaction context, cache was first used in transaction ["
                            + BOUND_JTA_TRANSACTIONS + "]" +
                            " but is now used in transaction [" + tx + "].");
                }
            } else {
                Transaction tx = transactionManager.getTransaction();
                if (tx == null) {
                    throw new TransactionException("no JTA transaction context started, xa caches cannot be used outside of" +
                            " JTA transactions");
                }
                BOUND_JTA_TRANSACTIONS.set(tx);

                transactionController.begin();
                tx.registerSynchronization(new JtaLocalEhcacheSynchronization(transactionController,
                        transactionController.getCurrentTransactionContext().getTransactionId()));
            }
        } catch (SystemException e) {
            throw new TransactionException("internal JTA transaction manager error, cannot bind xa cache with it", e);
        } catch (RollbackException e) {
            throw new TransactionException("JTA transaction rolled back, cannot bind xa cache with it", e);
        }
    }

    /**
     * A Synchronization used to terminate the local transaction and clean it up
     */
    private static class JtaLocalEhcacheSynchronization implements Synchronization {
        private final TransactionController transactionController;
        private final TransactionID transactionId;

        public JtaLocalEhcacheSynchronization(TransactionController transactionController, TransactionID transactionId) {
            this.transactionController = transactionController;
            this.transactionId = transactionId;
        }

        public void beforeCompletion() {
            //
        }

        public void afterCompletion(int status) {
            JtaLocalTransactionStore.BOUND_JTA_TRANSACTIONS.remove();
            if (status == javax.transaction.Status.STATUS_COMMITTED) {
                transactionController.commit(true);
            } else {
                transactionController.rollback();
            }
        }

        @Override
        public String toString() {
            return "JtaLocalEhcacheSynchronization of transaction [" + transactionId + "]";
        }
    }


    private void setRollbackOnly() {
        try {
            BOUND_JTA_TRANSACTIONS.get().setRollbackOnly();
            transactionController.setRollbackOnly();
        } catch (SystemException e) {
            LOG.warn("internal JTA transaction manager error", e);
        }
    }

    /* transactional methods */

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        registerInJtaContext();
        try {
            return underlyingStore.put(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        try {
            boolean put = underlyingStore.put(element);
            transactionManager.getTransaction().registerSynchronization(new Synchronization() {
                public void beforeCompletion() {
                    if (writerManager != null) {
                        writerManager.put(element);
                    } else {
                        cache.getWriterManager().put(element);
                    }
                }
                public void afterCompletion(int status) {
                    //
                }
            });
            return put;
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        } catch (RollbackException e) {
            throw new TransactionException("error registering writer synchronization", e);
        } catch (SystemException e) {
            throw new TransactionException("error registering writer synchronization", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        registerInJtaContext();
        try {
            return underlyingStore.get(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        registerInJtaContext();
        try {
            return underlyingStore.getQuiet(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        registerInJtaContext();
        try {
            return underlyingStore.getKeys();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        registerInJtaContext();
        try {
            return underlyingStore.remove(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        try {
            Element removed = underlyingStore.remove(key);
            final CacheEntry cacheEntry = new CacheEntry(key, getQuiet(key));
            transactionManager.getTransaction().registerSynchronization(new Synchronization() {
                public void beforeCompletion() {
                    if (writerManager != null) {
                        writerManager.remove(cacheEntry);
                    } else {
                        cache.getWriterManager().remove(cacheEntry);
                    }
                }
                public void afterCompletion(int status) {
                    //
                }
            });
            return removed;
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        } catch (RollbackException e) {
            throw new TransactionException("error registering writer synchronization", e);
        } catch (SystemException e) {
            throw new TransactionException("error registering writer synchronization", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        registerInJtaContext();
        try {
            underlyingStore.removeAll();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        registerInJtaContext();
        try {
            return underlyingStore.putIfAbsent(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        registerInJtaContext();
        try {
            return underlyingStore.removeElement(element, comparator);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator)
            throws NullPointerException, IllegalArgumentException {
        registerInJtaContext();
        try {
            return underlyingStore.replace(old, element, comparator);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        registerInJtaContext();
        try {
            return underlyingStore.replace(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        registerInJtaContext();
        try {
            return underlyingStore.getSize();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        registerInJtaContext();
        try {
            return underlyingStore.containsKey(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

}

