package net.sf.ehcache.transaction.xa;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.store.TransactionalStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nabib El-Rahman
 */
public class EhCacheXAResource implements XAResource {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheXAResource.class.getName());
    private final TransactionalStore store;
    private final TransactionManager txnManager;
    private int transactionTimeout;

    public EhCacheXAResource(TransactionalStore store, TransactionManager txnManager) {
        this.store = store;
        this.txnManager = txnManager;
    }

    /**
     * XAResource Implementation
     */
    public void start(final Xid xid, final int flags) throws XAException {
        LOG.info("start called for Txn with id: " + xid);
    }

    public void commit(final Xid xid, final boolean onePhase) throws XAException {
        LOG.info("commit called for Txn with id: " + xid);

    }

    public void end(final Xid xid, final int flags) throws XAException {
        LOG.info("end called for Txn with id: " + xid);
    }

    public void forget(final Xid xid) throws XAException {
        LOG.info("forget called for Txn with id: " + xid);
    }

    public int prepare(final Xid xid) throws XAException {
        LOG.info("prepare called for Txn with id: " + xid);
        return 0;
    }

    public Xid[] recover(final int i) throws XAException {
        return new Xid[0];
    }

    public void rollback(final Xid xid) throws XAException {

    }

    public boolean isSameRM(final XAResource xaResource) throws XAException {
        return this == xaResource;
    }

    public boolean setTransactionTimeout(final int i) throws XAException {
        this.transactionTimeout = i;
        // TODO: Figure out what to return here, it should be set to true of
        // setting the transaction timeout was successful.
        return false;
    }

    public int getTransactionTimeout() throws XAException {
        return this.transactionTimeout;
    }

}
