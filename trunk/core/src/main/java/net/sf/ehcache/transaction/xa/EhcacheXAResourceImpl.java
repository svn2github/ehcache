package net.sf.ehcache.transaction.xa;

import java.util.HashSet;
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
 * @author Nabib El-Rahman
 */
public class EhcacheXAResourceImpl implements EhcacheXAResource {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheXAResourceImpl.class.getName());
    
    private final String cacheName;
    private final EhcacheXAStore ehcacheXAStore;
    private int transactionTimeout;

    private Store store;
    private Store oldVersionStore;
    private TransactionManager txnManager;

    public EhcacheXAResourceImpl(String cacheName, Store store, TransactionManager txnManager, EhcacheXAStore ehcacheXAStore) {
        this.cacheName = cacheName;
        this.store = store;
        this.txnManager = txnManager;
        this.ehcacheXAStore = ehcacheXAStore;
        this.oldVersionStore = ehcacheXAStore.getOldVersionStore();
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
        LOG.debug("Start called for Txn with id: " + xid);
      
 
        // todo we should probably track state propertly here...
        Transaction tx;
        try {
            tx = txnManager.getTransaction();
        } catch (SystemException e) {
            throw new EhcacheXAException("Couldn't get to current Transaction: " + e.getMessage(), e.errorCode, e);
        }
        if(tx == null) {
            throw new EhcacheXAException("Couldn't get to current Transaction ", XAException.XAER_OUTSIDE);
        }
        Xid prevXid = ehcacheXAStore.storeXid2Transaction(xid, tx);
        if(prevXid != null && !prevXid.equals(xid)) {
            throw new EhcacheXAException("Duplicated XID: " + xid, XAException.XAER_DUPID);
        }
    }

    public void commit(final Xid xid, final boolean onePhase) throws XAException {
        if (onePhase) {
            prepare(xid); // TODO if XA_READONLY, do we need to do anymore?
        }
        LOG.debug("{} phase commit called for Txn with id: {}", (onePhase ? "One" : "Two"), xid);
        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);

        Set<Object> keys = new HashSet<Object>();
        for (VersionAwareCommand command : context.getCommands()) {
            keys.add(command.getKey());
        }

        Sync[] syncForKeys = ((CacheLockProvider)oldVersionStore.getInternalContext()).getAndWriteLockAllSyncForKeys(keys.toArray());
        for (VersionAwareCommand command : context.getCommands()) {
            ehcacheXAStore.checkin(command.getKey(), xid, command.isWriteCommand());
            Object key = command.getKey();
            oldVersionStore.remove(key);
            ((CacheLockProvider)store.getInternalContext()).getSyncForKey(key).unlock(LockType.WRITE);
        }

        for (Sync syncForKey : syncForKeys) {
            syncForKey.unlock(LockType.WRITE);
        }

        ehcacheXAStore.removeData(xid);
    }

    public void end(final Xid xid, final int flags) throws XAException {
        if(flags != TMSUSPEND) {
            Transaction txn = ehcacheXAStore.getTransactionContext(xid).getTransaction();
        }
        LOG.debug("End called for Txn with id: {}", xid);
    }

    public void forget(final Xid xid) throws XAException {
        LOG.debug("Forget called for Txn with id: {}", xid);
    }

    public int prepare(final Xid xid) throws XAException {
        LOG.debug("Prepare called for Txn with id: {}", xid);

        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);
        CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
        CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

        // First dirty bulk check?
        validateCommands(context);
        Set<Object> keys = new HashSet<Object>();
        // Copy old versions in front-accessed store
        for (VersionAwareCommand command : context.getCommands()) {
            Object key = command.getKey();
            Sync syncForKey = oldVersionStoreLockProvider.getSyncForKey(key);
            syncForKey.lock(LockType.WRITE);
            try {
                if (!ehcacheXAStore.isValid(command)) {
                    for (Object addedKey : keys) {
                        oldVersionStore.remove(addedKey);
                    }
                    throw new EhcacheXAException("Invalid version for element: " + command.getKey(),
                        XAException.XA_RBINTEGRITY);
                }
                keys.add(key);
                oldVersionStore.put(store.get(key));
            } finally {
                syncForKey.unlock(LockType.WRITE);
            }
        }

        // Lock all keys in real store
        Sync[] syncForKeys = storeLockProvider.getAndWriteLockAllSyncForKeys(keys.toArray());

        LOG.debug("Locked {} syncs for {} keys", syncForKeys == null ? 0 : syncForKeys.length, keys.size());

        ehcacheXAStore.prepared(xid);

        // Execute write command within the real underlying store
        boolean writes = false;
        for (VersionAwareCommand command : context.getCommands()) {
            writes = command.execute(store) || writes;
        }
        return writes ? XA_OK : XA_RDONLY;
    }

    private void validateCommands(TransactionContext context) throws XAException {
        for (VersionAwareCommand command : context.getCommands()) {
            if (command.isVersionAware()) {
                if (!ehcacheXAStore.isValid(command)) {
                    throw new EhcacheXAException("Invalid version for element: " + command.getKey(), XAException.XA_RBINTEGRITY);
                }
            }
        }
    }

    public Xid[] recover(final int i) throws XAException {
        return ehcacheXAStore.getPreparedXids();
    }

    public void rollback(final Xid xid) throws XAException {
        LOG.debug("Rollback called for Txn with id: {}", xid);

        TransactionContext context = ehcacheXAStore.getTransactionContext(xid);
        if (ehcacheXAStore.isPrepared(xid)) {
            CacheLockProvider storeLockProvider = (CacheLockProvider)store.getInternalContext();
            CacheLockProvider oldVersionStoreLockProvider = (CacheLockProvider)oldVersionStore.getInternalContext();

            for (VersionAwareCommand command : context.getCommands()) {
                Object key = command.getKey();
                Sync syncForKey = oldVersionStoreLockProvider.getSyncForKey(key);
                syncForKey.lock(LockType.WRITE);
                Element element = null;
                try {
                    element = oldVersionStore.remove(key);
                    if(element != null) {
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
        ehcacheXAStore.removeData(xid);
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
        EhcacheXAResource resource2 = (EhcacheXAResource) obj;
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
        if(transaction == null) {
            throw new CacheException("Cache " + cacheName + " can only be accessed within a JTA Transaction!");
        }

        if(transaction.getStatus() != Status.STATUS_ACTIVE) {
            throw new CacheException("Transaction not active!");
        }

        TransactionContext context =  ehcacheXAStore.getTransactionContext(transaction);
        if (context == null) {
            transaction.enlistResource(this);
            context = ehcacheXAStore.createTransactionContext(transaction);
        }
        return context;
    }

    public Element get(final Object key) {
        Element element = oldVersionStore.get(key);
        if(element == null) {
            element = store.get(key);
        }
        return element;
    }

    public Element getQuiet(final Object key) {
        Element element = oldVersionStore.getQuiet(key);
        if(element == null) {
            element = store.getQuiet(key);
        }
        return element;
    }
}
