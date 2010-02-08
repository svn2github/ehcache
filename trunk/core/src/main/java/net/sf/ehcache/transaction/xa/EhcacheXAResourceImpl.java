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

package net.sf.ehcache.transaction.xa;

import java.util.Set;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for EhcacheXAResource.
 * It encapsulates the store to be accessed in a transactional way, the TransactionManager
 * and an {@link net.sf.ehcache.transaction.xa.EhcacheXAStore EhcacheXAStore}, where it'll save transaction data during
 * the two-phase commit process, and between suspend/resume transaction cycles.<p>
 * It'll also associate {@link javax.transaction.Transaction Transaction} instances with their {@link javax.transaction.xa.Xid Xid}
 *
 * @author Nabib El-Rahman
 * @author Alex Snaps
 */
public class EhcacheXAResourceImpl implements EhcacheXAResource {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheXAResourceImpl.class.getName());
    
    private final    String             cacheName;
    private final    EhcacheXAStore     ehcacheXAStore;
    
    private volatile int                transactionTimeout;
    private volatile Store              store;
    private volatile Store              oldVersionStore;
    private volatile TransactionManager txnManager;

    /**
     * Constructor
     * @param cacheName The cache name of the Cache wrapped
     * @param txnManager the TransactionManager associated with this XAResource
     * @param ehcacheXAStore The EhcacheXAStore for this cache
     */
    public EhcacheXAResourceImpl(String cacheName, TransactionManager txnManager, EhcacheXAStore ehcacheXAStore) {
        this.cacheName = cacheName;
        this.store = ehcacheXAStore.getUnderlyingStore();
        this.txnManager = txnManager;
        this.ehcacheXAStore = ehcacheXAStore;
        this.oldVersionStore = ehcacheXAStore.getOldVersionStore();
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * {@inheritDoc}
     */
    public void start(final Xid xid, final int flags) throws XAException {
        LOG.debug("Start called for Txn with id: " + xid);
      
 
        // todo we should probably track state properly here...
        Transaction tx;
        try {
            tx = txnManager.getTransaction();
        } catch (SystemException e) {
            throw new EhcacheXAException("Couldn't get to current Transaction: " + e.getMessage(), e.errorCode, e);
        }
        if (tx == null) {
            throw new EhcacheXAException("Couldn't get to current Transaction ", XAException.XAER_OUTSIDE);
        }
        Xid prevXid = ehcacheXAStore.storeXid2Transaction(xid, tx);
        if (prevXid != null && !prevXid.equals(xid)) {
            throw new EhcacheXAException("Duplicated XID: " + xid, XAException.XAER_DUPID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commit(final Xid xid, final boolean onePhase) throws XAException {
        if (onePhase) {
            // TODO if XA_READONLY, we can optimize that!
            prepare(xid);
        }
        LOG.debug("{} phase commit called for Txn with id: {}", (onePhase ? "One" : "Two"), xid);
        PreparedContext context = ehcacheXAStore.getPreparedContext(xid);
        
        Sync[] syncForKeys = ((CacheLockProvider)oldVersionStore.getInternalContext())
            .getAndWriteLockAllSyncForKeys(context.getUpdatedKeys().toArray());
        for (VersionAwareCommand command : context.getCommands()) {
            Object key = command.getKey();
            if (key != null) {
                ehcacheXAStore.checkin(key, xid, command.isWriteCommand());
                oldVersionStore.remove(key);
                ((CacheLockProvider)store.getInternalContext()).getSyncForKey(key).unlock(LockType.WRITE);
            }
        }

        for (Sync syncForKey : syncForKeys) {
            syncForKey.unlock(LockType.WRITE);
        }

        ehcacheXAStore.removeData(xid);
    }

    /**
     * {@inheritDoc}
     */
    public void end(final Xid xid, final int flags) throws XAException {
        LOG.debug("End called for Txn with id: {}", xid);
    }

    /**
     * {@inheritDoc}
     */
    public void forget(final Xid xid) throws XAException {
        LOG.debug("Forget called for Txn with id: {}", xid);
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(final Xid xid) throws XAException {
        LOG.debug("Prepare called for Txn with id: {}", xid);

        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);
        CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
        CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

        // First dirty bulk check?
        validateCommands(context, xid);
        PreparedContext preparedContext = ehcacheXAStore.createPreparedContext();
        // Copy old versions in front-accessed store
        for (VersionAwareCommand command : context.getCommands()) {
            Object key = command.getKey();
            if (key != null) {
                Sync syncForKey = oldVersionStoreLockProvider.getSyncForKey(key);
                syncForKey.lock(LockType.WRITE);
                try {
                    if (!ehcacheXAStore.isValid(command, xid)) {
                        for (VersionAwareCommand addedCommand : preparedContext.getCommands()) {
                            oldVersionStore.remove(addedCommand.getKey());
                        }
                        throw new EhcacheXAException("Invalid version for element: " + command.getKey(),
                            XAException.XA_RBINTEGRITY);
                    }
                   
                    preparedContext.addCommand(command);
                    oldVersionStore.put(store.get(key));
                } finally {
                    syncForKey.unlock(LockType.WRITE);
                }
            }
        }

        Set<Object> keys = context.getUpdatedKeys();
        
        // Lock all keys in real store
        Sync[] syncForKeys = storeLockProvider.getAndWriteLockAllSyncForKeys(keys.toArray());

        LOG.debug("Locked {} syncs for {} keys", syncForKeys == null ? 0 : syncForKeys.length, keys.size());

        // Execute write command within the real underlying store
        boolean writes = false;
        for (VersionAwareCommand command : context.getCommands()) {
            writes = command.execute(store) || writes;
        }

        ehcacheXAStore.prepared(xid, preparedContext);

        return writes ? XA_OK : XA_RDONLY;
    }

    private void validateCommands(TransactionContext context, Xid xid) throws XAException {
        for (VersionAwareCommand command : context.getCommands()) {
            if (command.isVersionAware()) {
                if (!ehcacheXAStore.isValid(command, xid)) {
                    throw new EhcacheXAException("Invalid version for element: " + command.getKey(), XAException.XA_RBINTEGRITY);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Xid[] recover(final int i) throws XAException {
        return ehcacheXAStore.getPreparedXids();
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(final Xid xid) throws XAException {
        LOG.debug("Rollback called for Txn with id: {}", xid);

        PreparedContext context = ehcacheXAStore.getPreparedContext(xid);
        if (ehcacheXAStore.isPrepared(xid)) {
            CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
            CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

            for (VersionAwareCommand command : context.getCommands()) {
                Object key = command.getKey();
                if (key != null) {
                    Sync syncForKey = oldVersionStoreLockProvider.getSyncForKey(key);
                    syncForKey.lock(LockType.WRITE);
                    Element element = null;
                    try {
                        element = oldVersionStore.remove(key);
                        if (element != null) {
                            store.put(element);
                        } else {
                            LOG.error("No element found in oldVersionStore for key '{}'", key);
                        }
                    } finally {
                        syncForKey.unlock(LockType.WRITE);
                        if (element != null) {
                            storeLockProvider.getSyncForKey(key).unlock(LockType.WRITE);
                        }
                    }
                }
            }
        }
        ehcacheXAStore.removeData(xid);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSameRM(final XAResource xaResource) throws XAException {
        return this == xaResource;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(final int i) throws XAException {
        this.transactionTimeout = i;
        // TODO Figure out what to return here, it should be set to true of
        // setting the transaction timeout was successful.
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getTransactionTimeout() throws XAException {
        return this.transactionTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public Store getStore() {
        return store;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        EhcacheXAResource resource2 = (EhcacheXAResource) obj;
        if (cacheName.equals(resource2.getCacheName())) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return cacheName.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public TransactionContext getOrCreateTransactionContext() throws SystemException, RollbackException {
        Transaction transaction = txnManager.getTransaction();
        if (transaction == null) {
            throw new CacheException("Cache " + cacheName + " can only be accessed within a JTA Transaction!");
        }

        if (transaction.getStatus() != Status.STATUS_ACTIVE) {
            throw new CacheException("Transaction not active!");
        }

        TransactionContext context =  ehcacheXAStore.getTransactionContext(transaction);
        if (context == null) {
            transaction.enlistResource(this);
            context = ehcacheXAStore.createTransactionContext(transaction);
        }
        return context;
    }

    /**
     * {@inheritDoc}
     */
    public Element get(final Object key) {
        Element element = oldVersionStore.get(key);
        if (element == null) {
            element = store.get(key);
        }
        return element;
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(final Object key) {
        Element element = oldVersionStore.getQuiet(key);
        if (element == null) {
            element = store.getQuiet(key);
        }
        return element;
    }
}
