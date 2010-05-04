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

package net.sf.ehcache.transaction.xa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;
import net.sf.ehcache.transaction.xa.XARequest.RequestType;

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

    private static final int LOCK_TIMEOUT = 15000;
    private static final int DEFAULT_TX_TIMEOUT = 60;
    private static final int MILLISEC_PER_SECOND = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheXAResourceImpl.class.getName());

    private final String             cacheName;
    private final XARequestProcessor processor;
    private final EhcacheXAStore     ehcacheXAStore;
    private final Store              store;
    private final Store              oldVersionStore;
    private final TransactionManager txnManager;
    private final Ehcache            cache;
    private final Set<Xid>           recoverySet     = new HashSet<Xid>();
   

    private       volatile int                transactionTimeout = DEFAULT_TX_TIMEOUT;
    private       volatile Xid                currentXid;

    private List<TwoPcExecutionListener> twoPcExecutionListeners = new ArrayList<TwoPcExecutionListener>();


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
        String cacheMgrName;
        if (cache.getCacheManager() == null || !cache.getCacheManager().isNamed()) {
          cacheMgrName = CacheManager.DEFAULT_NAME;
        } else {
          cacheMgrName = cache.getCacheManager().getName();
        }
        this.cacheName          = cache.getName() + "@" + cacheMgrName + ".cacheManager";
        this.store              = ehcacheXAStore.getUnderlyingStore();
        this.txnManager         = txnManager;
        this.ehcacheXAStore     = ehcacheXAStore;
        this.oldVersionStore    = ehcacheXAStore.getOldVersionStore();
        this.cache              = cache;
        this.processor          = new TransactionXARequestProcessor(this);        
    }

    /**
     * {@inheritDoc}
     */
    public void addTwoPcExecutionListener(TwoPcExecutionListener listener) {
        twoPcExecutionListeners.add(listener);
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
        //todo: check flags, do not allow 2X start()

        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.start called for Txn with flag: " + prettyPrintFlags(flags)  + " and id: " + xid);   
        }
        currentXid = xid;
    }

   
    /**
     * {@inheritDoc}
     */
    public void end(final Xid xid, final int flags) throws XAException {
        //todo: check flags, throw an exception if start() was not called

        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.end called for Txn with flag: " + prettyPrintFlags(flags)  + " and id: " + xid);   
        }
        if (isFlagSet(flags, TMFAIL)) {
            if (ehcacheXAStore.isPrepared(xid)) {
                // todo: throw protocol violation!
                markContextAsRolledbackIfRecovered(xid);
            } else {
                ehcacheXAStore.removeData(xid);
            }
        }
        currentXid = null;
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(final Xid xid) throws XAException {
        //todo: check XID, throw an exception if end() not called or if start()/end() never called

        for (TwoPcExecutionListener twoPcExecutionListener : twoPcExecutionListeners) {
            try {
                twoPcExecutionListener.beforePrepare(this);
            } catch (RuntimeException ex) {
                LOG.warn("exception thrown before prepare in TwoPcExecutionListener " + twoPcExecutionListener, ex);
            }
        }

        return this.processor.process(new XARequest(RequestType.PREPARE, getCurrentTransaction(), xid, XAResource.TMNOFLAGS));
    }

    /**
     * Called by {@link XARequestProcessor}
     * @param xid the Xid of the transaction to prepare
     * @return XA_OK, or XA_RDONLY if no write operations prepared
     * @throws XAException if an integrity issue occurs
     */
    int prepareInternal(final Xid xid) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.prepare called for Txn with id: " + xid);
        }

        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);
        CacheLockProvider storeLockProvider = (CacheLockProvider) store.getInternalContext();
        CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider) oldVersionStore.getInternalContext();

        // Lock all keys in both stores
        Object[] updatedKeys = context.getUpdatedKeys();
        tryLockingKeysRequiredForPrepare(storeLockProvider, oldVersionStoreLockProvider, updatedKeys);


        try {
            // validate we will be able to commit
            validateCommands(context, xid);
        } catch (XAException e) {
            // If something goes wrong
            cleanUpFailure(xid, storeLockProvider, oldVersionStoreLockProvider, updatedKeys);
            throw e;
        }

        PreparedContext preparedContext = ehcacheXAStore.createPreparedContext();
        // Copy old versions in front-accessed store todo crappy coupling going on here!
        for (VersionAwareCommand command : context.getCommands()) {
            // as we only have write commands to specific keys here... but this could change, and we'd try to unlock non-locked keys
            Object key = command.getKey();
            if (key != null) {
                oldVersionStore.put(store.get(command.getKey()));
                preparedContext.addCommand(command);
            }
        }
        oldVersionStoreLockProvider.unlockWriteLockForAllKeys(updatedKeys);

        // Execute write command within the real underlying store
        boolean writes = false;
        Set<Object> keysAlreadyProcessed = new HashSet<Object>(updatedKeys.length);
        try {
            for (VersionAwareCommand command : context.getCommands()) {
                writes = command.execute(store) || writes;
                keysAlreadyProcessed.add(command.getKey());
            }
        } catch (IllegalStateException e) {
            switchValuesBack(keysAlreadyProcessed);
            cleanUpFailure(xid, storeLockProvider, null, updatedKeys);
            throw new EhcacheXAException("Couldn't execute command on store!", XAException.XA_RBINTEGRITY);
        }

        return determinePrepareReturnCode(xid, updatedKeys, preparedContext, writes);
    }

    private int determinePrepareReturnCode(Xid xid, Object[] updatedKeys, PreparedContext preparedContext, boolean writes)
            throws EhcacheXAException {
        if (writes) {
            ehcacheXAStore.prepare(xid, preparedContext);
            return XA_OK;
        } else {
            if (updatedKeys.length > 0) {
                LOG.warn(updatedKeys.length + " updated keys, but nothing got changed?!");
                ehcacheXAStore.prepare(xid, preparedContext);
                return XA_OK;
            }

            ehcacheXAStore.removeData(xid);
            fireAfterCommitOrRollback();
            return XA_RDONLY;
        }
    }

    private void switchValuesBack(final Object... keysAlreadyProcessed) {
        for (Object key : keysAlreadyProcessed) {
            if (key != null) {
                Element element = oldVersionStore.remove(key);
                if (element != null) {
                    store.put(element);
                } else {
                    store.remove(key);
                }
            }
        }
    }

    private void tryLockingKeysRequiredForPrepare(CacheLockProvider storeLockProvider, CacheLockProvider oldVersionStoreLockProvider,
                                                  Object[] updatedKeys) throws EhcacheXAException {
        // Lock here first, so that threads wait on every get for this to be released?
        try {
            oldVersionStoreLockProvider.getAndWriteLockAllSyncForKeys(LOCK_TIMEOUT, updatedKeys);
        } catch (TimeoutException ex) {
            throw new EhcacheXAException("could not lock all required entries in oldVersionStore", XAException.XA_RBDEADLOCK, ex);
        }
        // Then lock here, so that normally no one is staying in line for the lock
        try {
            storeLockProvider.getAndWriteLockAllSyncForKeys(LOCK_TIMEOUT, updatedKeys);
        } catch (TimeoutException ex) {
            oldVersionStoreLockProvider.unlockWriteLockForAllKeys(updatedKeys);
            throw new EhcacheXAException("could not lock all required entries in storeLockProvider", XAException.XA_RBDEADLOCK, ex);
        }
    }

    private void cleanUpFailure(final Xid xid, final CacheLockProvider storeLockProvider,
                                final CacheLockProvider oldVersionStoreLockProvider,
                                final Object[] updatedKeys) {
        Set<Object> keys = new HashSet<Object>(Arrays.asList(updatedKeys));
        for (Object updatedKey : keys) {
            // Decrease counters, readonly operation, as we are "rolling back"
            ehcacheXAStore.checkin(updatedKey, xid, true);
        }
        storeLockProvider.unlockWriteLockForAllKeys(updatedKeys);
        if (oldVersionStoreLockProvider != null) {
            oldVersionStoreLockProvider.unlockWriteLockForAllKeys(updatedKeys);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void forget(final Xid xid) throws XAException {
        // todo: make sure the XID is tested for existence
        this.processor.process(new XARequest(RequestType.FORGET, getCurrentTransaction(), xid));
    }

    /**
     * Called by {@link XARequestProcessor}
     * @param xid The Xid of the transaction to forget
     * @throws XAException if transaction was prepared already and couldn't be marked for rollback
     */
    void forgetInternal(final Xid xid) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.forget called for Txn with id: " + xid);   
        }
       
        if (ehcacheXAStore.isPrepared(xid)) {
            // todo: throw protocol violation!
            markContextAsRolledbackIfRecovered(xid);
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
            markContextAsRolledbackIfRecovered(preparedXid);
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
        //todo: check XID, throw an exception if end() not called or if start()/end() never called
        //todo: check onePhase: cannot be true if prepare was called, cannot be false if it wasn't

        Transaction txn = getCurrentTransaction();
        this.processor.process(new XARequest(RequestType.COMMIT, txn , xid, XAResource.TMNOFLAGS, onePhase));
    }

    /**
     * Called by {@link XARequestProcessor}
     * @param xid the Xid of the transaction to commit
     * @param onePhase flag whether this is a onePhase commit (i.e. Xid was not prepared)
     * @throws XAException Should some error happen, like if the transaction was already heuristically rolled back
     */
    void commitInternal(final Xid xid, final boolean onePhase) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.commit called for Txn with phase: " + (onePhase ? "onePhase" : "twoPhase") +  " and id: " + xid);
        }
        if (onePhase) {
            if (ehcacheXAStore.getPreparedContext(xid) == null) {
                onePhaseCommit(xid);
            } else {
                throw new EhcacheXAException(xid + " has been prepared! Cannot operate one phased commit!", XAException.XAER_PROTO);
            }
        } else {
            PreparedContext context = ehcacheXAStore.getPreparedContext(xid);

            CacheLockProvider oldVersionStoreProvider = ((CacheLockProvider) oldVersionStore.getInternalContext());
            CacheLockProvider storeProvider = ((CacheLockProvider) store.getInternalContext());
            Object[] keys = context.getUpdatedKeys();
            if (!context.isCommitted() && !context.isRolledBack()) {
                context.setCommitted(true);
                oldVersionStoreProvider.getAndWriteLockAllSyncForKeys(keys);
                for (PreparedCommand command : context.getPreparedCommands()) {
                    Object key = command.getKey();
                    if (key != null) {
                        potentiallyCheckin(context, command, xid);
                        oldVersionStore.remove(key);
                    }
                }

                storeProvider.unlockWriteLockForAllKeys(keys);
                oldVersionStoreProvider.unlockWriteLockForAllKeys(keys);

            } else if (context.isRolledBack()) {
                throw new EhcacheXAException("Transaction " + xid + " has been heuristically rolled back", XAException.XA_HEURRB);
            }
        }

        ehcacheXAStore.removeData(xid);

        fireAfterCommitOrRollback();
    }

    private boolean isLastCommandForKey(PreparedContext context, PreparedCommand command) {
        List<PreparedCommand> commands = context.getPreparedCommands();
        ListIterator<PreparedCommand> listIterator = commands.listIterator(commands.lastIndexOf(command) + 1);
        while (listIterator.hasNext()) {
            if (listIterator.next().getKey().equals(command.getKey())) {
                return false;
            }
        }

        return true;
    }

    private void potentiallyCheckin(final PreparedContext context, final PreparedCommand command, final Xid xid) {
        if (isLastCommandForKey(context, command)) {
            ehcacheXAStore.checkin(command.getKey(), xid, !command.isWriteCommand());
        }
    }


    private void fireAfterCommitOrRollback() {
        for (TwoPcExecutionListener twoPcExecutionListener : twoPcExecutionListeners) {
            try {
                twoPcExecutionListener.afterCommitOrRollback(this);
            } catch (RuntimeException ex) {
                LOG.warn("exception thrown after commit or rollback in TwoPcExecutionListener " + twoPcExecutionListener, ex);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rollback(final Xid xid) throws XAException {
        //todo: check XID, throw an exception if end() not called or if start()/end() never called
       this.processor.process(new XARequest(RequestType.ROLLBACK, getCurrentTransaction(), xid));
    }

    /**
     * Called by {@link XARequestProcessor}
     * @param xid the Xid of the Transaction to rollback
     * @throws XAException should the Transaction have been heuristically commited
     */
    void rollbackInternal(final Xid xid) throws XAException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("xaResource.rollback called for Txn with id: " + xid);
        }

        PreparedContext context = ehcacheXAStore.getPreparedContext(xid);
        if (ehcacheXAStore.isPrepared(xid) && !context.isRolledBack() && !context.isCommitted()) {
            context.setRolledBack(true);
            CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
            CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

            Object[] updatedKeys = context.getUpdatedKeys();
            oldVersionStoreLockProvider.getAndWriteLockAllSyncForKeys(updatedKeys);

            try {
                for (Object updatedKey : updatedKeys) {
                    switchValuesBack(updatedKey);
                    storeLockProvider.getSyncForKey(updatedKey).unlock(LockType.WRITE);
                }
            } finally {
                oldVersionStoreLockProvider.unlockWriteLockForAllKeys(updatedKeys);
            }
        } else if (context != null && context.isCommitted()) {
            throw new EhcacheXAException("Transaction " + xid + " has been heuristically committed", XAException.XA_HEURCOM);
        }
        ehcacheXAStore.removeData(xid);

        fireAfterCommitOrRollback();
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
    public boolean setTransactionTimeout(final int timeout) throws XAException {
        //todo: is timeout supported? If not, false should be returned
        if (timeout < 0) {
            throw new EhcacheXAException("time out has to be > 0, but was " + timeout, XAException.XAER_INVAL);
        }
        //this.transactionTimeout = timeout == 0 ? DEFAULT_TX_TIMEOUT : timeout;
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
    public TransactionContext createTransactionContext() throws SystemException, RollbackException {
        //todo: TX enlistment and TX registerSynchronization() should be moved to XATransactionalStore
        Transaction transaction = txnManager.getTransaction();

        transaction.enlistResource(this);

        if (cache.getWriterManager() != null) {
            try {
                transaction.registerSynchronization(new CacheWriterManagerSynchronization(currentXid));
            } catch (RollbackException e) {
                // Safely ignore this
            } catch (SystemException e) {
                throw new CacheException("Couldn't register CacheWriter's Synchronization with the JTA Transaction : "
                        + e.getMessage(), e);
            }
        }

        // currentXid is set by a call to start() which itself is called by transaction.enlistResource(this)
        // this is quite confusing, there should be a way to simplify all that.
        if (currentXid == null) {
            throw new CacheException("enlistment of XAResource of cache named '" + getCacheName() +
                    "' did not end up calling XAResource.start()"); 
        }
        return ehcacheXAStore.createTransactionContext(currentXid);
    }

    /**
     * {@inheritDoc}
     */
    public TransactionContext getCurrentTransactionContext() {
        return currentXid != null ? ehcacheXAStore.getTransactionContext(currentXid) : null;
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
     * @return the current transaction
     * @throws EhcacheXAException If no transaction is alive
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
     * @param xid the Xid of the transaction to commit
     * @throws XAException Should a MVCC validation error happen
     */
    private void onePhaseCommit(final Xid xid) throws XAException {
        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);
        CacheLockProvider storeLockProvider = (CacheLockProvider) store.getInternalContext();

        Object[] keys = context.getUpdatedKeys();

        // Lock all keys in real store
        try {
            storeLockProvider.getAndWriteLockAllSyncForKeys(transactionTimeout * MILLISEC_PER_SECOND, keys);
        } catch (TimeoutException ex) {
            throw new EhcacheXAException("could not lock all required entries in storeLockProvider", XAException.XA_RBDEADLOCK, ex);
        }

        try {
            validateCommands(context, xid);

            LOG.debug("One phase commit called for Txn with id: {}", xid);

            // Execute write command within the real underlying store
            List<VersionAwareCommand> commands = context.getCommands();
            for (VersionAwareCommand command : commands) {
                command.execute(store);
                Object key = command.getKey();
                if (key != null) {
                    potentiallyCheckin(context, command, xid);
                }
            }
        } finally {
            storeLockProvider.unlockWriteLockForAllKeys(keys);
        }
    }

    private void potentiallyCheckin(final TransactionContext context, final VersionAwareCommand command, final Xid xid) {
        List<VersionAwareCommand> commands = context.getCommands();
        ListIterator<VersionAwareCommand> listIterator = commands.listIterator(commands.lastIndexOf(command) + 1);
        boolean lastCommandForKey = true;
        while (listIterator.hasNext()) {
            if (listIterator.next().getKey().equals(command.getKey())) {
                lastCommandForKey = false;
                break;
            }
        }
        if (lastCommandForKey) {
            ehcacheXAStore.checkin(command.getKey(), xid, !command.isWriteCommand());
        }
    }

    /**
     * Check if commands are still valid for prepare/commit for given Xid
     * @param context the context containing the commands to validate against the MVCC optimistic locking mechanism
     * @param xid the Xid of the Transaction this context is bound to
     * @throws XAException If a validation error happens
     */
    private void validateCommands(TransactionContext context, Xid xid) throws XAException {
        for (VersionAwareCommand command : context.getCommands()) {
            if (command.isVersionAware()) {
                if (!ehcacheXAStore.isValid(command, xid)) {
                    throw new EhcacheXAException("Element for key <" + command.getKey() + "> has changed since it was " +
                            command.getCommandName() + " in the cache and the transaction committed (currentVersion: " +
                            command.getVersion() + ")", XAException.XA_RBINTEGRITY);
                }
            }
        }
    }

    /**
     * Marks the context of a prepared Xid as rolled back if the context has been recovered after a crash.
     * @param preparedXid the Xid of the Transaction to be marked for rollback
     * @throws EhcacheXAException Should the operation be cancelled while trying to lock keys
     */
    private void markContextAsRolledbackIfRecovered(final Xid preparedXid) throws EhcacheXAException {
        PreparedContext context = ehcacheXAStore.getPreparedContext(preparedXid);
        if (context == null) {
            return;
        }

        Object[] updatedKeys = context.getUpdatedKeys();
        if (updatedKeys.length > 0) {
            Object someKey = updatedKeys[0];
            Sync syncForKey = ((CacheLockProvider) store.getInternalContext()).getSyncForKey(someKey);
            boolean readLocked;
            try {
                readLocked = syncForKey.tryLock(LockType.READ, 1);
            } catch (InterruptedException e) {
                throw new EhcacheXAException("Interrupted testing for Xid's status: " + preparedXid, XAException.XAER_RMFAIL);
            }
            if (readLocked) {
                try {
                    if (!context.isCommitted() && !context.isRolledBack()) {
                        // Transaction was recovered! Clean oldVersionStore should we still have stuff lying around in there
                        for (Object updatedKey : updatedKeys) {
                            oldVersionStore.remove(updatedKey);
                        }
                        context.setRolledBack(true);
                    }
                } finally {
                    syncForKey.unlock(LockType.READ);
                }
            }
        }
    }
    
    /**
     * Checks whether a flag is set, based on a mask, in flags
     * @param flags the flags
     * @param mask the mask
     * @return true if set, false otherwise
     */
    private boolean isFlagSet(int flags, int mask) {
        return mask == (flags & mask);
    }

    /**
     * Print flagStr if the mask is set in flags
     * @param flags the current flags set
     * @param mask the mask to check for
     * @param flagStr the String to return if flag is set
     * @return flagStr if set or an empty string otherwise
     */
    private String printFlag(int flags, int mask, String flagStr) {
        return isFlagSet(flags, mask) ? flagStr : "";
    }

    /**
     * Return the string version of the flag
     * @param flags flags to print
     * @return the string representation of flags set or TMNOFLAGS
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

        private Xid currentXid;

        public CacheWriterManagerSynchronization(Xid currentXid) {
            this.currentXid = currentXid;
        }

        /**
         * {@inheritDoc}
         */
        public void beforeCompletion() {
            TransactionContext context = ehcacheXAStore.getTransactionContext(currentXid);
            for (VersionAwareCommand versionAwareCommand : context.getCommands()) {
                versionAwareCommand.execute(cache.getWriterManager());
            }
        }

        /**
         * {@inheritDoc}
         */
        public void afterCompletion(final int status) {
            // we don't care
        }
    }

}
