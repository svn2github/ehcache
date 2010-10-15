package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.AbstractNonXaTransactionalStore;
import net.sf.ehcache.store.AbstractStore;
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
public class JtaNonXaTransactionalStore extends AbstractStore {

    private final AbstractNonXaTransactionalStore transactionalStore;
    private final TransactionController transactionController;
    private final TransactionManager transactionManager;

    public JtaNonXaTransactionalStore(AbstractNonXaTransactionalStore transactionalStore, TransactionManagerLookup transactionManagerLookup, TransactionController transactionController) {
        this.transactionalStore = transactionalStore;
        this.transactionController = transactionController;
        this.transactionManager = transactionManagerLookup.getTransactionManager();
        if (this.transactionManager == null) {
            throw new TransactionException("no JTA transaction manager could be located, cannot bind non-xa cache with JTA transactions");
        }
    }

    private synchronized void registerInJtaContext() {
        if (transactionController.getCurrentTransactionContext() != null) {
            // already started local TX and registered in JTA
            return;
        }

        try {
            Transaction tx = transactionManager.getTransaction();
            if (tx == null) {
                throw new TransactionException("no JTA transaction context");
            }
            transactionController.begin(1);
            tx.registerSynchronization(new NonXaEhcacheSynchronization(transactionController));
        } catch (SystemException e) {
            throw new TransactionException("internal JTA exception", e);
        } catch (RollbackException e) {
            throw new TransactionException("JTA transaction rolled back", e);
        }
    }

    /* transactional methods */

    public boolean put(Element element) throws CacheException {
        registerInJtaContext();
        return transactionalStore.put(element);
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        return transactionalStore.putWithWriter(element, writerManager);
    }

    public Element get(Object key) {
        registerInJtaContext();
        return transactionalStore.get(key);
    }

    public Element getQuiet(Object key) {
        registerInJtaContext();
        return transactionalStore.getQuiet(key);
    }

    public List getKeys() {
        registerInJtaContext();
        return transactionalStore.getKeys();
    }

    public Element remove(Object key) {
        registerInJtaContext();
        return transactionalStore.remove(key);
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        registerInJtaContext();
        return transactionalStore.removeWithWriter(key, writerManager);
    }

    public void removeAll() throws CacheException {
        registerInJtaContext();
        transactionalStore.removeAll();
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        registerInJtaContext();
        return transactionalStore.putIfAbsent(element);
    }

    public Element removeElement(Element element) throws NullPointerException {
        registerInJtaContext();
        return transactionalStore.removeElement(element);
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        registerInJtaContext();
        return transactionalStore.replace(old, element);
    }

    public Element replace(Element element) throws NullPointerException {
        registerInJtaContext();
        return transactionalStore.replace(element);
    }

    public int getSize() {
        registerInJtaContext();
        return transactionalStore.getSize();
    }

    public int getInMemorySize() {
        registerInJtaContext();
        return transactionalStore.getInMemorySize();
    }

    public int getOffHeapSize() {
        registerInJtaContext();
        return transactionalStore.getOffHeapSize();
    }

    public int getOnDiskSize() {
        registerInJtaContext();
        return transactionalStore.getOnDiskSize();
    }

    public int getTerracottaClusteredSize() {
        registerInJtaContext();
        return transactionalStore.getTerracottaClusteredSize();
    }

    public long getInMemorySizeInBytes() {
        registerInJtaContext();
        return transactionalStore.getInMemorySizeInBytes();
    }

    public long getOffHeapSizeInBytes() {
        registerInJtaContext();
        return transactionalStore.getOffHeapSizeInBytes();
    }

    public long getOnDiskSizeInBytes() {
        registerInJtaContext();
        return transactionalStore.getOnDiskSizeInBytes();
    }

    public boolean containsKey(Object key) {
        registerInJtaContext();
        return transactionalStore.containsKey(key);
    }

    public boolean containsKeyOnDisk(Object key) {
        registerInJtaContext();
        return transactionalStore.containsKeyOnDisk(key);
    }

    public boolean containsKeyOffHeap(Object key) {
        registerInJtaContext();
        return transactionalStore.containsKeyOffHeap(key);
    }

    public boolean containsKeyInMemory(Object key) {
        registerInJtaContext();
        return transactionalStore.containsKeyInMemory(key);
    }

    /* non-transactional methods */

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

}
