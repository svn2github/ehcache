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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.AbstractStore;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
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
import java.io.IOException;
import java.util.List;

/**
 * A Store implementation with support for local transactions driven by a JTA transaction manager
 *
 * @author Ludovic Orban
 */
public class JtaLocalTransactionStore extends AbstractStore {

    private static final Logger LOG = LoggerFactory.getLogger(JtaLocalTransactionStore.class.getName());

    private static final ThreadLocal<Transaction> BOUND_JTA_TRANSACTIONS = new ThreadLocal<Transaction>();

    private final LocalTransactionStore transactionalStore;
    private final TransactionController transactionController;
    private final TransactionManager transactionManager;

    /**
     * Create a new JtaLocalTransactionStore instance
     * @param transactionalStore the underlying LocalTransactionStore
     * @param transactionManagerLookup the TransactionManagerLookup
     * @param transactionController the TransactionController
     */
    public JtaLocalTransactionStore(LocalTransactionStore transactionalStore, TransactionManagerLookup transactionManagerLookup,
                                    TransactionController transactionController) {
        this.transactionalStore = transactionalStore;
        this.transactionController = transactionController;
        this.transactionManager = transactionManagerLookup.getTransactionManager();
        if (this.transactionManager == null) {
            throw new TransactionException("no JTA transaction manager could be located, cannot bind local_jta cache with JTA");
        }
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
                    throw new TransactionException("no JTA transaction context started, local_jta caches cannot be used outside of" +
                            " JTA transactions");
                }
                BOUND_JTA_TRANSACTIONS.set(tx);

                transactionController.begin();
                tx.registerSynchronization(new JtaLocalEhcacheSynchronization(transactionController,
                        transactionController.getCurrentTransactionContext().getTransactionId()));
            }
        } catch (SystemException e) {
            throw new TransactionException("internal JTA transaction manager error, cannot bind local_jta cache with it", e);
        } catch (RollbackException e) {
            throw new TransactionException("JTA transaction rolled back, cannot bind local_jta cache with it", e);
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
            return transactionalStore.put(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        try {
            return transactionalStore.putWithWriter(element, writerManager);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        registerInJtaContext();
        try {
            return transactionalStore.get(key);
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
            return transactionalStore.getQuiet(key);
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
            return transactionalStore.getKeys();
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
            return transactionalStore.remove(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        try {
            return transactionalStore.removeWithWriter(key, writerManager);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        registerInJtaContext();
        try {
            transactionalStore.removeAll();
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
            return transactionalStore.putIfAbsent(element);
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
            return transactionalStore.removeElement(element, comparator);
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
            return transactionalStore.replace(old, element, comparator);
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
            return transactionalStore.replace(element);
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
            return transactionalStore.getSize();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        registerInJtaContext();
        try {
            return transactionalStore.getTerracottaClusteredSize();
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
            return transactionalStore.containsKey(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /* non-transactional methods */

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return transactionalStore.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return transactionalStore.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return transactionalStore.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return transactionalStore.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return transactionalStore.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return transactionalStore.getOnDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return transactionalStore.containsKeyOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return transactionalStore.containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return transactionalStore.containsKeyInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        transactionalStore.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return transactionalStore.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        transactionalStore.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        transactionalStore.flush();
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return transactionalStore.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return transactionalStore.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        transactionalStore.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return transactionalStore.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return transactionalStore.getMBean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeCoherent(boolean coherent) {
        transactionalStore.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilClusterCoherent() {
        transactionalStore.waitUntilClusterCoherent();
    }
}

