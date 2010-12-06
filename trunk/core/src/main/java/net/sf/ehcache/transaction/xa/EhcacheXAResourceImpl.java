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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.xa.commands.Command;
import net.sf.ehcache.transaction.xa.processor.XARequestProcessor;
import net.sf.ehcache.transaction.xa.processor.XARequest;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * The EhcacheXAResource implementation
 *
 * @author Ludovic Orban
 */
public class EhcacheXAResourceImpl implements EhcacheXAResource {

    private final Ehcache cache;
    private final Store underlyingStore;
    private final TransactionIDFactory transactionIDFactory;
    private final TransactionManager txnManager;
    private final SoftLockFactory softLockFactory;
    private final ConcurrentMap<Xid, XATransactionContext> xidToContextMap = new ConcurrentHashMap<Xid, XATransactionContext>();
    private final XARequestProcessor processor;
    private volatile Xid currentXid;
    private volatile int transactionTimeout = DEFAULT_TRANSACTION_TIMEOUT;
    private final List<XAExecutionListener> listeners = new ArrayList<XAExecutionListener>();
    private ElementValueComparator comparator;

    /**
     * Constructor
     * @param cache the cache
     * @param underlyingStore the underlying store
     * @param txnManager the transaction manager, which will be cast to {@link javax.transaction.TransactionManager}
     * @param softLockFactory the soft lock factory
     * @param transactionIDFactory the transaction ID factory
     */
    public EhcacheXAResourceImpl(Ehcache cache, Store underlyingStore, Object txnManager, SoftLockFactory softLockFactory,
                                 TransactionIDFactory transactionIDFactory) {
        this.cache = cache;
        this.underlyingStore = underlyingStore;
        this.transactionIDFactory = transactionIDFactory;
        this.txnManager = (TransactionManager) txnManager;
        this.softLockFactory = softLockFactory;
        this.processor = new XARequestProcessor(this);
        this.comparator = cache.getCacheConfiguration().getElementValueComparatorConfiguration().getElementComparatorInstance();
    }

    /**
     * {@inheritDoc}
     */
    public void start(Xid xid, int flag) throws XAException {
        if (currentXid != null) {
            throw new EhcacheXAException("resource already started on " + currentXid, XAException.XAER_PROTO);
        }

        if (flag == TMNOFLAGS) {
            if (xidToContextMap.containsKey(xid)) {
                throw new EhcacheXAException("cannot start with duplicate XID: " + xid, XAException.XAER_DUPID);
            }
            currentXid = xid;
        } else if (flag == TMJOIN || flag == TMRESUME) {
            if (!xidToContextMap.containsKey(xid)) {
                throw new EhcacheXAException("cannot join/resume non-existent XID: " + xid, XAException.XAER_NOTA);
            }
            currentXid = xid;
        } else {
            throw new EhcacheXAException("unsupported flag: " + flag, XAException.XAER_PROTO);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void end(Xid xid, int flag) throws XAException {
        if (currentXid == null) {
            throw new EhcacheXAException("resource not started on " + xid, XAException.XAER_PROTO);
        }

        if (flag == TMSUCCESS || flag == TMSUSPEND) {
            if (!currentXid.equals(xid)) {
                throw new EhcacheXAException("cannot end working on unknown XID " + xid, XAException.XAER_NOTA);
            }
            currentXid = null;
        } else if (flag == TMFAIL) {
            if (!currentXid.equals(xid)) {
                throw new EhcacheXAException("cannot end working on " + xid + " while work on current XID " + currentXid + " hasn't ended",
                        XAException.XAER_PROTO);
            }
            xidToContextMap.remove(xid);
            currentXid = null;
        } else {
            throw new EhcacheXAException("unsupported flag: " + flag, XAException.XAER_PROTO);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void forget(Xid xid) throws XAException {
        processor.process(new XARequest(XARequest.RequestType.FORGET, xid));
    }

    /**
     * The forget implementation
     * @param xid a XID
     * @throws XAException when an error occurs
     */
    public void forgetInternal(Xid xid) throws XAException {
        List<Xid> xids = Arrays.asList(recover(TMSTARTRSCAN));
        if (!xids.contains(xid)) {
            throw new EhcacheXAException("forget called on in-doubt XID" + xid, XAException.XAER_PROTO);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getTransactionTimeout() throws XAException {
        return transactionTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return xaResource == this;
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(Xid xid) throws XAException {
        if (currentXid != null) {
            throw new EhcacheXAException("prepare called on non-ended XID: " + xid, XAException.XAER_PROTO);
        }
        return processor.process(new XARequest(XARequest.RequestType.PREPARE, xid));
    }

    /**
     * The prepare implementation
     * @param xid a XID
     * @return XA_OK or XA_RDONLY
     * @throws XAException when an error occurs
     */
    public int prepareInternal(Xid xid) throws XAException {
        fireBeforePrepare();

        XATransactionContext twopcTransactionContext = xidToContextMap.get(xid);
        if (twopcTransactionContext == null) {
            throw new EhcacheXAException("transaction never started: " + xid, XAException.XAER_NOTA);
        }


        XidTransactionID xidTransactionID = transactionIDFactory.createXidTransactionID(xid);

        List<Command> commands = twopcTransactionContext.getCommands();
        List<Command> preparedCommands = new LinkedList<Command>();

        boolean prepareUpdated = false;
        for (Command command : commands) {
            try {
                prepareUpdated |= command.prepare(underlyingStore, softLockFactory, xidTransactionID, comparator);
                preparedCommands.add(0, command);
            } catch (OptimisticLockFailureException ie) {
                for (Command preparedCommand : preparedCommands) {
                    preparedCommand.rollback(underlyingStore);
                }
                preparedCommands.clear();
                throw new EhcacheXAException(command + " failed because value changed between execution and 2PC",
                        XAException.XA_RBINTEGRITY, ie);
            }
        }

        xidToContextMap.remove(xid);

        if (!prepareUpdated) {
            rollbackInternal(xid);
        }

        return prepareUpdated ? XA_OK : XA_RDONLY;
    }

    /**
     * {@inheritDoc}
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (currentXid != null) {
            throw new EhcacheXAException("commit called on non-ended XID: " + xid, XAException.XAER_PROTO);
        }
        this.processor.process(new XARequest(XARequest.RequestType.COMMIT, xid, onePhase));
    }

    /**
     * The commit implementation
     * @param xid a XID
     * @param onePhase true if onePhase, false otherwise
     * @throws XAException when an error occurs
     */
    public void commitInternal(Xid xid, boolean onePhase) throws XAException {
        if (onePhase) {
            XATransactionContext twopcTransactionContext = xidToContextMap.get(xid);
            if (twopcTransactionContext == null) {
                throw new EhcacheXAException("cannot call commit(onePhase=true) after prepare", XAException.XAER_PROTO);
            }

            int rc = prepareInternal(xid);
            if (rc == XA_RDONLY) {
                return;
            }
        }

        XidTransactionID xidTransactionID = transactionIDFactory.createXidTransactionID(xid);
        Set<SoftLock> softLocks = softLockFactory.collectAllSoftLocksForTransactionID(xidTransactionID);
        for (SoftLock softLock : softLocks) {
            if (softLock.isExpired()) {
                softLock.lock();
                softLock.freeze();
            }
        }

        for (SoftLock softLock : softLocks) {
            try {
                softLock.getTransactionID().markForCommit();
            } catch (IllegalStateException ise) {
                throw new EhcacheXAException("XID already was rolling back: " + xid, XAException.XAER_RMERR);
            }

            Element frozenElement = softLock.getFrozenElement();

            if (frozenElement != null) {
                underlyingStore.put(frozenElement);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }

        for (SoftLock softLock : softLocks) {
            softLock.unfreeze();
            softLock.unlock();
        }


        fireAfterCommitOrRollback();
    }

    /**
     * {@inheritDoc}
     */
    public Xid[] recover(int flags) throws XAException {
        if ((flags & TMSTARTRSCAN) != TMSTARTRSCAN) {
            return new Xid[0];
        }

        Set<TransactionID> transactionIDs = softLockFactory.collectExpiredTransactionIDs();
        Set<Xid> xids = new HashSet<Xid>();

        for (TransactionID transactionID : transactionIDs) {
            XidTransactionID xidTransactionID = (XidTransactionID) transactionID;
            xids.add(xidTransactionID.getXid());
        }

        return xids.toArray(new Xid[0]);
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Xid xid) throws XAException {
        this.processor.process(new XARequest(XARequest.RequestType.ROLLBACK, xid));
    }

    /**
     * The rollback implementation
     * @param xid a XID
     * @throws XAException when an error occurs
     */
    public void rollbackInternal(Xid xid) throws XAException {
        XidTransactionID xidTransactionID = transactionIDFactory.createXidTransactionID(xid);
        Set<SoftLock> softLocks = softLockFactory.collectAllSoftLocksForTransactionID(xidTransactionID);
        for (SoftLock softLock : softLocks) {
            if (softLock.isExpired()) {
                softLock.lock();
                softLock.freeze();
            }
        }

        for (SoftLock softLock : softLocks) {
            try {
                ((XidTransactionID) softLock.getTransactionID()).markForRollback();
            } catch (IllegalStateException ise) {
                throw new EhcacheXAException("XID already was committing: " + xid, XAException.XAER_RMERR);
            }

            Element frozenElement = softLock.getFrozenElement();

            if (frozenElement != null) {
                underlyingStore.put(frozenElement);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }

        for (SoftLock softLock : softLocks) {
            softLock.unfreeze();
            softLock.unlock();
        }

        // in case of a phase 1 rollback, we need to clean the context
        xidToContextMap.remove(xid);

        fireAfterCommitOrRollback();
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(int timeout) throws XAException {
        if (timeout < 0) {
            throw new EhcacheXAException("timeout must be >= 0, was: " + timeout, XAException.XAER_INVAL);
        }
        if (timeout == 0) {
            this.transactionTimeout = DEFAULT_TRANSACTION_TIMEOUT;
        } else {
            this.transactionTimeout = timeout;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void addTwoPcExecutionListener(XAExecutionListener listener) {
        listeners.add(listener);
    }

    private void fireBeforePrepare() {
        for (XAExecutionListener listener : listeners) {
            listener.beforePrepare(this);
        }
    }

    private void fireAfterCommitOrRollback() {
        for (XAExecutionListener listener : listeners) {
            listener.afterCommitOrRollback(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public XATransactionContext createTransactionContext() throws SystemException, RollbackException {
        XATransactionContext ctx = getCurrentTransactionContext();
        if (ctx != null) {
            return ctx;
        }

        Transaction transaction = txnManager.getTransaction();
        transaction.enlistResource(this);

        // currentXid is set by a call to start() which itself is called by transaction.enlistResource(this)
        if (currentXid == null) {
            throw new CacheException("enlistment of XAResource of cache named '" + getCacheName() +
                    "' did not end up calling XAResource.start()");
        }

        XATransactionContext twopcTransactionContext = new XATransactionContext(underlyingStore);
        xidToContextMap.put(currentXid, twopcTransactionContext);
        return twopcTransactionContext;
    }

    /**
     * {@inheritDoc}
     */
    public XATransactionContext getCurrentTransactionContext() {
        if (currentXid == null) {
            return null;
        }
        return xidToContextMap.get(currentXid);
    }

}
