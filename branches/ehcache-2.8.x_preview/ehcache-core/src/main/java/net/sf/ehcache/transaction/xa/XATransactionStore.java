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
package net.sf.ehcache.transaction.xa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.TimeUnit;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.statistics.StatisticBuilder;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.transaction.AbstractTransactionStore;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockManager;
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.TransactionAwareAttributeExtractor;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.TransactionInterruptedException;
import net.sf.ehcache.transaction.TransactionTimeoutException;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.commands.StorePutCommand;
import net.sf.ehcache.transaction.xa.commands.StoreRemoveCommand;
import net.sf.ehcache.util.LargeSet;
import net.sf.ehcache.util.SetAsList;
import net.sf.ehcache.writer.CacheWriterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.observer.OperationObserver;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Ludovic Orban
 */
public class XATransactionStore extends AbstractTransactionStore {

    private static final Logger LOG = LoggerFactory.getLogger(XATransactionStore.class.getName());

    private final TransactionManagerLookup transactionManagerLookup;
    private final TransactionIDFactory transactionIdFactory;
    private final SoftLockManager softLockManager;
    private final Ehcache cache;
    private final EhcacheXAResourceImpl recoveryResource;

    private final ConcurrentHashMap<Transaction, EhcacheXAResource> transactionToXAResourceMap =
            new ConcurrentHashMap<Transaction, EhcacheXAResource>();
    private final ConcurrentHashMap<Transaction, Long> transactionToTimeoutMap = new ConcurrentHashMap<Transaction, Long>();

    private final OperationObserver<XaCommitOutcome> commitObserver = StatisticBuilder.operation(XaCommitOutcome.class)
            .of(this).named("xa-commit").tag("xa-transactional").build();
    private final OperationObserver<XaRollbackOutcome> rollbackObserver = StatisticBuilder.operation(XaRollbackOutcome.class)
            .of(this).named("xa-rollback").tag("xa-transactional").build();
    private final OperationObserver<XaRecoveryOutcome> recoveryObserver = StatisticBuilder.operation(XaRecoveryOutcome.class)
            .of(this).named("xa-recovery").tag("xa-transactional").build();

    /**
     * Constructor
     * @param transactionManagerLookup the transaction manager lookup implementation
     * @param softLockManager the soft lock manager
     * @param transactionIdFactory the transaction ID factory
     * @param cache the cache
     * @param store the underlying store
     * @param copyStrategy the original copy strategy
     */
    public XATransactionStore(TransactionManagerLookup transactionManagerLookup, SoftLockManager softLockManager,
                              TransactionIDFactory transactionIdFactory, Ehcache cache, Store store,
                              ReadWriteCopyStrategy<Element> copyStrategy) {
        super(store, copyStrategy);
        this.transactionManagerLookup = transactionManagerLookup;
        this.transactionIdFactory = transactionIdFactory;
        if (transactionManagerLookup.getTransactionManager() == null) {
            throw new TransactionException("no JTA transaction manager could be located, cannot bind twopc cache with JTA");
        }
        this.softLockManager = softLockManager;
        this.cache = cache;

        // this xaresource is for initial registration and recovery
        this.recoveryResource = new EhcacheXAResourceImpl(cache, underlyingStore, transactionManagerLookup, softLockManager, transactionIdFactory,
                copyStrategy, commitObserver, rollbackObserver, recoveryObserver);
        transactionManagerLookup.register(recoveryResource, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        transactionManagerLookup.unregister(recoveryResource, true);
    }

    private Transaction getCurrentTransaction() throws SystemException {
        Transaction transaction = transactionManagerLookup.getTransactionManager().getTransaction();
        if (transaction == null) {
            throw new TransactionException("JTA transaction not started");
        }
        return transaction;
    }

    /**
     * Get or create the XAResource of this XA store
     * @return the EhcacheXAResource of this store
     * @throws SystemException when something goes wrong with the transaction manager
     */
    public EhcacheXAResourceImpl getOrCreateXAResource() throws SystemException {
        Transaction transaction = getCurrentTransaction();
        EhcacheXAResourceImpl xaResource = (EhcacheXAResourceImpl) transactionToXAResourceMap.get(transaction);
        if (xaResource == null) {
            LOG.debug("creating new XAResource");
            xaResource = new EhcacheXAResourceImpl(cache, underlyingStore, transactionManagerLookup,
                    softLockManager, transactionIdFactory, copyStrategy, commitObserver, rollbackObserver,
                    recoveryObserver);
            transactionToXAResourceMap.put(transaction, xaResource);
            xaResource.addTwoPcExecutionListener(new CleanupXAResource(getCurrentTransaction()));
        }
        return xaResource;
    }

    private XATransactionContext getTransactionContext() {
        try {
            Transaction transaction = getCurrentTransaction();
            EhcacheXAResourceImpl xaResource = (EhcacheXAResourceImpl) transactionToXAResourceMap.get(transaction);
            if (xaResource == null) {
                return null;
            }
            XATransactionContext transactionContext = xaResource.getCurrentTransactionContext();

            if (transactionContext == null) {
                transactionManagerLookup.register(xaResource, false);
                LOG.debug("creating new XA context");
                transactionContext = xaResource.createTransactionContext();
                xaResource.addTwoPcExecutionListener(new UnregisterXAResource());
            } else {
                transactionContext = xaResource.getCurrentTransactionContext();
            }

            LOG.debug("using XA context {}", transactionContext);
            return transactionContext;
        } catch (SystemException e) {
            throw new TransactionException("cannot get the current transaction", e);
        } catch (RollbackException e) {
            throw new TransactionException("transaction rolled back", e);
        }
    }

    private XATransactionContext getOrCreateTransactionContext() {
        try {
            EhcacheXAResourceImpl xaResource = getOrCreateXAResource();
            XATransactionContext transactionContext = xaResource.getCurrentTransactionContext();

            if (transactionContext == null) {
                transactionManagerLookup.register(xaResource, false);
                LOG.debug("creating new XA context");
                transactionContext = xaResource.createTransactionContext();
                xaResource.addTwoPcExecutionListener(new UnregisterXAResource());
            } else {
                transactionContext = xaResource.getCurrentTransactionContext();
            }

            LOG.debug("using XA context {}", transactionContext);
            return transactionContext;
        } catch (SystemException e) {
            throw new TransactionException("cannot get the current transaction", e);
        } catch (RollbackException e) {
            throw new TransactionException("transaction rolled back", e);
        }
    }

    /**
     * This class is used to clean up the transactionToTimeoutMap after a transaction
     * committed or rolled back.
     */
    private final class CleanupTimeout implements Synchronization {
        private final Transaction transaction;

        private CleanupTimeout(final Transaction transaction) {
            this.transaction = transaction;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(final int status) {
            transactionToTimeoutMap.remove(transaction);
        }
    }

    /**
     * This class is used to clean up the transactionToXAResourceMap after a transaction
     * committed or rolled back.
     */
    private final class CleanupXAResource implements XAExecutionListener {
        private final Transaction transaction;

        private CleanupXAResource(Transaction transaction) {
            this.transaction = transaction;
        }

        public void beforePrepare(EhcacheXAResource xaResource) {
        }

        public void afterCommitOrRollback(EhcacheXAResource xaResource) {
            transactionToXAResourceMap.remove(transaction);
        }
    }

    /**
     * This class is used to unregister the XAResource after a transaction
     * committed or rolled back.
     */
    private final class UnregisterXAResource implements XAExecutionListener {

        public void beforePrepare(EhcacheXAResource xaResource) {
        }

        public void afterCommitOrRollback(EhcacheXAResource xaResource) {
            transactionManagerLookup.unregister(xaResource, false);
        }
    }


    /**
     * @return milliseconds left before timeout
     */
    private long assertNotTimedOut() {
        try {
            if (Thread.interrupted()) {
                throw new TransactionInterruptedException("transaction interrupted");
            }

            Transaction transaction = getCurrentTransaction();
            Long timeoutTimestamp = transactionToTimeoutMap.get(transaction);
            long now = MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
            if (timeoutTimestamp == null) {
                long timeout;
                EhcacheXAResource xaResource = transactionToXAResourceMap.get(transaction);
                if (xaResource != null) {
                    int xaResourceTimeout = xaResource.getTransactionTimeout();
                    timeout = MILLISECONDS.convert(xaResourceTimeout, TimeUnit.SECONDS);
                } else {
                    int defaultTransactionTimeout = cache.getCacheManager().getTransactionController().getDefaultTransactionTimeout();
                    timeout = MILLISECONDS.convert(defaultTransactionTimeout, TimeUnit.SECONDS);
                }
                timeoutTimestamp = now + timeout;
                transactionToTimeoutMap.put(transaction, timeoutTimestamp);
                try {
                    transaction.registerSynchronization(new CleanupTimeout(transaction));
                } catch (RollbackException e) {
                    throw new TransactionException("transaction has been marked as rollback only", e);
                }
                return timeout;
            } else {
                long timeToExpiry = timeoutTimestamp - now;
                if (timeToExpiry <= 0) {
                    throw new TransactionTimeoutException("transaction timed out");
                } else {
                    return timeToExpiry;
                }
            }
        } catch (SystemException e) {
            throw new TransactionException("cannot get the current transaction", e);
        } catch (XAException e) {
            throw new TransactionException("cannot get the XAResource transaction timeout", e);
        }
    }

    /* transactional methods */

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        LOG.debug("cache {} get {}", cache.getName(), key);
        XATransactionContext context = getTransactionContext();
        Element element;
        if (context == null) {
            element = getFromUnderlyingStore(key);
        } else {
            element = context.get(key);
            if (element == null && !context.isRemoved(key)) {
                element = getFromUnderlyingStore(key);
            }
        }
        return copyElementForRead(element);
    }


    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        LOG.debug("cache {} getQuiet {}", cache.getName(), key);
        XATransactionContext context = getTransactionContext();
        Element element;
        if (context == null) {
            element = getQuietFromUnderlyingStore(key);
        } else {
            element = context.get(key);
            if (element == null && !context.isRemoved(key)) {
                element = getQuietFromUnderlyingStore(key);
            }
        }
        return copyElementForRead(element);
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        LOG.debug("cache {} getSize", cache.getName());
        XATransactionContext context = getOrCreateTransactionContext();
        int size = underlyingStore.getSize();
        return Math.max(0, size + context.getSizeModifier());
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        try {
            Transaction transaction = transactionManagerLookup.getTransactionManager().getTransaction();
            if (transaction == null) {
                return underlyingStore.getTerracottaClusteredSize();
            }
        } catch (SystemException se) {
            throw new TransactionException("cannot get the current transaction", se);
        }

        LOG.debug("cache {} getTerracottaClusteredSize", cache.getName());
        XATransactionContext context = getOrCreateTransactionContext();
        int size = underlyingStore.getTerracottaClusteredSize();
        return size + context.getSizeModifier();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        LOG.debug("cache {} containsKey", cache.getName(), key);
        XATransactionContext context = getOrCreateTransactionContext();
        return !context.isRemoved(key) && (context.getAddedKeys().contains(key) || underlyingStore.containsKey(key));
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        LOG.debug("cache {} getKeys", cache.getName());
        XATransactionContext context = getOrCreateTransactionContext();
        Set<Object> keys = new LargeSet<Object>() {

            @Override
            public int sourceSize() {
                return underlyingStore.getSize();
            }

            @Override
            public Iterator<Object> sourceIterator() {
                return underlyingStore.getKeys().iterator();
            }
        };
        keys.addAll(context.getAddedKeys());
        keys.removeAll(context.getRemovedKeys());
        return new SetAsList<Object>(keys);
    }


    private Element getFromUnderlyingStore(final Object key) {
        while (true) {
            long timeLeft = assertNotTimedOut();
            LOG.debug("cache {} underlying.get key {} not timed out, time left: " + timeLeft, cache.getName(), key);

            Element element = underlyingStore.get(key);
            if (element == null) {
                return null;
            }
            Object value = element.getObjectValue();
            if (value instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) value;
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                if (softLock == null) {
                    LOG.debug("cache {} underlying.get key {} soft lock died, retrying...", cache.getName(), key);
                    continue;
                } else {
                    try {
                        LOG.debug("cache {} key {} soft locked, awaiting unlock...", cache.getName(), key);
                        if (softLock.tryLock(timeLeft)) {
                            softLock.clearTryLock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                return element;
            }
        }
    }

    private Element getQuietFromUnderlyingStore(final Object key) {
        while (true) {
            long timeLeft = assertNotTimedOut();
            LOG.debug("cache {} underlying.getQuiet key {} not timed out, time left: " + timeLeft, cache.getName(), key);

            Element element = underlyingStore.getQuiet(key);
            if (element == null) {
                return null;
            }
            Object value = element.getObjectValue();
            if (value instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) value;
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                if (softLock == null) {
                    LOG.debug("cache {} underlying.getQuiet key {} soft lock died, retrying...", cache.getName(), key);
                    continue;
                } else {
                    try {
                        LOG.debug("cache {} key {} soft locked, awaiting unlock...", cache.getName(), key);
                        if (softLock.tryLock(timeLeft)) {
                            softLock.clearTryLock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                return element;
            }
        }
    }

    private Element getCurrentElement(final Object key, final XATransactionContext context) {
        Element previous = context.get(key);
        if (previous == null && !context.isRemoved(key)) {
            previous = getQuietFromUnderlyingStore(key);
        }
        return previous;
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        LOG.debug("cache {} put {}", cache.getName(), element);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        getOrCreateTransactionContext();

        Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
        return internalPut(new StorePutCommand(oldElement, copyElementForWrite(element)));
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        LOG.debug("cache {} putWithWriter {}", cache.getName(), element);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        getOrCreateTransactionContext();

        Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
        if (writerManager != null) {
            writerManager.put(element);
        } else {
            cache.getWriterManager().put(element);
        }
        return internalPut(new StorePutCommand(oldElement, copyElementForWrite(element)));
    }

    private boolean internalPut(final StorePutCommand putCommand) {
        final Element element = putCommand.getElement();
        boolean isNull;
        if (element == null) {
            return true;
        }
        XATransactionContext context = getOrCreateTransactionContext();
        // In case this key is currently being updated...
        isNull = underlyingStore.get(element.getKey()) == null;
        if (isNull) {
            isNull = context.get(element.getKey()) == null;
        }
        context.addCommand(putCommand, element);
        return isNull;
    }


    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        LOG.debug("cache {} remove {}", cache.getName(), key);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        getOrCreateTransactionContext();

        Element oldElement = getQuietFromUnderlyingStore(key);
        return removeInternal(new StoreRemoveCommand(key, oldElement));
    }

    private Element removeInternal(final StoreRemoveCommand command) {
        Element element = command.getEntry().getElement();
        getOrCreateTransactionContext().addCommand(command, element);
        return copyElementForRead(element);
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        LOG.debug("cache {} removeWithWriter {}", cache.getName(), key);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        getOrCreateTransactionContext();

        Element oldElement = getQuietFromUnderlyingStore(key);
        if (writerManager != null) {
            writerManager.remove(new CacheEntry(key, null));
        } else {
            cache.getWriterManager().remove(new CacheEntry(key, null));
        }
        return removeInternal(new StoreRemoveCommand(key, oldElement));
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        LOG.debug("cache {} removeAll", cache.getName());
        List keys = getKeys();
        for (Object key : keys) {
            remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        LOG.debug("cache {} putIfAbsent {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getObjectKey(), context);

        if (previous == null) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            Element elementForWrite = copyElementForWrite(element);
            context.addCommand(new StorePutCommand(oldElement, elementForWrite), elementForWrite);
        }

        return copyElementForRead(previous);
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        LOG.debug("cache {} removeElement {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        Element elementForWrite = copyElementForWrite(element);
        if (previous != null && comparator.equals(previous, elementForWrite)) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            context.addCommand(new StoreRemoveCommand(element.getObjectKey(), oldElement), elementForWrite);
            return copyElementForRead(previous);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator)
            throws NullPointerException, IllegalArgumentException {
        LOG.debug("cache {} replace2 {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        boolean replaced = false;
        if (previous != null && comparator.equals(previous, copyElementForWrite(old))) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            Element elementForWrite = copyElementForWrite(element);
            context.addCommand(new StorePutCommand(oldElement, elementForWrite), elementForWrite);
            replaced = true;
        }
        return replaced;
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        LOG.debug("cache {} replace1 {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        if (previous != null) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            Element elementForWrite = copyElementForWrite(element);
            context.addCommand(new StorePutCommand(oldElement, elementForWrite), elementForWrite);
        }
        return copyElementForRead(previous);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        Map<String, AttributeExtractor> wrappedExtractors = new HashMap(extractors.size());
        for (Entry<String, AttributeExtractor> e : extractors.entrySet()) {
            wrappedExtractors.put(e.getKey(), new TransactionAwareAttributeExtractor(copyStrategy, e.getValue()));
        }
        underlyingStore.setAttributeExtractors(wrappedExtractors);
    }

}
