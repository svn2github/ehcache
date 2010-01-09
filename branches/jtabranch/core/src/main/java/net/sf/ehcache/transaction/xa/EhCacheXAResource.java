package net.sf.ehcache.transaction.xa;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.StoreWriteCommand;
import net.sf.ehcache.transaction.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Nabib El-Rahman
 */
public class EhCacheXAResource implements XAResource {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheXAResource.class.getName());

    private final ConcurrentMap<Transaction, TransactionContext> transactionDataTable = new ConcurrentHashMap<Transaction, TransactionContext>();
    private final ConcurrentMap<Xid, Transaction>                transactionXids      = new ConcurrentHashMap<Xid, Transaction>               ();
    private final String                                         cacheName;
    private       TransactionManager                             txnManager;
    private       Store                                          store;
    private       int                                            transactionTimeout;
  
   
    public EhCacheXAResource(String cacheName) {
        this.cacheName = cacheName;
    }
    
    public String  getCacheName() {
        return cacheName;
    }
    
    public void setTransactionManager(TransactionManager txnManager) {
        this.txnManager = txnManager;
    }  

    public void setStore(Store store) {
        this.store = store;
    }
    /**
     * XAResource Implementation
     */
    public void start(final Xid xid, final int flags) throws XAException {
        LOG.info("start called for Txn with id: " + xid);
        try {
            transactionXids.putIfAbsent(xid, txnManager.getTransaction());
        } catch (SystemException e) {
            XAException xaException = new XAException("WTF? " + e.getMessage());
            xaException.initCause(e);
            throw xaException;
        }
        System.out.println("\n\n\n    ===> SUCCESSFULLY LINKED XID TO TX\n\n");
    }

    public void commit(final Xid xid, final boolean onePhase) throws XAException {
        LOG.info("commit called for Txn with id: " + xid);
        TransactionContext context = transactionDataTable.get(transactionXids.get(xid));
        for (StoreWriteCommand storeWriteCommand : context.getCommands()) {
            storeWriteCommand.execute(store);
        }
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
        LOG.info("rollback called for Txn with id: " + xid);
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

    public Store getStore() {
        return store;
    }

    public TransactionContext getOrCreateTransactionContext() throws SystemException, RollbackException {
        Transaction transaction = txnManager.getTransaction();
        TransactionContext context = transactionDataTable.get(transaction);
        if(context == null) {
            context = new XaTransactionContext();
            transaction.enlistResource(this);
            TransactionContext previous = transactionDataTable.putIfAbsent(transaction, context);
            if(previous != null) {
                context = previous;
            }
        }
        return context;
    }

    @Override
    public boolean equals(Object obj) {
        EhCacheXAResource resource2 = (EhCacheXAResource)obj;
        if(cacheName.equals(resource2.getCacheName())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return cacheName.hashCode();
    }
    
    
}
