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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;

/**
 * Default implementation of {@link EhcacheXAStore}.<p>
 * It uses {@link java.util.concurrent.ConcurrentHashMap} for the local data (non safe in case of failure) and requires
 * a "safe" {@link net.sf.ehcache.store.Store} for the oldVersionStore, and a reference to the underlying Store.
 *
 * @author Alex Snaps
 */
public class EhcacheXAStoreImpl implements EhcacheXAStore {
    
    /** protected for testing **/
    protected final ConcurrentMap<Xid, XATransactionContext>   transactionContextXids = new ConcurrentHashMap<Xid, XATransactionContext>();
    /** protected for testing **/
    protected final ConcurrentMap<Xid, PreparedContext>        prepareXids            = new ConcurrentHashMap<Xid, PreparedContext>();
    /** protected for testing **/
    protected final ConcurrentMap<Xid, XATransactionContext>   suspendXids            = new ConcurrentHashMap<Xid, XATransactionContext>();
    /** protected for testing **/
    protected final ConcurrentMap<Transaction, Xid>            localTxn2XidTable      = new ConcurrentHashMap<Transaction, Xid>();
    /** protected for testing **/
    protected final ConcurrentMap<Xid, Transaction>            localXid2TxnTable      = new ConcurrentHashMap<Xid, Transaction>();
    /** protected for testing **/
    protected final VersionTable                               versionTable           = new VersionTable();
    /** protected for testing **/
    protected Store underlyingStore;
    /** protected for testing **/
    protected Store oldVersionStore;

    /**
     * Constructor
     * @param underlyingStore the real underlying store
     * @param oldVersionStore the old version, read-only, used to access keys during 2pc
     */
    public EhcacheXAStoreImpl(Store underlyingStore, Store oldVersionStore) {
        this.underlyingStore = new SyncAwareStore(underlyingStore);
        this.oldVersionStore = new SyncAwareStore(oldVersionStore);
    }

    /**
     * {@inheritDoc}
     */
    public Store getOldVersionStore() {
        return this.oldVersionStore;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPrepared(final Xid xid) {
        return prepareXids.containsKey(xid);
    }

    /**
     * {@inheritDoc}
     */
    public void removeData(final Xid xid) {
        prepareXids.remove(xid);
        suspendXids.remove(xid);
        transactionContextXids.remove(xid);
        Transaction txn = localXid2TxnTable.get(xid);
        if (txn != null) {
          localTxn2XidTable.remove(txn);
        }
        localXid2TxnTable.remove(xid);

    }

    /**
     * {@inheritDoc}
     */
    public void checkin(Object key, Xid xid, boolean readOnly) {
       versionTable.checkin(key, xid, readOnly);
    }

    /**
     * {@inheritDoc}
     */
    public long checkout(Object key, Xid xid) {
        return versionTable.checkout(key, xid);
    }
    
    /**
     * {@inheritDoc}
     */
    public Xid storeXid2Transaction(Xid xid, Transaction transaction) {
        localXid2TxnTable.putIfAbsent(xid, transaction);
        return localTxn2XidTable.putIfAbsent(transaction, xid);
    }

    /**
     * {@inheritDoc}
     */
    public TransactionContext createTransactionContext(Transaction txn) {
        Xid xid = localTxn2XidTable.get(txn);
        XATransactionContext context = new XATransactionContext(xid, this);
        XATransactionContext previous = transactionContextXids.putIfAbsent(xid, context);
        if (previous != null) {
            context = previous;
        }
        return context;
    }

    /**
     * {@inheritDoc}
     */
    public Xid[] getPreparedXids() {
        Set<Xid> xidSet = prepareXids.keySet();
        return xidSet.toArray(new Xid[xidSet.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public TransactionContext getTransactionContext(Xid xid) {
        return transactionContextXids.get(xid);
    }

    /**
     * {@inheritDoc}
     */
    public TransactionContext getTransactionContext(Transaction txn) {
        Xid xid = localTxn2XidTable.get(txn);
        return transactionContextXids.get(xid);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid(VersionAwareCommand command, Xid xid) {
        return versionTable.valid(command.getKey(), command.getVersion(), xid);
    }
    
    /**
     * {@inheritDoc}
     */
    public void prepare(Xid xid, PreparedContext context) {
        prepareXids.put(xid, context);
    }
    
    /**
     * {@inheritDoc}
     */
    public PreparedContext getPreparedContext(Xid xid) {
        return prepareXids.get(xid);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public PreparedContext createPreparedContext() {
        return new PreparedContextImpl();
    }

    /**
     * {@inheritDoc}
     */
    public Store getUnderlyingStore() {
        return underlyingStore;
    }

    /**
     * {@inheritDoc}
     */
    public boolean resume(Xid xid) {
        XATransactionContext context = suspendXids.get(xid);
        if (context != null) {
            transactionContextXids.put(xid, context);
            return true;
        } 
        return false;
        
    }

    /**
     * {@inheritDoc}
     */
    public void suspend(Xid xid) {
        XATransactionContext context = transactionContextXids.get(xid);
        suspendXids.putIfAbsent(xid, context);
        transactionContextXids.remove(xid);
    }

    /**
     * A table containing element version information
     */
    public static class VersionTable {

        /**
         * The data underlying structure for the version info for Key (where the version is the Long)
         */
        protected final ConcurrentMap<Object, Version> versionStore = new ConcurrentHashMap<Object, Version>();

        /**
         * Checks whether a version is still up to date
         * @param key the key to check for
         * @param currentVersionNumber the version checked out
         * @return true if the version is still up to date
         */
        public synchronized boolean valid(Object key, long currentVersionNumber, Xid xid) {
            Version version = versionStore.get(key);
            if (version != null) {
                long currentVersion = version.getVersion(xid);
                return (currentVersion == currentVersionNumber);
            } else {
                // TODO Figure out what this case is..
                return true;
            }

        }

        /**
         * Track a version for an element, potentially adding it to the store
         * @param key the key matching the element in the store
         * @param xid the Xid of the Transaction
         * @return the version
         */
        public synchronized long checkout(Object key, Xid xid) {
            long versionNumber;
            Version version = versionStore.get(key);
            if (version == null) {
                version = new Version();
                versionStore.put(key, version);
            }
            versionNumber = version.checkout(xid);

            return versionNumber;
        }

        /**
         * Increment versioning information of a mutated and stored information to the store
         * @param key the key matching the element in the store
         * @param xid the Xid of the Transaction
         * @param readOnly whether the element was mutated in the Store
         */
        public synchronized void checkin(Object key, Xid xid, boolean readOnly) {
            if (key != null) {
                Version version = versionStore.get(key);
                boolean removeEntry;
                if (readOnly) {
                    removeEntry = version.checkinRead(xid);
                } else {
                    removeEntry = version.checkinWrite(xid);
                }
                if (removeEntry) {
                    versionStore.remove(key);
                }
            }
        }

    }

    /**
     * Represents an Element's version for a Store
     */
    public static class Version {

        private final AtomicLong version = new AtomicLong(0);

        // TODO We need to figure out a more compressed data-structure (need to performance test to confirm
        private final ConcurrentMap<Xid, Long> txnVersions = new ConcurrentHashMap<Xid, Long>();

        
        /**
         * For testing, get current version
         * @return the version
         */
        public synchronized long getVersion() {
            return version.get();
          }
          
        /**
         * Checks whether a Transaction already accessed the key
         * @param xid the Xid for the Transaction
         * @return true, if known
         */
        public boolean hasTransaction(Xid xid) {
            return txnVersions.containsKey(xid);
        }

        /**
         * Gets the version known to a Transaction
         * @param xid the Xis for the Transaction
         * @return the versionNumber
         */
        public long getVersion(Xid xid) {
            try {
                return txnVersions.get(xid);
            } catch (NullPointerException e) {
                throw new AssertionError("Cannot get version for not existing transaction: " + xid);
            }
        }

        /**
         * Checks out a version for a Transaction
         * @param xid the Xid for the Transaction
         * @return the version number
         */
        public long checkout(Xid xid) {
            long v = version.get();
            txnVersions.put(xid, v);
            return v;
        }

        /**
         * Read check in: the transaction is done with the element, but did not mutate it
         * @param xid the Xid for the Transaction
         * @return false if the Element is still known to other Transaction, otherwise true
         */
        public boolean checkinRead(Xid xid) {
            txnVersions.remove(xid);
            return txnVersions.isEmpty();
        }

        /**
         * Write check in: the transaction is done with the element and did mutate it
         * @param xid the Xid for the Transaction
         * @return false if the Element is still known to other Transaction, otherwise true
         */
        public boolean checkinWrite(Xid xid) {
            long v = txnVersions.remove(xid);
            version.incrementAndGet();
            return txnVersions.isEmpty();
        }

        /**
         * Getter to the underlying structure
         * @return the mapping of Xid to Version
         */
        ConcurrentMap<Xid, Long> getTxnVersions() {
            return txnVersions;
        }
    }
}
