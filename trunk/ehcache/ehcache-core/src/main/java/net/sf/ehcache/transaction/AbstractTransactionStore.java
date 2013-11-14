/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.transaction;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.AbstractStore;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.context.annotations.ContextChild;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Abstract transactional store which provides implementation of all non-transactional methods
 *
 * @author Ludovic Orban
 */
public abstract class AbstractTransactionStore extends AbstractStore implements TerracottaStore {

    /**
     * The underlying store wrapped by this store
     */
    @ContextChild protected final Store underlyingStore;

    /**
     * Constructor
     * @param underlyingStore the underlying store
     */
    protected AbstractTransactionStore(Store underlyingStore) {
        this.underlyingStore = underlyingStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results executeQuery(StoreQuery query) {
        return underlyingStore.executeQuery(query);
    }

    /* non-transactional methods */

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return underlyingStore.getInMemorySize();
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
    public int getOnDiskSize() {
        return underlyingStore.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return underlyingStore.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return underlyingStore.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return underlyingStore.getOnDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return underlyingStore.containsKeyOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return underlyingStore.containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return underlyingStore.containsKeyInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        underlyingStore.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return underlyingStore.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        underlyingStore.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        underlyingStore.flush();
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return underlyingStore.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return underlyingStore.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        underlyingStore.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return underlyingStore.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return underlyingStore.getMBean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeCoherent(boolean coherent) {
        if (!coherent) {
            throw new InvalidConfigurationException("a transactional cache cannot be incoherent");
        }
        underlyingStore.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isNodeCoherent()
     */
    @Override
    public boolean isNodeCoherent() {
        return underlyingStore.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isCacheCoherent()
     */
    @Override
    public boolean isCacheCoherent() {
        return underlyingStore.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isClusterCoherent()
     */
    @Override
    public boolean isClusterCoherent() {
        return underlyingStore.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     * @throws InterruptedException
     * @throws UnsupportedOperationException
     * @throws TerracottaNotRunningException
     */
    @Override
    public void waitUntilClusterCoherent() throws TerracottaNotRunningException, UnsupportedOperationException, InterruptedException {
        underlyingStore.waitUntilClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        underlyingStore.setAttributeExtractors(extractors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Attribute<T> getSearchAttribute(String attributeName) throws CacheException {
        return underlyingStore.getSearchAttribute(attributeName);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Attribute> getSearchAttributes() {
        return underlyingStore.getSearchAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAbortedSizeOf() {
        return underlyingStore.hasAbortedSizeOf();
    }

    /* TerracottaStore methods */

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore) underlyingStore).unsafeGet(key);
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore) underlyingStore).getLocalKeys();
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    /**
     * {@inheritDoc}
     */
    public CacheConfiguration.TransactionalMode getTransactionalMode() {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore) underlyingStore).getTransactionalMode();
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    @Override
    public WriteBehind createWriteBehind() {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore)underlyingStore).createWriteBehind();
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    @Override
    public void quickClear() {
        if (underlyingStore instanceof TerracottaStore) {
            ((TerracottaStore)underlyingStore).quickClear();
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    @Override
    public int quickSize() {
        if (underlyingStore instanceof TerracottaStore) {
            return ((TerracottaStore)underlyingStore).quickSize();
        }
        throw new CacheException("underlying store is not an instance of TerracottaStore");
    }

    /**
     * Method to get to the {@link Element} matching the key, oblivious of any in-flight transaction.
     *
     * @param key the key to look for
     * @return the mapped element, outside of any transaction
     */
    public Element getOldElement(Object key) {
        if (key == null) {
            return null;
        }

        Element oldElement = underlyingStore.getQuiet(key);
        if (oldElement == null) {
            return null;
        }

        Object value = oldElement.getObjectValue();
        if (value instanceof SoftLockID) {
            return ((SoftLockID)value).getOldElement();
        } else {
            return oldElement;
        }
    }

    @Override
    public void notifyCacheEventListenersChanged() {
        if (underlyingStore instanceof TerracottaStore) {
            ((TerracottaStore)underlyingStore).notifyCacheEventListenersChanged();
        }
    }
}
