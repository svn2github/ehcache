/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

    /**
     * Constructor
     * @param xaResource the xaResource wrapping the Cache this store is backing up
     */
    public XATransactionalStore(final EhcacheXAResource xaResource) {
        this.xaResource = xaResource;
        this.underlyingStore = xaResource.getStore();
    }


    /**
     * {@inheritDoc}
     */
    public void put(final Element element) throws CacheException {
        TransactionContext context = getOrCreateTransactionContext();
        // In case this key is currently being updated...
        underlyingStore.get(element.getKey());
        context.addCommand(new StorePutCommand(element), element);
    
    }

    /**
     * XATransactionalStore doesn't support putWithWriter
     * @throws UnsupportedOperationException
     */
    public void putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Element get(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if (element == null && !context.isRemoved(key)) {
            element = xaResource.get(key);
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
            element = xaResource.getQuiet(key);
        }
        return element;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getKeyArray() {
        TransactionContext context = getOrCreateTransactionContext();
        Set<Object> keys = new HashSet<Object>(Arrays.asList(underlyingStore.getKeyArray()));
        keys.addAll(context.getAddedKeys());
        keys.removeAll(context.getRemovedKeys());
        return keys.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        Element element = context.get(key);
        if (element == null && !context.isRemoved(key)) {
            element = xaResource.getQuiet(key);
        }
        if (element != null) {
            context.addCommand(new StoreRemoveCommand(key), element);
        }

        //todo is this really good enough?
        return element;
    }

    /**
     * XATransactionalStore doesn't support removeWithWriter
     * @throws UnsupportedOperationException
     */
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException();
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
     * @return size of the store, including tx local pending changes
     */
    public int getSize() {
        TransactionContext context = getOrCreateTransactionContext();
        int size = underlyingStore.getSize();
        return size + context.getSizeModifier();
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
    public long getSizeInBytes() {
        getOrCreateTransactionContext();
        return underlyingStore.getSizeInBytes();
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
     * @param key The Element key
     * @return whether the element is currently in the cache, or pending put
     */
    public boolean containsKey(final Object key) {
        TransactionContext context = getOrCreateTransactionContext();
        return !context.isRemoved(key) && (context.getAddedKeys().contains(key) || underlyingStore.containsKey(key));
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
    public Policy getEvictionPolicy() {
        return underlyingStore.getEvictionPolicy();
    }

    /**
     * Non transactional
     * {@inheritDoc}
     */
    public void setEvictionPolicy(final Policy policy) {
        underlyingStore.setEvictionPolicy(policy);
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
    public boolean isCacheCoherent() {
        return underlyingStore.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        return underlyingStore.isClusterCoherent();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        return underlyingStore.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherence(boolean coherent) {
        underlyingStore.setNodeCoherence(coherent);
    }

    /**
     * {@inheritDoc}
     */    
    public void waitUntilClusterCoherent() {
        underlyingStore.waitUntilClusterCoherent();
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
