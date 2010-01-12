package net.sf.ehcache.transaction.xa;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.StoreWriteCommand;
import net.sf.ehcache.transaction.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nabib El-Rahman
 */
public class EhCacheXAResourceImpl implements EhCacheXAResource {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheXAResourceImpl.class.getName());

    private final ConcurrentMap<Transaction, TransactionContext> transactionDataTable = new ConcurrentHashMap<Transaction, TransactionContext>();
    private final ConcurrentMap<Xid, Transaction>                transactionXids      = new ConcurrentHashMap<Xid, Transaction>               ();
    private final VersionTable                                   versionTable         = new VersionTable();

    private final String                                         cacheName;
    private final Store                                          store;
    private final TransactionManager                             txnManager;
    private       int                                            transactionTimeout;
  
   
    public EhCacheXAResourceImpl(String cacheName, Store store, TransactionManager txnManager) {
        this.cacheName = cacheName;
        this.store = store;
        this.txnManager = txnManager;
    }
    
    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#getCacheName()
     */
    public String  getCacheName() {
        return cacheName;
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

    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#getStore()
     */
    public Store getStore() {
        return store;
    }
    
    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#checkoutReadOnly(net.sf.ehcache.Element, javax.transaction.Transaction)
     */
    public long checkoutReadOnly(Element element, Transaction txn) {
        return versionTable.checkoutReadOnly(element, txn);
    }
    
    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#checkout(net.sf.ehcache.Element, javax.transaction.Transaction)
     */
    public long checkout(Element element, Transaction txn) {
        return versionTable.checkout(element, txn);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#getOrCreateTransactionContext()
     */
    public TransactionContext getOrCreateTransactionContext() throws SystemException, RollbackException {
        Transaction transaction = txnManager.getTransaction();
        TransactionContext context = transactionDataTable.get(transaction);
        if(context == null) {
            context = new XaTransactionContext(transaction);
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
    
    public static class VersionTable {

        protected final ConcurrentMap versionStore = new ConcurrentHashMap();
        
        public long checkoutReadOnly(Element element, Transaction txn) {
            return checkout(element, txn, true);
        }
        
        public long checkout(Element element, Transaction txn) {
            return checkout(element, txn, false);
        }

        private long checkout(Element element, Transaction txn, boolean readOnly) {
            long versionNumber = -1;
            Object key = element.getObjectKey();
            Version version = (Version)versionStore.get(key);
            if(version == null) {
                version = new Version();
                versionStore.put(key, version);
            }           
            if (readOnly) {
                versionNumber = version.checkoutRead(txn);
            } else {
                versionNumber = version.checkoutWrite(txn);
            }
            return versionNumber;
        }

        public void checkin(Element element, Transaction txn) {
            Object key = element.getObjectKey();
            Version version = (Version) versionStore.get(key);
            boolean removeEntry = version.checkin(txn);
            if (removeEntry) {
                versionStore.remove(key);
            }
        }

    }

    static class Version {

        AtomicLong version = new AtomicLong(0);

        // TODO: We need to figure out a more compressed data-structure (need to performance test to confirm
        ConcurrentMap txnVersionMap = new ConcurrentHashMap();


        public long getCurrentVersion() {
            return version.get();
        }
        
        public boolean hasTransaction(Transaction txn) {
            return txnVersionMap.containsKey(txn);
        }
        
        public long getVersion(Transaction txn) {   
            try {
                return (Long)txnVersionMap.get(txn);
            } catch(NullPointerException e) {
                throw new AssertionError("Cannot get version for not existing transaction: " + txn);
            }
        }

        public long checkoutRead(Transaction txn) {
            long v = version.get();
            txnVersionMap.put(txn, v);
            return v;
        }

        public long checkoutWrite(Transaction txn) {
            long v = version.incrementAndGet();
            txnVersionMap.put(txn, v);
            return v;
        }

        // TODO: need to come back to the isEmpty call to see how reliable that is for CHM
        public boolean checkin(Transaction txn) {
            txnVersionMap.remove(txn);
            return txnVersionMap.isEmpty();
        }

    }
    
    
    
}
