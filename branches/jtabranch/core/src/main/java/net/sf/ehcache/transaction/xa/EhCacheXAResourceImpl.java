package net.sf.ehcache.transaction.xa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nabib El-Rahman
 */
public class EhCacheXAResourceImpl implements EhCacheXAResource {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheXAResourceImpl.class.getName());
    private static final TransactionTableFactory FACTORY = new TransactionTableFactoryImpl();
    private final TransactionTableFactory factory;
    private final ConcurrentMap<Transaction, XaTransactionContext> transactionDataTable;
    private final ConcurrentMap<Xid, Transaction> transactionXids;
    private final ConcurrentMap<Xid, Xid> prepareXids;
    private final VersionTable versionTable;

    
    private final String cacheName;
    private int transactionTimeout;

    private transient Store store;
    private transient Store oldVersionStore;
    private transient TransactionManager txnManager;

    public EhCacheXAResourceImpl(String cacheName, Store store, TransactionManager txnManager) {
        this(cacheName, store, txnManager, FACTORY);
    }
    
    public EhCacheXAResourceImpl(String cacheName, Store store, TransactionManager txnManager,
            TransactionTableFactory factory) {
        this.cacheName = cacheName;
        this.store = store;
        this.txnManager = txnManager;
        this.factory = factory;
        this.transactionDataTable = factory.getTransactionDataTable();
        this.transactionXids = factory.getTransactionXids();
        this.prepareXids = factory.getPrepareXids();
        this.versionTable = new VersionTable(factory);
      
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#getCacheName()
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * XAResource Implementation
     */
    public void start(final Xid xid, final int flags) throws XAException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Start called for Txn with id: " + xid);
        }
        // todo we should probably track state propertly here...
        Transaction tx;
        try {
            tx = txnManager.getTransaction();
        } catch (SystemException e) {
            throw new EhCacheXAException("Couldn't get to current Transaction: " + e.getMessage(), e.errorCode, e);
        }
        if(tx == null) {
            throw new EhCacheXAException("Couldn't get to current Transaction ", XAException.XAER_OUTSIDE);
        }
        Transaction previous = transactionXids.putIfAbsent(xid, tx);
        if(previous != null && !previous.equals(tx)) {
            throw new EhCacheXAException("Duplicated XID: " + xid, XAException.XAER_DUPID);
        }
    }

    public void commit(final Xid xid, final boolean onePhase) throws XAException {
        if (onePhase) {
            prepare(xid); // TODO if XA_READONLY, do we need to do anymore?
        }
        if(LOG.isInfoEnabled()) {
            LOG.info((onePhase ? "One" : "Two") + " phase commit called for Txn with id: " + xid);
        }
        XaTransactionContext context = transactionDataTable.get(transactionXids.get(xid));

        Set<Serializable> keys = new HashSet<Serializable>();
        for (VersionAwareWrapper command : context.getCommands()) {
            if(command.isWriteCommand()) {
                keys.add(command.getElement().getKey());
            }
        }

        Sync[] syncForKeys = ((CacheLockProvider)oldVersionStore.getInternalContext()).getAndWriteLockAllSyncForKeys(keys.toArray());
        for (VersionAwareWrapper command : context.getCommands()) {
            versionTable.checkin(command.getElement(), context.getTransaction(), command.isWriteCommand());
            if(command.isWriteCommand()) {
                Serializable key = command.getElement().getKey();
                oldVersionStore.remove(key);
                ((CacheLockProvider)store.getInternalContext()).getSyncForKey(key).unlock(LockType.WRITE);
            }
        }

        for (Sync syncForKey : syncForKeys) {
            syncForKey.unlock(LockType.WRITE);
        }
    }

    public void end(final Xid xid, final int flags) throws XAException {
        try {
            if(flags != TMSUSPEND) {
                Transaction txn = transactionXids.get(xid);
                txn.delistResource(this, flags);
                transactionDataTable.remove(txn);
                transactionXids.remove(xid);           
            } else {
                //todo move tx data to CDM!
            }
        } catch(SystemException e) {
            throw new EhCacheXAException("Couldn't delist XAResource", e.errorCode, e);
        }
        if(LOG.isInfoEnabled()) {
            LOG.info("End called for Txn with id: " + xid);
        }
    }

    public void forget(final Xid xid) throws XAException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Forget called for Txn with id: " + xid);
        }
    }

    public int prepare(final Xid xid) throws XAException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Prepare called for Txn with id: " + xid);
        }

        XaTransactionContext context = transactionDataTable.get(transactionXids.get(xid));
        CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
        CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

        // First dirty bulk check? todo keep this?
        validateCommands(context);
        Set<Serializable> keys = new HashSet<Serializable>();
        // Copy old versions in front-accessed store
        for (VersionAwareWrapper versionAwareWrapper : context.getCommands()) {
            if(versionAwareWrapper.isWriteCommand()) {
                Serializable key = versionAwareWrapper.getElement().getKey();
                Sync syncForKey = oldVersionStoreLockProvider.getSyncForKey(key);
                syncForKey.lock(LockType.WRITE);
                try {
                    if (!versionTable.valid(versionAwareWrapper.getElement(), versionAwareWrapper.getVersion())) {
                        for (Serializable addedKey : keys) {
                            oldVersionStore.remove(addedKey);
                        }
                        throw new EhCacheXAException("Invalid version for element: " + versionAwareWrapper.getElement(),
                            XAException.XA_RBINTEGRITY);
                    }
                    keys.add(key);
                    oldVersionStore.put(store.get(key));
                } finally {
                    syncForKey.unlock(LockType.WRITE);
                }
            }
        }

        // Lock all keys in real store
        storeLockProvider.getAndWriteLockAllSyncForKeys(keys.toArray());

        // Execute write command within the real underlying store
        for (VersionAwareWrapper versionAwareWrapper : context.getCommands()) {
            if(versionAwareWrapper.isWriteCommand()) {
                versionAwareWrapper.execute(store);
            }
        }
        prepareXids.put(xid, xid);
        return context.getCommands().isEmpty() ? XA_RDONLY : XA_OK; // todo is this right?
    }

    private void validateCommands(XaTransactionContext context) throws XAException {
        for (VersionAwareWrapper wrapper : context.getCommands()) {
            if (wrapper.isVersionAware()) {
                if (!versionTable.valid(wrapper.getElement(), wrapper.getVersion())) {
                    throw new EhCacheXAException("Invalid version for element: " + wrapper.getElement(), XAException.XA_RBINTEGRITY);
                }
            }
        }
    }

    public Xid[] recover(final int i) throws XAException {
        Set xidSet = prepareXids.keySet();
        return (Xid[])xidSet.toArray(new Xid[xidSet.size()]);
    }

    public void rollback(final Xid xid) throws XAException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Rollback called for Txn with id: " + xid);
        }
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

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#getStore()
     */
    public Store getStore() {
        return store;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#checkoutReadOnly(net.sf.ehcache.Element, javax.transaction.Transaction)
     */
    public long checkout(Element element, Transaction txn) {
        return versionTable.checkout(element, txn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.transaction.xa.EhCacheXAResource#getOrCreateTransactionContext()
     */
    public TransactionContext getOrCreateTransactionContext() throws SystemException, RollbackException {
        Transaction transaction = txnManager.getTransaction();
        XaTransactionContext context = transactionDataTable.get(transaction);
        if (context == null) {
            context = new XaTransactionContext(transaction, this);
            transaction.enlistResource(this);
            XaTransactionContext previous = transactionDataTable.putIfAbsent(transaction, context);
            if (previous != null) {
                context = previous;
            }
        }
        return context;
    }

    @Override
    public boolean equals(Object obj) {
        EhCacheXAResource resource2 = (EhCacheXAResource) obj;
        if (cacheName.equals(resource2.getCacheName())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return cacheName.hashCode();
    }

    public static class VersionTable {

        private final TransactionTableFactory factory;
        protected final ConcurrentMap<Object, Version> versionStore;
        

        public VersionTable(TransactionTableFactory factory) {
            this.factory = factory;
            this.versionStore = factory.getVersionStore();
        }
       

        public synchronized boolean valid(Element element, long currentVersionNumber) {
            Object key = element.getObjectKey();
            Version version = versionStore.get(key);
            if (version != null) {
                long currentVersion = version.getCurrentVersion();
                boolean valid = (currentVersion == currentVersionNumber);
                return valid;
            } else {
                // TODO: Figure out what this case is..
                return true;
            }

        }

        public synchronized long checkout(Element element, Transaction txn) {
            long versionNumber = -1;
            Object key = element.getObjectKey();
            Version version = versionStore.get(key);
            if (version == null) {
                version = new Version(factory);
                versionStore.put(key, version);
            }
            versionNumber = version.checkout(txn);

            return versionNumber;
        }

        public synchronized void checkin(Element element, Transaction txn, boolean readOnly) {
            Object key = element.getObjectKey();
            Version version = versionStore.get(key);
            boolean removeEntry = false;
            if (readOnly) {
                removeEntry = version.checkinRead(txn);
            } else {
                removeEntry = version.checkinWrite(txn);
            }
            if (removeEntry) {
                versionStore.remove(key);
            }
        }

    }

    public static class Version {

        final AtomicLong version = new AtomicLong(0);

        // TODO: We need to figure out a more compressed data-structure (need to performance test to confirm
        final ConcurrentMap<Transaction, Long> txnVersions;
        
        public Version(TransactionTableFactory factory) {
            this.txnVersions = factory.getTxnVersions();
        }

        public long getCurrentVersion() {
            return version.get();
        }

        public boolean hasTransaction(Transaction txn) {
            return txnVersions.containsKey(txn);
        }

        public long getVersion(Transaction txn) {
            try {
                return txnVersions.get(txn);
            } catch (NullPointerException e) {
                throw new AssertionError("Cannot get version for not existing transaction: " + txn);
            }
        }

        public long checkout(Transaction txn) {
            long v = version.get();
            txnVersions.put(txn, v);
            return v;
        }

        public boolean checkinRead(Transaction txn) {
            txnVersions.remove(txn);
            return txnVersions.isEmpty();
        }

        public boolean checkinWrite(Transaction txn) {
            long v = txnVersions.remove(txn);
            version.incrementAndGet();
            return txnVersions.isEmpty();
        }
    }

    public void initalizeTransients(Store store, Store oldVersionStore, TransactionManager txnManager) {
        this.store = store;
        this.oldVersionStore = oldVersionStore;
        this.txnManager = txnManager;
    }

}
