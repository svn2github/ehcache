package net.sf.ehcache.store;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
import net.sf.ehcache.transaction.xa.EhcacheXAResource;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * @author Alex Snaps
 */
public class XATransactionalStore implements Store {

    private final Store underlyingStore;
    private final EhcacheXAResource xaResource;
  
    public XATransactionalStore(final EhcacheXAResource xaResource) {
        this.xaResource = xaResource;
        this.underlyingStore = xaResource.getStore();
    }
    

    public void put(final Element element) throws CacheException {
        TransactionContext context = getOrCreateTransactionContext();
        // In case this key is currently being updated...
        underlyingStore.get(element.getKey());
        context.addCommand(new StorePutCommand(element), element);
    
    }

    public void putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public Element get(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if(element == null && !context.isRemoved(key)) {
            element = xaResource.get(key);
        }
        return element;
    }

    public Element getQuiet(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if(element == null && !context.isRemoved(key)) {
            element = xaResource.getQuiet(key);
        }
        return element;
    }

    public Object[] getKeyArray() {
        TransactionContext context = getOrCreateTransactionContext();
        Set<Object> keys = new HashSet<Object>(Arrays.asList(underlyingStore.getKeyArray()));
        keys.addAll(context.getAddedKeys());
        keys.removeAll(context.getRemovedKeys());
        return keys.toArray();
    }

    public Element remove(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if(element == null && !context.isRemoved(key)) {
            element = xaResource.getQuiet(key);
        }
        if(element != null) {
            context.addCommand(new StoreRemoveCommand(key), element);
        }

        return element; // Todo is this good enough?
    }

    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public void removeAll() throws CacheException {     
        getOrCreateTransactionContext().addCommand(new StoreRemoveAllCommand(), null); // TODO is this meaningful? WRT getSize()
    }

    /**
     * Non transactional
     */
    public void dispose() {
        underlyingStore.dispose();
    }

    public int getSize() {
        TransactionContext context = getOrCreateTransactionContext();
        int size = underlyingStore.getSize();
        return size + context.getSizeModifier();
    }

    public int getTerracottaClusteredSize() {
        TransactionContext context = getOrCreateTransactionContext();
        return underlyingStore.getTerracottaClusteredSize() + context.getSizeModifier();
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
        TransactionContext context = getOrCreateTransactionContext();
        return !context.isRemoved(key) && (context.getAddedKeys().contains(key) || underlyingStore.containsKey(key));
    }

    public void expireElements() {
        getOrCreateTransactionContext().addCommand(new StoreExpireAllElementsCommand(), null); // TODO is this meaningful?
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
        return underlyingStore.bufferFull(); // TODO verify this really isn't tx!
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

    public void setCoherent(final boolean coherent) throws UnsupportedOperationException {
        underlyingStore.setCoherent(coherent);
    }

    public void waitUntilCoherent() throws UnsupportedOperationException {
        underlyingStore.waitUntilCoherent();
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
