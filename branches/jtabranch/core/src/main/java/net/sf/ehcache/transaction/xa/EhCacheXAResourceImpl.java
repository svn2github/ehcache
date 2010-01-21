package net.sf.ehcache.transaction.xa;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

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
    
    private final String cacheName;
    private final EhCacheXAStore ehCacheXAStore;
    private int transactionTimeout;

    private Store store;
    private Store oldVersionStore;
    private TransactionManager txnManager;

    public EhCacheXAResourceImpl(String cacheName, Store store, TransactionManager txnManager, EhCacheXAStore ehcacheXAStore) {
        this.cacheName = cacheName;
        this.store = store;
        this.txnManager = txnManager;
        this.ehCacheXAStore = ehcacheXAStore;
        this.oldVersionStore = ehCacheXAStore.getOldVersionStore();
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
        Xid prevXid = ehCacheXAStore.storeXid2Transaction(xid, tx);
        if(prevXid != null && !prevXid.equals(xid)) {
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
        TransactionContext context = ehCacheXAStore.getTransactionContext(xid);

        Set<Serializable> keys = new HashSet<Serializable>();
        for (VersionAwareCommand command : context.getCommands()) {
            if(command.isWriteCommand()) {
                keys.add(command.getKey());
            }
        }

        Sync[] syncForKeys = ((CacheLockProvider)oldVersionStore.getInternalContext()).getAndWriteLockAllSyncForKeys(keys.toArray());
        for (VersionAwareCommand command : context.getCommands()) {
            ehCacheXAStore.checkin(command.getKey(), xid, command.isWriteCommand());
            if(command.isWriteCommand()) {
                Serializable key = command.getKey();
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
                Transaction txn = ehCacheXAStore.getTransactionContext(xid).getTransaction();
                if(txn != null) {
                    txn.delistResource(this, flags);
                }
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

        TransactionContext context = ehCacheXAStore.getTransactionContext(xid);
        CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
        CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

        // First dirty bulk check? todo keep this?
        validateCommands(context);
        Set<Serializable> keys = new HashSet<Serializable>();
        // Copy old versions in front-accessed store
        for (VersionAwareCommand command : context.getCommands()) {
            if(command.isWriteCommand()) {
                Serializable key = command.getKey();
                Sync syncForKey = oldVersionStoreLockProvider.getSyncForKey(key);
                syncForKey.lock(LockType.WRITE);
                try {
                    if (!ehCacheXAStore.isValid(command)) {
                        for (Serializable addedKey : keys) {
                            oldVersionStore.remove(addedKey);
                        }
                        throw new EhCacheXAException("Invalid version for element: " + command.getKey(),
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
        for (VersionAwareCommand command : context.getCommands()) {
            if(command.isWriteCommand()) {
                command.execute(store);
            }
        }
        ehCacheXAStore.prepared(xid);
        return context.getCommands().isEmpty() ? XA_RDONLY : XA_OK; // todo is this right?
    }

    private void validateCommands(TransactionContext context) throws XAException {
        for (VersionAwareCommand command : context.getCommands()) {
            if (command.isVersionAware()) {
                if (!ehCacheXAStore.isValid(command)) {
                    throw new EhCacheXAException("Invalid version for element: " + command.getKey(), XAException.XA_RBINTEGRITY);
                }
            }
        }
    }

    public Xid[] recover(final int i) throws XAException {
        return ehCacheXAStore.getPreparedXids();
    }

    public void rollback(final Xid xid) throws XAException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Rollback called for Txn with id: " + xid);
        }
        
        CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
        CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

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

    public TransactionContext getOrCreateTransactionContext() throws SystemException, RollbackException {
        Transaction transaction = txnManager.getTransaction();
        TransactionContext context =  ehCacheXAStore.getTransactionContext(transaction);
        if (context == null) {
            context = ehCacheXAStore.createTransactionContext(transaction);
            transaction.enlistResource(this);
           
        }
        return context;
    }
}
