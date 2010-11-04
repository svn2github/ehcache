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

package net.sf.ehcache.store;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.transaction.StoreExpireAllElementsCommand;
import net.sf.ehcache.transaction.StorePutCommand;
import net.sf.ehcache.transaction.StorePutIfAbsentCommand;
import net.sf.ehcache.transaction.StorePutWithWriterCommandImpl;
import net.sf.ehcache.transaction.StoreRemoveAllCommand;
import net.sf.ehcache.transaction.StoreRemoveCommand;
import net.sf.ehcache.transaction.StoreRemoveElementCommand;
import net.sf.ehcache.transaction.StoreRemoveWithWriterCommand;
import net.sf.ehcache.transaction.StoreReplaceCommand;
import net.sf.ehcache.transaction.StoreReplaceElementCommand;
import net.sf.ehcache.transaction.TransactionContext;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;
import net.sf.ehcache.transaction.xa.EhcacheXAResourceImpl;
import net.sf.ehcache.transaction.xa.EhcacheXAStore;
import net.sf.ehcache.transaction.xa.TwoPcExecutionListener;
import net.sf.ehcache.util.LargeSet;
import net.sf.ehcache.util.SetWrapperList;
import net.sf.ehcache.writer.CacheWriterManager;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A transaction aware store that wraps the actual Store.
 * It will provide proper READ_COMMITED transaction isolation. It does so by queuing write operations on the cache.
 * These will eventually be executed at transaction commit time, or discarded in case of rollback.
 *
 * @author Alex Snaps
 * @author Nabib El-Rahman
 */
public class XATransactionalStore extends AbstractStore {

    private final Store underlyingStore;
    private final Store oldVersionStore;

    private Ehcache cache;
    private EhcacheXAStore ehcacheXAStore;
    private TransactionManagerLookup transactionManagerLookup;
    private TransactionManager txnManager;

    private final ConcurrentHashMap<Transaction, EhcacheXAResource> transactionToXAResourceMap =
            new ConcurrentHashMap<Transaction, EhcacheXAResource>();

    /**
     * Create a store which will wrap another one to provide XA transactions.
     * @param cache the cache this store is backing
     * @param ehcacheXAStore the XAStore to be used by this store
     * @param transactionManagerLookup the TransactionManagerLookup used to get hold of the JTA transaction manager
     * @param txnManager
     */
    public XATransactionalStore(Ehcache cache, EhcacheXAStore ehcacheXAStore,
                                TransactionManagerLookup transactionManagerLookup, TransactionManager txnManager) {
        this.cache = cache;
        this.ehcacheXAStore = ehcacheXAStore;
        this.transactionManagerLookup = transactionManagerLookup;
        this.txnManager = txnManager;

        this.underlyingStore = ehcacheXAStore.getUnderlyingStore();
        this.oldVersionStore = ehcacheXAStore.getOldVersionStore();
    }


    /**
     * {@inheritDoc}
     */
    public boolean put(final Element element) throws CacheException {
        return internalPut(new StorePutCommand(element));
    }

    private boolean internalPut(final StorePutCommand putCommand) {
        final Element element = putCommand.getElement();
        boolean isNull;
        if (element == null) {
            return true;
        }
        TransactionContext context = getOrCreateTransactionContext();
        // In case this key is currently being updated...
        isNull = underlyingStore.get(element.getKey()) == null;
        if (isNull) {
            isNull = context.get(element.getKey()) == null;
        }
        context.addCommand(putCommand, element);
        return isNull;
    }

    /**
     * XATransactionalStore to put including to the underlying data store. That needs to be registered with the TransactionManager
     * and participate in the XA Transaction. The call to {@link net.sf.ehcache.writer.CacheWriterManager#put} will be held back
     * until commit time!
     *
     * @param element       the element to add to the store
     * @param writerManager will only work properly with {@link net.sf.ehcache.writer.writethrough.WriteThroughManager WriteThroughManager}
     */
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        return internalPut(new StorePutWithWriterCommandImpl(element));
    }

    /**
     * {@inheritDoc}
     */
    public Element get(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if (element == null && !context.isRemoved(key)) {
            element = getFromUnderlyingStore(key);
        }
        return element;
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if (element == null && !context.isRemoved(key)) {
            element = getQuietFromUnderlyingStore(key);
        }
        return element;
    }

    /**
     * {@inheritDoc}
     */
    public final List getKeys() {
        TransactionContext context = getOrCreateTransactionContext();
        Set < Object > keys = new LargeSet() {

        @Override
        public int sourceSize() {
           return underlyingStore.getSize();
        }

        @Override
        public Iterator < Object > sourceIterator() {
          return underlyingStore.getKeys().iterator();
        }
        };
        keys.addAll(context.getAddedKeys());
        keys.removeAll(context.getRemovedKeys());
        return new SetWrapperList(keys);
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(final Object key) {
        return removeInternal(new StoreRemoveCommand(new CacheEntry(key, retrieveElement(key))));
    }

    private Element retrieveElement(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if (element == null && !context.isRemoved(key)) {
            element = getQuietFromUnderlyingStore(key);
        }

        return element;
    }

    private Element removeInternal(final StoreRemoveCommand command) {
        Element element = command.getEntry().getElement();
        getOrCreateTransactionContext().addCommand(command, element);
        return element;
    }

    /**
     * XATransactionalStore to remove including from the underlying data store. That needs to be registered with the TransactionManager
     * and participate in the XA Transaction. The call to {@link net.sf.ehcache.writer.CacheWriterManager#remove} will be not held back
     * until commit time!
     *
     * @param key           the key to remove
     * @param writerManager will only work properly with {@link net.sf.ehcache.writer.writethrough.WriteThroughManager WriteThroughManager}
     * @return the value to be removed
     */
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        return removeInternal(new StoreRemoveWithWriterCommand(new CacheEntry(key, retrieveElement(key))));
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        // todo file jira here
        getOrCreateTransactionContext().addCommand(new StoreRemoveAllCommand(), null);
    }

    /**
     * Non transactional
     * <p>{@inheritDoc}
     */
    public void dispose() {
        underlyingStore.dispose();
    }

    /**
     * TransactionContext impacted size of the store
     *
     * @return size of the store, including transaction local pending changes
     */
    public int getSize() {
        TransactionContext context = getOrCreateTransactionContext();
        int size = underlyingStore.getSize();
        return size + context.getSizeModifier();
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return underlyingStore.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return underlyingStore.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return underlyingStore.getInMemorySize();
    }
    
    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        TransactionContext context = getOrCreateTransactionContext();
        return underlyingStore.getTerracottaClusteredSize() + context.getSizeModifier();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        getOrCreateTransactionContext();
        return underlyingStore.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        getOrCreateTransactionContext();
        return underlyingStore.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        getOrCreateTransactionContext();
        return underlyingStore.getOnDiskSizeInBytes();
    }
    
    /**
     * Non transactional
     * <p>{@inheritDoc}
     */
    public Status getStatus() {
        return underlyingStore.getStatus();
    }

    /**
     * {@inheritDoc}
     *
     * @param key The Element key
     * @return whether the element is currently in the cache, or pending put
     */
    public boolean containsKey(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        return !context.isRemoved(key) && (context.getAddedKeys().contains(key) || underlyingStore.containsKey(key));
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(final Object key) {
        return underlyingStore.containsKeyInMemory(key);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(final Object key) {
        return underlyingStore.containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(final Object key) {
        return underlyingStore.containsKeyOnDisk(key);
    }
    
    /**
     * Non transactional
     * {@inheritDoc}
     */
    public void expireElements() {
        // todo file jira
        getOrCreateTransactionContext().addCommand(new StoreExpireAllElementsCommand(), null);
    }

    /**
     * Non transactional
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        underlyingStore.flush();
    }

    /**
     * Non transactional
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return underlyingStore.bufferFull();
    }

    /**
     * Non transactional
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return underlyingStore.getInMemoryEvictionPolicy();
    }

    /**
     * Non transactional
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(final Policy policy) {
        underlyingStore.setInMemoryEvictionPolicy(policy);
    }

    /**
     * Non transactional
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return underlyingStore.getInternalContext();
    }

    /**
     * Non transactional
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheCoherent() {
        return underlyingStore.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClusterCoherent() {
        return underlyingStore.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNodeCoherent() {
        return underlyingStore.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeCoherent(boolean coherent) {
        underlyingStore.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilClusterCoherent() {
        underlyingStore.waitUntilClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        TransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        if (previous == null) {
            context.addCommand(new StorePutIfAbsentCommand(element), element);
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        TransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        if (previous != null && previous.getValue().equals(element.getValue())) {
            context.addCommand(new StoreRemoveElementCommand(element, comparator), element);
            return previous;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator)
            throws NullPointerException, IllegalArgumentException {
        TransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        boolean replaced = false;
        if (previous != null && previous.getValue().equals(old.getValue())) {
            context.addCommand(new StoreReplaceElementCommand(old, element, comparator), element);
            replaced = true;
        }
        return replaced;
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        TransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        if (previous != null) {
            context.addCommand(new StoreReplaceCommand(element), element);
        }
        return previous;
    }

    private Element getCurrentElement(final Serializable key, final TransactionContext context) {
        Element previous = context.get(key);
        if (previous == null && !context.isRemoved(key)) {
            previous = getFromUnderlyingStore(key);
        }
        return previous;
    }

    /* 1 xaresource per transaction */

    private Element getFromUnderlyingStore(final Object key) {
        Element element = oldVersionStore.get(key);
        if (element == null) {
            element = underlyingStore.get(key);
        }
        return element;
    }

    private Element getQuietFromUnderlyingStore(final Object key) {
        Element element = oldVersionStore.getQuiet(key);
        if (element == null) {
            element = underlyingStore.getQuiet(key);
        }
        return element;
    }

    /**
     * This method either returns the XAResource associated with the current transaction or creates a new one
     * if there was none yet.
     * @return the XAResource bound to this transaction
     */
    public EhcacheXAResource getOrCreateXAResource() {
        try {
            Transaction transaction = txnManager.getTransaction();
            if (transaction == null) {
                throw new CacheException("Cache " + cache.getName() + " can only be accessed within a JTA Transaction!");
            }

            EhcacheXAResource xaResource = transactionToXAResourceMap.get(transaction);
            if (xaResource == null) {
                xaResource = new EhcacheXAResourceImpl(cache, txnManager, ehcacheXAStore);
                transactionToXAResourceMap.put(transaction, xaResource);
            }

            return xaResource;
        } catch (SystemException e) {
            throw new CacheException(e);
        }
    }

    private TransactionContext getOrCreateTransactionContext() {
        try {
            Transaction transaction = txnManager.getTransaction();
            if (transaction == null) {
                throw new CacheException("Cache " + cache.getName() + " can only be accessed within a JTA Transaction!");
            }
            if (transaction.getStatus() == javax.transaction.Status.STATUS_MARKED_ROLLBACK) {
                throw new CacheException("Cache [" + cache.getName() + "] cannot be used anymore, " +
                                         "transaction is marked for rollback: " + transaction);                
            }

            TransactionContext context;
            EhcacheXAResource xaResource = getOrCreateXAResource();
            context = xaResource.getCurrentTransactionContext();
            if (context != null) {
                return context;
            }

            // xaResource.createTransactionContext() is going to enlist the XAResource in
            // the transaction so it MUST be registered first
            transactionManagerLookup.register(xaResource);
            context = xaResource.createTransactionContext();
            xaResource.addTwoPcExecutionListener(new CleanupTransactionContext(transaction));

            return context;

        } catch (SystemException e) {
            throw new CacheException(e);
        } catch (RollbackException e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return underlyingStore.getMBean();
    }

    /**
     * This class is used to clean up the transactionToContextMap after a transaction
     * committed or rolled back.
     */
    private final class CleanupTransactionContext implements TwoPcExecutionListener {

        private Transaction transaction;

        private CleanupTransactionContext(Transaction transaction) {
            this.transaction = transaction;
        }

        public void beforePrepare(EhcacheXAResource xaResource) {
        }

        public void afterCommitOrRollback(EhcacheXAResource xaResource) {
            transactionToXAResourceMap.remove(transaction);
            transactionManagerLookup.unregister(xaResource);
        }
    }

}
