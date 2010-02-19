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

import java.util.HashSet;
import java.util.Set;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;
import net.sf.ehcache.transaction.xa.XARequest.RequestType;
import net.sf.ehcache.writer.CacheWriterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for EhcacheXAResource.
 * It encapsulates the store to be accessed in a transactional way, the TransactionManager
 * and an {@link net.sf.ehcache.transaction.xa.EhcacheXAStore EhcacheXAStore}, where it'll save transaction data during
 * the two-phase commit process, and between suspend/resume transaction cycles.
 * <p>
 * It'll also associate {@link javax.transaction.Transaction Transaction} instances with their {@link javax.transaction.xa.Xid Xid}
 * 
 * @author Nabib El-Rahman
 * @author Alex Snaps
 */
public class EhcacheXAResourceImpl implements EhcacheXAResource {

    private static final int DEFAULT_TIMEOUT = 60;

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheXAResourceImpl.class.getName());

    private final String             cacheName;
    private final XARequestProcessor processor;
    private final EhcacheXAStore     ehcacheXAStore;
    private final Store              store;
    private final Store              oldVersionStore;
    private final TransactionManager txnManager;
    private final CacheWriterManager cacheWriterManager;
    private final ThreadLocal<Xid>   currentXid      = new ThreadLocal<Xid>();
    private final Set<Xid>           recoverySet     = new HashSet<Xid>();
   

    private       volatile int                transactionTimeout = DEFAULT_TIMEOUT;

    /**
     * Constructor
     * 
     * @param cache
     *            The cache name of the Cache wrapped    
     * @param txnManager
     *            the TransactionManager associated with this XAResource
     * @param ehcacheXAStore
     *            The EhcacheXAStore for this cache
     */
    public EhcacheXAResourceImpl(Ehcache cache, TransactionManager txnManager, EhcacheXAStore ehcacheXAStore) {
        this.cacheName          = cache.getName();
        this.store              = ehcacheXAStore.getUnderlyingStore();
        this.txnManager         = txnManager;
        this.ehcacheXAStore     = ehcacheXAStore;
        this.oldVersionStore    = ehcacheXAStore.getOldVersionStore();
        this.cacheWriterManager = cache.getWriterManager();
        this.processor          = new TransactionXARequestProcessor(this);        
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.start called for Txn with flag: " + prettyPrintFlags(flags)  + " and id: " + xid);   
        }
        if (!isFlagSet(flags, TMRESUME) && !isFlagSet(flags, TMJOIN)) {
            Transaction tx = getCurrentTransaction();
            if (tx == null) {
                throw new EhcacheXAException("Couldn't get to current Transaction ", XAException.XAER_OUTSIDE);
            }
            try {  
                if (cacheWriterManager != null) {
                    try {
                        tx.registerSynchronization(new CacheWriterManagerSynchronization());
                    } catch (RollbackException e) {
                        // Safely ignore this
                    }
                }
            } catch (SystemException e) {
                throw new EhcacheXAException("Couldn't get to current Transaction: " + e.getMessage(), e.errorCode, e);
            }   
            Xid prevXid = currentXid.get();
            
            if (prevXid != null && !prevXid.equals(xid)) {
                throw new EhcacheXAException("Duplicated XID: " + xid, XAException.XAER_DUPID);
            }
        }
        currentXid.set(xid);
    }

   
    /**
     * {@inheritDoc}
     */
    public void end(final Xid xid, final int flags) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.end called for Txn with flag: " + prettyPrintFlags(flags)  + " and id: " + xid);   
        }
        if (isFlagSet(flags, TMFAIL)) {
            if (ehcacheXAStore.isPrepared(xid)) {
                markContextForRollback(xid);
            } else {
                ehcacheXAStore.removeData(xid);
            }
        }
        currentXid.remove();
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(final Xid xid) throws XAException {
        return this.processor.process(new XARequest(RequestType.PREPARE, getCurrentTransaction(), xid, XAResource.TMNOFLAGS));
    }

    /**
     * Called by {@link XARequestProcessor}
     */
    int prepareInternal(final Xid xid) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.prepare called for Txn with id: " + xid);   
        }

        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);
        CacheLockProvider storeLockProvider = (CacheLockProvider) store.getInternalContext();
        CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider) oldVersionStore.getInternalContext();

        validateCommands(context, xid);
        
        PreparedContext preparedContext = ehcacheXAStore.createPreparedContext();
        // Copy old versions in front-accessed store
        for (VersionAwareCommand command : context.getCommands()) {
            Object key = command.getKey();
            if (key != null) {
                prepareCommand(xid, oldVersionStoreLockProvider, preparedContext, command);
            }
        }

        // Lock all keys in real store
        Sync[] syncForKeys = storeLockProvider.getAndWriteLockAllSyncForKeys(preparedContext.getUpdatedKeys().toArray());

         // Execute write command within the real underlying store
        boolean writes = false;
        for (VersionAwareCommand command : context.getCommands()) {
            writes = command.execute(store) || writes;
        }

        ehcacheXAStore.prepare(xid, preparedContext);

        return writes ? XA_OK : XA_RDONLY;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void forget(final Xid xid) throws XAException {
        this.processor.process(new XARequest(RequestType.FORGET, getCurrentTransaction(), xid));
    }
    
    /**
     * Called by {@link XARequestProcessor}
     */
    void forgetInternal(final Xid xid) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.forget called for Txn with id: " + xid);   
        }
       
        if (ehcacheXAStore.isPrepared(xid)) {
            markContextForRollback(xid);
        } else {
            ehcacheXAStore.removeData(xid);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Xid[] recover(final int flags) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.recover called for Txn with flag: " + prettyPrintFlags(flags));   
        }

        Set<Xid> xids = new HashSet<Xid>();

        if (isFlagSet(flags, TMSTARTRSCAN)) {
            recoverySet.clear();
        }

        Xid[] allPreparedXids = ehcacheXAStore.getPreparedXids();
        for (Xid preparedXid : allPreparedXids) {
            if (!recoverySet.contains(preparedXid)) {
                xids.add(preparedXid);
            }
            recoverySet.add(preparedXid);
        }

        for (Xid preparedXid : xids) {
            markContextForRollback(preparedXid);
        }

        Xid[] toRecover = xids.toArray(new Xid[xids.size()]);

        if (isFlagSet(flags, TMENDRSCAN)) {
            recoverySet.clear();
        }

        return toRecover;
    }

    /**
     * {@inheritDoc}
     */
    public void commit(final Xid xid, final boolean onePhase) throws XAException {
        Transaction txn = getCurrentTransaction();
        this.processor.process(new XARequest(RequestType.COMMIT, txn , xid, XAResource.TMNOFLAGS, onePhase));
    }
    
    /**
     * Called by {@link XARequestProcessor}
     */
    void commitInternal(final Xid xid, final boolean onePhase) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.commit called for Txn with phase: " + (onePhase ? "onePhase" : "twoPhase") +  " and id: " + xid);   
        }
        if (onePhase) {
            onePhaseCommit(xid);
        } else {
            PreparedContext context = ehcacheXAStore.getPreparedContext(xid);

            CacheLockProvider storeProvider = ((CacheLockProvider) store.getInternalContext());
            CacheLockProvider oldVersionStoreProvider = ((CacheLockProvider) oldVersionStore.getInternalContext());
            Set<Object> preparedKeys = context.getUpdatedKeys();
            if (!context.isCommitted() && !context.isRolledBack()) {
                Sync[] syncForKeys = oldVersionStoreProvider.getAndWriteLockAllSyncForKeys(preparedKeys.toArray());
                for (PreparedCommand command : context.getPreparedCommands()) {
                    Object key = command.getKey();
                    if (key != null) {
                        ehcacheXAStore.checkin(key, xid, command.isWriteCommand());
                        oldVersionStore.remove(key);
                        
                    }
                }
                
                storeProvider.unlockWriteLockForAllKeys(preparedKeys.toArray());
                oldVersionStoreProvider.unlockWriteLockForAllKeys(preparedKeys.toArray());
                             
                context.setCommitted(true);
            } else if (context.isRolledBack()) {
                throw new EhcacheXAException("Transaction " + xid + " has been heuristically rolled back", XAException.XA_HEURRB);
            }
        }

        ehcacheXAStore.removeData(xid);
    }


    
    /**
     * {@inheritDoc}
     */
    public void rollback(final Xid xid) throws XAException {
       this.processor.process(new XARequest(RequestType.ROLLBACK, getCurrentTransaction(), xid));
    }
    
    /**
     * Called by {@link XARequestProcessor}
     */
    void rollbackInternal(final Xid xid) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.rollback called for Txn with id: " + xid);   
        }

        PreparedContext context = ehcacheXAStore.getPreparedContext(xid);
        if (ehcacheXAStore.isPrepared(xid) && !context.isRolledBack() && !context.isCommitted()) {
            context.setRolledBack(true);
            CacheLockProvider storeLockProvider = (CacheLockProvider) store.getInternalContext();
            CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider) oldVersionStore.getInternalContext();

            for (PreparedCommand command : context.getPreparedCommands()) {
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
        } else if (context != null && context.isCommitted()) {
            throw new EhcacheXAException("Transaction " + xid + " has been heuristically committed", XAException.XA_HEURRB);
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
        if (i < 0) {
            throw new EhcacheXAException("time out has to be > 0, but was " + i, XAException.XAER_INVAL);
        }
        this.transactionTimeout = i == 0 ? DEFAULT_TIMEOUT : i;
        return true;
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
    public TransactionContext getOrCreateTransactionContext() throws SystemException, RollbackException {
        Transaction transaction = txnManager.getTransaction();
        if (transaction == null) {
            throw new CacheException("Cache " + cacheName + " can only be accessed within a JTA Transaction!");
        }

        if (transaction.getStatus() != Status.STATUS_ACTIVE) {
            throw new CacheException("Transaction not active!");
        }

        TransactionContext context = ehcacheXAStore.getTransactionContext(currentXid.get());
        if (context == null) {
            transaction.enlistResource(this);
            context = ehcacheXAStore.createTransactionContext(currentXid.get());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EhcacheXAResource) {
            EhcacheXAResource resource2 = (EhcacheXAResource) obj;
            return cacheName.equals(resource2.getCacheName());
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
     * Get the current Transaction from the thread context
     * @return
     * @throws EhcacheXAException
     */
    private Transaction getCurrentTransaction() throws EhcacheXAException {
        Transaction txn;
        try {
            txn = txnManager.getTransaction();
        } catch (SystemException e) {
            throw new EhcacheXAException("Couldn't get to current Transaction: " + e.getMessage(), e.errorCode, e);
        }
        return txn;
    }
    /**
     * Optimized one-phase commit, assumed prepare is never called.
     * @param xid
     * @throws XAException
     */
    private void onePhaseCommit(final Xid xid) throws XAException {
        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);
        CacheLockProvider storeLockProvider = (CacheLockProvider) store.getInternalContext();

        // First dirty bulk check?
        validateCommands(context, xid);

        Set<Object> keys = context.getUpdatedKeys();

        // Lock all keys in real store
        Sync[] syncForKeys = storeLockProvider.getAndWriteLockAllSyncForKeys(keys.toArray());

        LOG.debug("{} phase commit called for Txn with id: {} One", xid);

        // Execute write command within the real underlying store
        boolean writes = false;
        for (VersionAwareCommand command : context.getCommands()) {
            writes = command.execute(store) || writes;
            Object key = command.getKey();
            if (key != null) {
                ehcacheXAStore.checkin(key, xid, command.isWriteCommand());
            }
        }

        for (Sync syncForKey : syncForKeys) {
            syncForKey.unlock(LockType.WRITE);
        }
    }
    
    /**
     * Check if commands are still valid for prepare/commit for given Xid
     * @param context
     * @param xid
     * @throws XAException
     */
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
     * 
     * @param xid
     * @param oldVersionStoreLockProvider
     * @param preparedContext
     * @param command
     * @param key
     * @throws EhcacheXAException
     */
    private void prepareCommand(final Xid xid, CacheLockProvider oldVersionStoreLockProvider, PreparedContext preparedContext,
            VersionAwareCommand command) throws EhcacheXAException {
        Sync syncForKey = oldVersionStoreLockProvider.getSyncForKey(command.getKey());
        syncForKey.lock(LockType.WRITE);
        try {
            if (!ehcacheXAStore.isValid(command, xid)) {
                for (PreparedCommand addedCommand : preparedContext.getPreparedCommands()) {
                    oldVersionStore.remove(addedCommand.getKey());
                }
                throw new EhcacheXAException("Invalid version for element: " + command.getKey(), XAException.XA_RBINTEGRITY);
            }
            oldVersionStore.put(store.get(command.getKey()));
            preparedContext.addCommand(command);          
        } finally {
            syncForKey.unlock(LockType.WRITE);
        }
    }
    
    /**
     * 
     * @param preparedXid
     * @throws EhcacheXAException
     */
    private void markContextForRollback(final Xid preparedXid) throws EhcacheXAException {
        PreparedContext context = ehcacheXAStore.getPreparedContext(preparedXid);
        Set<Object> updatedKeys = context.getUpdatedKeys();
        if (!updatedKeys.isEmpty()) {
            Object someKey = updatedKeys.iterator().next();
            Sync syncForKey = ((CacheLockProvider) store.getInternalContext()).getSyncForKey(someKey);
            boolean readLocked;
            try {
                readLocked = syncForKey.tryLock(LockType.READ, 1);
            } catch (InterruptedException e) {
                throw new EhcacheXAException("Interrupted testing for Xid's status: " + preparedXid, XAException.XAER_RMFAIL);
            }
            if (readLocked) {
                try {
                    // Transaction was rolled back! Clean oldVersionStore should we still have stuff lying around in there
                    for (Object updatedKey : updatedKeys) {
                        oldVersionStore.remove(updatedKey);
                    }
                    context.setRolledBack(true);
                    ehcacheXAStore.prepare(preparedXid, context);
                } finally {
                    syncForKey.unlock(LockType.READ);
                }
            }
        }
    }
    
    /**
     * 
     * @param flags
     * @param flag
     * @return
     */
    private boolean isFlagSet(int flags, int mask) {
        return mask == (flags & mask);
    }
    
    private String printFlag(int flags, int mask, String flagStr) {
        return isFlagSet(flags, mask) ? flagStr : "";
    }
    /**
     * Return the string version of the flag
     * @param flag
     * @return
     */
    private String prettyPrintFlags(int flags) {
        StringBuffer flagStrings = new StringBuffer();
        flagStrings.append(printFlag(flags, TMENDRSCAN, "TMENDRSCAN "));
        flagStrings.append(printFlag(flags, TMFAIL, "TMFAIL "));
        flagStrings.append(printFlag(flags, TMJOIN, "TMJOIN "));
        flagStrings.append(printFlag(flags, TMONEPHASE, "TMONEPHASE "));
        flagStrings.append(printFlag(flags, TMRESUME, "TMRESUME "));
        flagStrings.append(printFlag(flags, TMSTARTRSCAN, "TMSTARTRSCAN "));
        flagStrings.append(printFlag(flags, TMSUCCESS, "TMSUCCESS "));
        flagStrings.append(printFlag(flags, TMSUSPEND, "TMSUSPEND ")); 
        String flagStr = flagStrings.toString();
        return flagStr.equals("") ?  "TMNOFLAGS" : flagStr;
    }
    
    
    /**
     * Writes stuff to the CacheWriterManager just before the Transaction is ended for commit
     */
    private class CacheWriterManagerSynchronization implements Synchronization {

        /**
         * {@inheritDoc}
         */
        public void beforeCompletion() {
            try {
                TransactionContext context = getOrCreateTransactionContext();
                context.getRemovedKeys();
                for (VersionAwareCommand versionAwareCommand : context.getCommands()) {
                    versionAwareCommand.execute(cacheWriterManager);
                }
            } catch (SystemException e) {
                // this will cause the tx to be rolled back by the TransactionManager
                throw new CacheException("Error synching writer", e);
            } catch (RollbackException e) {
                // Ignore safely
            }
        }

        /**
         * {@inheritDoc}
         */
        public void afterCompletion(final int status) {
            // we don't care
        }
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return "EhcacheXAResourceImpl [ " + getCacheName() + " ] ";
    }
    
    
}
