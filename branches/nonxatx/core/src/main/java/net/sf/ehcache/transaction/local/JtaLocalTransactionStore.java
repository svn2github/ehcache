package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.AbstractStore;
import net.sf.ehcache.store.ElementComparer;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriterManager;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class JtaLocalTransactionStore extends AbstractStore {

    private final LocalTransactionStore transactionalStore;
    private final TransactionController transactionController;
    private final TransactionManager transactionManager;
    private Transaction boundTransaction;

    public JtaLocalTransactionStore(LocalTransactionStore transactionalStore, TransactionManagerLookup transactionManagerLookup, TransactionController transactionController) {
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
                if (!boundTransaction.equals(tx)) {
                    throw new TransactionException("Invalid JTA transaction context, cache was first used in transaction [" + boundTransaction + "]" +
                            " but is now used in transaction [" + tx + "].");
                }
                return;
            }

            Transaction tx = transactionManager.getTransaction();
            if (tx == null) {
                throw new TransactionException("no JTA transaction context started, local_jta caches cannot be used outside of JTA transactions");
            }
            boundTransaction = tx;

            transactionController.begin();
            tx.registerSynchronization(new JtaLocalEhcacheSynchronization(transactionController));
        } catch (SystemException e) {
            throw new TransactionException("internal JTA exception", e);
        } catch (RollbackException e) {
            throw new TransactionException("JTA transaction rolled back", e);
        }
    }

    private void setRollbackOnly() {
        try {
            transactionManager.getTransaction().setRollbackOnly();
            transactionController.setRollbackOnly();
        } catch (SystemException e) {
            // ignore
        }
    }

    /* transactional methods */

    public boolean put(Element element) throws CacheException {
        registerInJtaContext();
        try {
            return transactionalStore.put(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        try {
            return transactionalStore.putWithWriter(element, writerManager);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public Element get(Object key) {
        registerInJtaContext();
        try {
            return transactionalStore.get(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public Element getQuiet(Object key) {
        registerInJtaContext();
        try {
            return transactionalStore.getQuiet(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public List getKeys() {
        registerInJtaContext();
        try {
            return transactionalStore.getKeys();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public Element remove(Object key) {
        registerInJtaContext();
        try {
            return transactionalStore.remove(key);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        try {
            return transactionalStore.removeWithWriter(key, writerManager);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public void removeAll() throws CacheException {
        registerInJtaContext();
        try {
            transactionalStore.removeAll();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        registerInJtaContext();
        try {
            return transactionalStore.putIfAbsent(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public Element removeElement(Element element) throws NullPointerException {
        registerInJtaContext();
        try {
            return transactionalStore.removeElement(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        registerInJtaContext();
        try {
            return transactionalStore.replace(old, element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public boolean replace(Element old, Element element, ElementComparer comparer) throws NullPointerException, IllegalArgumentException {
        registerInJtaContext();
        try {
            return transactionalStore.replace(old, element, comparer);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public Element replace(Element element) throws NullPointerException {
        registerInJtaContext();
        try {
            return transactionalStore.replace(element);
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public int getSize() {
        registerInJtaContext();
        try {
            return transactionalStore.getSize();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

    public int getTerracottaClusteredSize() {
        registerInJtaContext();
        try {
            return transactionalStore.getTerracottaClusteredSize();
        } catch (CacheException e) {
            setRollbackOnly();
            throw e;
        }
    }

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

    public int getInMemorySize() {
        return transactionalStore.getInMemorySize();
    }

    public int getOffHeapSize() {
        return transactionalStore.getOffHeapSize();
    }

    public int getOnDiskSize() {
        return transactionalStore.getOnDiskSize();
    }

    public long getInMemorySizeInBytes() {
        return transactionalStore.getInMemorySizeInBytes();
    }

    public long getOffHeapSizeInBytes() {
        return transactionalStore.getOffHeapSizeInBytes();
    }

    public long getOnDiskSizeInBytes() {
        return transactionalStore.getOnDiskSizeInBytes();
    }

    public boolean containsKeyOnDisk(Object key) {
        return transactionalStore.containsKeyOnDisk(key);
    }

    public boolean containsKeyOffHeap(Object key) {
        return transactionalStore.containsKeyOffHeap(key);
    }

    public boolean containsKeyInMemory(Object key) {
        return transactionalStore.containsKeyInMemory(key);
    }

    public void dispose() {
        transactionalStore.dispose();
    }

    public Status getStatus() {
        return transactionalStore.getStatus();
    }

    public void expireElements() {
        transactionalStore.expireElements();
    }

    public void flush() throws IOException {
        transactionalStore.flush();
    }

    public boolean bufferFull() {
        return transactionalStore.bufferFull();
    }

    public Policy getInMemoryEvictionPolicy() {
        return transactionalStore.getInMemoryEvictionPolicy();
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        transactionalStore.setInMemoryEvictionPolicy(policy);
    }

    public Object getInternalContext() {
        return transactionalStore.getInternalContext();
    }

    public Object getMBean() {
        return transactionalStore.getMBean();
    }

    @Override
    public void setNodeCoherent(boolean coherent) {
        transactionalStore.setNodeCoherent(coherent);
    }

    @Override
    public void waitUntilClusterCoherent() {
        transactionalStore.waitUntilClusterCoherent();
    }
}
