package net.sf.ehcache.store;

import java.io.IOException;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.transaction.StoreExpireAllElementsCommand;
import net.sf.ehcache.transaction.StorePutCommand;
import net.sf.ehcache.transaction.StoreRemoveAllCommand;
import net.sf.ehcache.transaction.StoreRemoveCommand;
import net.sf.ehcache.transaction.TransactionContext;
import net.sf.ehcache.transaction.xa.EhCacheXAResource;

/**
 * @author Alex Snaps
 */
public class XaTransactionalStore implements Store {

    private final Store underlyingStore;
    private final EhCacheXAResource xaResource;
  
    public XaTransactionalStore(final EhCacheXAResource xaResource) {
        this.xaResource = xaResource;
        this.underlyingStore = xaResource.getStore();
    }
    

    public void put(final Element element) throws CacheException {
        TransactionContext context = getOrCreateTransactionContext();
        context.addCommand(new StorePutCommand(element));
        // TODO: That's probably not always true... For update yes, but for fresh inserts? 
        xaResource.checkout(element, context.getTransaction());
    }

    public Element get(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = underlyingStore.get(key);
        if(element != null) {
            xaResource.checkout(element, context.getTransaction());
        }
        return element;
    }

    public Element getQuiet(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = underlyingStore.getQuiet(key);
        if(element != null) {
            xaResource.checkout(element, context.getTransaction());
        }
        return element;
    }

    public Object[] getKeyArray() {
        return underlyingStore.getKeyArray();
    }

    public Element remove(final Object key) {
        Element element = underlyingStore.get(key);
        TransactionContext context = getOrCreateTransactionContext();
        context.addCommand(new StoreRemoveCommand(key, element));
        if(element != null) {
            xaResource.checkout(element, context.getTransaction());
        }
        return element; // Todo is this good enough?
    }

    public void removeAll() throws CacheException {
        getOrCreateTransactionContext().addCommand(new StoreRemoveAllCommand()); // TODO is this meaningful? WRT getSize()
    }

    /**
     * Non transactional
     */
    public void dispose() {
        underlyingStore.dispose();
    }

    public int getSize() {
        getOrCreateTransactionContext();
        return underlyingStore.getSize(); // TODO Argh?! Can this work outside any transaction? probably...
    }

    public int getTerracottaClusteredSize() {
        getOrCreateTransactionContext();
        return underlyingStore.getTerracottaClusteredSize(); // todo Can this work outside any transaction? probably...
    }

    public long getSizeInBytes() {
        getOrCreateTransactionContext();
        return underlyingStore.getSizeInBytes(); // todo Can this work outside any transaction? probably...
    }

    /**
     * Non transactional
     */
    public Status getStatus() {
        return underlyingStore.getStatus();
    }

    public boolean containsKey(final Object key) {
        getOrCreateTransactionContext();
        return underlyingStore.containsKey(key);
    }

    public void expireElements() {
        getOrCreateTransactionContext().addCommand(new StoreExpireAllElementsCommand()); // TODO is this meaningful? WRT getSize()
    }

    /**
     * Non transactional
     */
    public void flush() throws IOException {
        underlyingStore.flush();
    }

    /**
     * Non transactional
     */
    public boolean bufferFull() {
        return underlyingStore.bufferFull(); // TODO verify this really isn't!
    }

    /**
     * Non transactional
     */
    public Policy getEvictionPolicy() {
        return underlyingStore.getEvictionPolicy();
    }

    /**
     * Non transactional
     */
    public void setEvictionPolicy(final Policy policy) {
        underlyingStore.setEvictionPolicy(policy);
    }

    /**
     * Non transactional
     */
    public Object getInternalContext() {
        return underlyingStore.getInternalContext();
    }

    /**
     * Non transactional
     */
    public boolean isCacheCoherent() {
        return underlyingStore.isCacheCoherent();
    }

    private TransactionContext getOrCreateTransactionContext() {
        TransactionContext context;
        try {
            context = xaResource.getOrCreateTransactionContext();
        } catch (SystemException e) {
            throw new CacheException(e);
        } catch (RollbackException e) {
            throw new CacheException(e);
        }
        return context;
    }
}
