package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.transaction.xa.EhCacheXAResource;
import net.sf.ehcache.transaction.xa.XAResourceRepository;

import java.io.IOException;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * @author Alex Snaps
 */
public class TransactionalStore implements Store {

    private final String cacheName;
    private final Store underlyingStore;
    private final TransactionManager transactionManager;
    private final XAResourceRepository resourceRepository;

    public TransactionalStore(String cacheName, final Store underlyingStore, TransactionManager transactionManager, XAResourceRepository resourceRepository) {
        this.cacheName = cacheName;
        this.underlyingStore = underlyingStore;
        this.transactionManager = transactionManager;
        this.resourceRepository = resourceRepository;
    }

    public void put(final Element element) throws CacheException {
        enlist();
        underlyingStore.put(element);
    }

    public Element get(final Object key) {
        enlist();
        return underlyingStore.get(key);
    }

    public Element getQuiet(final Object key) {
        enlist();
        return underlyingStore.getQuiet(key);
    }

    public Object[] getKeyArray() {
        enlist();
        return underlyingStore.getKeyArray();
    }

    public Element remove(final Object key) {
        enlist();
        return underlyingStore.remove(key);
    }

    public void removeAll() throws CacheException {
        enlist();
        underlyingStore.removeAll();
    }

    public void dispose() {
        enlist();
        underlyingStore.dispose();
    }

    public int getSize() {
        enlist();
        return underlyingStore.getSize();
    }

    public int getTerracottaClusteredSize() {
        enlist();
        return underlyingStore.getTerracottaClusteredSize();
    }

    public long getSizeInBytes() {
        enlist();
        return underlyingStore.getSizeInBytes();
    }

    public Status getStatus() {
        enlist();
        return underlyingStore.getStatus();
    }

    public boolean containsKey(final Object key) {
        enlist();
        return underlyingStore.containsKey(key);
    }

    public void expireElements() {
        enlist();
        underlyingStore.expireElements();
    }

    public void flush() throws IOException {
        enlist();
        underlyingStore.flush();
    }

    public boolean bufferFull() {
        enlist();
        return underlyingStore.bufferFull();
    }

    public Policy getEvictionPolicy() {
        enlist();
        return underlyingStore.getEvictionPolicy();
    }

    public void setEvictionPolicy(final Policy policy) {
        enlist();
        underlyingStore.setEvictionPolicy(policy);
    }

    public Object getInternalContext() {
        return underlyingStore.getInternalContext();
    }

    public boolean isCacheCoherent() {
        return underlyingStore.isCacheCoherent();
    }

    private void enlist() {
        try {
            Transaction transaction = transactionManager.getTransaction();
            transaction.enlistResource(resourceRepository.getOrCreateXAResource(cacheName, this));
        } catch (SystemException e) {
            throw new CacheException(e);
        } catch (RollbackException e) {
            throw new CacheException(e);
        }
    }
}
