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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.SoftLockManager;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.TransactionIDNotFoundException;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.commands.Command;
import net.sf.ehcache.transaction.xa.processor.XARequest;
import net.sf.ehcache.transaction.xa.processor.XARequestProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.observer.OperationObserver;

/**
 * The EhcacheXAResource implementation
 *
 * @author Ludovic Orban
 */
public class EhcacheXAResourceImpl implements EhcacheXAResource {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheXAResourceImpl.class.getName());
    private static final long MILLISECOND_PER_SECOND = 1000L;

    private final Ehcache cache;
    private final Store underlyingStore;
    private final TransactionIDFactory transactionIDFactory;
    private final TransactionManager txnManager;
    private final SoftLockManager softLockManager;
    private final ConcurrentMap<Xid, XATransactionContext> xidToContextMap = new ConcurrentHashMap<Xid, XATransactionContext>();
    private final XARequestProcessor processor;
    private volatile Xid currentXid;
    private volatile int transactionTimeout;
    private final List<XAExecutionListener> listeners = new ArrayList<XAExecutionListener>();
    private final ElementValueComparator comparator;

    private final OperationObserver<XaCommitOutcome> commitObserver;
    private final OperationObserver<XaRollbackOutcome> rollbackObserver;
    private final OperationObserver<XaRecoveryOutcome> recoveryObserver;

    /**
     * Constructor
     * @param cache the cache
     * @param underlyingStore the underlying store
     * @param txnManagerLookup the transaction manager lookup
     * @param softLockManager the soft lock manager
     * @param transactionIDFactory the transaction ID factory
     */
    public EhcacheXAResourceImpl(Ehcache cache, Store underlyingStore, TransactionManagerLookup txnManagerLookup,
                                 SoftLockManager softLockManager, TransactionIDFactory transactionIDFactory,
                                 ReadWriteCopyStrategy<Element> copyStrategy, OperationObserver<XaCommitOutcome> commitObserver,
                                 OperationObserver<XaRollbackOutcome> rollbackObserver, OperationObserver<XaRecoveryOutcome> recoveryObserver) {
        this.cache = cache;
        this.underlyingStore = underlyingStore;
        this.transactionIDFactory = transactionIDFactory;
        this.txnManager = txnManagerLookup.getTransactionManager();
        this.softLockManager = softLockManager;
        this.processor = new XARequestProcessor(this);
        this.transactionTimeout = cache.getCacheManager().getTransactionController().getDefaultTransactionTimeout();
        this.comparator = cache.getCacheConfiguration().getElementValueComparatorConfiguration()
            .createElementComparatorInstance(cache.getCacheConfiguration());
        this.commitObserver = commitObserver;
        this.rollbackObserver = rollbackObserver;
        this.recoveryObserver = recoveryObserver;
    }

    /**
     * {@inheritDoc}
     */
    public void start(Xid xid, int flag) throws XAException {
        LOG.debug("start [{}] [{}]", xid, prettyPrintXAResourceFlags(flag));

        if (currentXid != null) {
            throw new EhcacheXAException("resource already started on " + currentXid, XAException.XAER_PROTO);
        }

        if (flag == TMNOFLAGS) {
            if (xidToContextMap.containsKey(xid)) {
                throw new EhcacheXAException("cannot start with duplicate XID: " + xid, XAException.XAER_DUPID);
            }
            currentXid = xid;
        } else if (flag == TMRESUME) {
            if (!xidToContextMap.containsKey(xid)) {
                throw new EhcacheXAException("cannot resume non-existent XID: " + xid, XAException.XAER_NOTA);
            }
            currentXid = xid;
        } else if (flag == TMJOIN) {
            currentXid = xid;
        } else {
            throw new EhcacheXAException("unsupported flag: " + flag, XAException.XAER_PROTO);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void end(Xid xid, int flag) throws XAException {
        LOG.debug("end [{}] [{}]", xid, prettyPrintXAResourceFlags(flag));

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
        LOG.debug("forget [{}]", xid);

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
        boolean same;
        if (xaResource == this) {
            same = true;
        } else if (xaResource instanceof EhcacheXAResourceImpl) {
            same = (cache == ((EhcacheXAResourceImpl) xaResource).cache);
        } else {
            same = false;
        }
        LOG.debug("{} isSameRm {} -> " + same, this, xaResource);
        return same;
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(Xid xid) throws XAException {
        LOG.debug("prepare [{}]", xid);

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


        XidTransactionID xidTransactionID = transactionIDFactory.createXidTransactionID(xid, cache);

        List<Command> commands = twopcTransactionContext.getCommands();
        List<Command> preparedCommands = new LinkedList<Command>();

        boolean prepareUpdated = false;
        LOG.debug("preparing {} command(s) for [{}]", commands.size(), xid);
        for (Command command : commands) {
            try {
                prepareUpdated |= command.prepare(underlyingStore, softLockManager, xidTransactionID, comparator);
                preparedCommands.add(0, command);
            } catch (OptimisticLockFailureException ie) {
                for (Command preparedCommand : preparedCommands) {
                    preparedCommand.rollback(underlyingStore, softLockManager);
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

        LOG.debug("prepared xid [{}] read only? {}", xid, !prepareUpdated);
        return prepareUpdated ? XA_OK : XA_RDONLY;
    }

    /**
     * {@inheritDoc}
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        LOG.debug("commit [{}] [{}]", xid, onePhase);

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
        commitObserver.begin();
        XidTransactionID xidTransactionID = transactionIDFactory.createXidTransactionID(xid, cache);
        try {
            if (onePhase) {
                XATransactionContext twopcTransactionContext = xidToContextMap.get(xid);
                if (twopcTransactionContext == null) {
                    throw new EhcacheXAException("cannot call commit(onePhase=true) after prepare", XAException.XAER_PROTO);
                }

                int rc = prepareInternal(xid);
                if (rc == XA_RDONLY) {
                    commitObserver.end(XaCommitOutcome.READ_ONLY);
                    return;
                }
            }

            Set<SoftLock> softLocks = softLockManager.collectAllSoftLocksForTransactionID(xidTransactionID);
            LOG.debug("committing {} soft lock(s) for [{}]", softLocks.size(), xid);
            for (SoftLock softLock : softLocks) {
                if (softLock.isExpired()) {
                    softLock.lock();
                    softLock.freeze();
                }
            }

            try {
                transactionIDFactory.markForCommit(xidTransactionID);
            } catch (TransactionIDNotFoundException tnfe) {
                commitObserver.end(XaCommitOutcome.EXCEPTION);
                throw new EhcacheXAException("cannot find XID, it might have been duplicated and cleaned up earlier on: " + xid,
                    XAException.XAER_NOTA, tnfe);
            } catch (IllegalStateException ise) {
                commitObserver.end(XaCommitOutcome.EXCEPTION);
                throw new EhcacheXAException("XID already was rolling back: " + xid, XAException.XAER_RMERR);
            }

            for (SoftLock softLock : softLocks) {
                Element e = underlyingStore.getQuiet(softLock.getKey());
                if (e == null) {
                    // the element can be null if it was manually unpinned, see DEV-8308
                    LOG.debug("soft lock ID with key '{}' is not present in underlying store, ignoring it", softLock.getKey());
                    continue;
                }
                SoftLockID softLockId = (SoftLockID)e.getObjectValue();
                Element frozenElement = softLockId.getNewElement();

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
            commitObserver.end(XaCommitOutcome.COMMITTED);
        } finally {
            transactionIDFactory.clear(xidTransactionID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Xid[] recover(int flags) throws XAException {
        recoveryObserver.begin();
        LOG.debug("recover [{}]", prettyPrintXAResourceFlags(flags));

        if ((flags & TMSTARTRSCAN) != TMSTARTRSCAN) {
            return new Xid[0];
        }

        final Set<Xid> xids = Collections.synchronizedSet(new HashSet<Xid>());

        Thread t = new Thread("ehcache [" + cache.getName() + "] XA recovery thread") {
            @Override
            public void run() {
                for (XidTransactionID xidTransactionID : transactionIDFactory.getAllXidTransactionIDsFor(cache)) {
                    if (transactionIDFactory.isExpired(xidTransactionID)) {
                        xids.add(xidTransactionID.getXid());
                    }
                }
            }
        };
        try {
            t.start();
            t.join(transactionTimeout * MILLISECOND_PER_SECOND);
        } catch (InterruptedException e) {
            // ignore
        }
        if (t.isAlive()) {
            t.interrupt();
        }

        if (xids.isEmpty()) {
            recoveryObserver.end(XaRecoveryOutcome.NOTHING);
        } else {
            recoveryObserver.end(XaRecoveryOutcome.RECOVERED, xids.size());
        }
        return xids.toArray(new Xid[0]);
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Xid xid) throws XAException {
        LOG.debug("rollback [{}]", xid);

        this.processor.process(new XARequest(XARequest.RequestType.ROLLBACK, xid));
    }

    /**
     * The rollback implementation
     * @param xid a XID
     * @throws XAException when an error occurs
     */
    public void rollbackInternal(Xid xid) throws XAException {
        rollbackObserver.begin();
        XidTransactionID xidTransactionID = transactionIDFactory.createXidTransactionID(xid, cache);
        try {
            Set<SoftLock> softLocks = softLockManager.collectAllSoftLocksForTransactionID(xidTransactionID);
            for (SoftLock softLock : softLocks) {
                if (softLock.isExpired()) {
                    softLock.lock();
                    softLock.freeze();
                }
            }

            try {
                transactionIDFactory.markForRollback(xidTransactionID);
            } catch (TransactionIDNotFoundException tnfe) {
                rollbackObserver.end(XaRollbackOutcome.EXCEPTION);
                throw new EhcacheXAException("cannot find XID, it might have been duplicated an cleaned up earlier on: " + xid,
                    XAException.XAER_NOTA, tnfe);
            } catch (IllegalStateException ise) {
                rollbackObserver.end(XaRollbackOutcome.EXCEPTION);
                throw new EhcacheXAException("XID already was committing: " + xid, XAException.XAER_RMERR);
            }

            for (SoftLock softLock : softLocks) {
                Element e = underlyingStore.getQuiet(softLock.getKey());
                if (e == null) {
                    // the element can be null if it was manually unpinned, see DEV-8308
                    LOG.debug("soft lock ID with key '{}' is not present in underlying store, ignoring it", softLock.getKey());
                    continue;
                }
                SoftLockID softLockId = (SoftLockID)e.getObjectValue();
                Element frozenElement = softLockId.getOldElement();

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
            rollbackObserver.end(XaRollbackOutcome.ROLLEDBACK);
        } finally {
            transactionIDFactory.clear(xidTransactionID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(int timeout) throws XAException {
        if (timeout < 0) {
            throw new EhcacheXAException("timeout must be >= 0, was: " + timeout, XAException.XAER_INVAL);
        }
        if (timeout == 0) {
            this.transactionTimeout = cache.getCacheManager().getTransactionController().getDefaultTransactionTimeout();
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
        LOG.debug("enlisting {} in {}", this, transaction);
        transaction.enlistResource(this);

        // currentXid is set by a call to start() which itself is called by transaction.enlistResource(this)
        if (currentXid == null) {
            throw new CacheException("enlistment of XAResource of cache named '" + getCacheName() +
                    "' did not end up calling XAResource.start()");
        }

        ctx = xidToContextMap.get(currentXid);
        if (ctx == null) {
            LOG.debug("creating new context for XID [{}]", currentXid);
            ctx = new XATransactionContext(underlyingStore);
            xidToContextMap.put(currentXid, ctx);
        }

        return ctx;
    }

    /**
     * {@inheritDoc}
     */
    public XATransactionContext getCurrentTransactionContext() {
        if (currentXid == null) {
            LOG.debug("getting current TX context of XAResource with current XID [null]: null");
            return null;
        }
        XATransactionContext xaTransactionContext = xidToContextMap.get(currentXid);
        LOG.debug("getting current TX context of XAResource with current XID [{}]: {}", currentXid, xaTransactionContext);
        return xaTransactionContext;
    }


    private static String prettyPrintXAResourceFlags(int flags) {
        StringBuilder sb = new StringBuilder();


        if ((flags & XAResource.TMENDRSCAN) == XAResource.TMENDRSCAN) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMENDRSCAN");
        }
        if ((flags & XAResource.TMFAIL) == XAResource.TMFAIL) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMFAIL");
        }
        if ((flags & XAResource.TMJOIN) == XAResource.TMJOIN) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMJOIN");
        }
        if ((flags & XAResource.TMONEPHASE) == XAResource.TMONEPHASE) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMONEPHASE");
        }
        if ((flags & XAResource.TMRESUME) == XAResource.TMRESUME) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMRESUME");
        }
        if ((flags & XAResource.TMSTARTRSCAN) == XAResource.TMSTARTRSCAN) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMSTARTRSCAN");
        }
        if ((flags & XAResource.TMSUCCESS) == XAResource.TMSUCCESS) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMSUCCESS");
        }
        if ((flags & XAResource.TMSUSPEND) == XAResource.TMSUSPEND) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append("TMSUSPEND");
        }
        if (sb.length() == 0 && flags == XAResource.TMNOFLAGS) {
            sb.append("TMNOFLAGS");
        }
        if (sb.length() == 0) {
            sb.append("unknown flag: ").append(flags);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "EhcacheXAResourceImpl of cache " + cache.getName();
    }
}
