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

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;

public class EhCacheXAStoreImpl implements EhCacheXAStore {

    private final ConcurrentMap<Transaction, Xid> localXidTable = new ConcurrentHashMap<Transaction, Xid>();    
    private final ConcurrentMap<Xid, XaTransactionContext> transactionContextXids = new ConcurrentHashMap<Xid, XaTransactionContext>();
    private final ConcurrentMap<Xid, Xid> prepareXids = new ConcurrentHashMap<Xid, Xid>();
    private final VersionTable versionTable = new VersionTable();
    private Store underlyingStore;
    private Store oldVersionStore;
    
    public EhCacheXAStoreImpl(Store underlyingStore, Store oldVersionStore) {
        this.underlyingStore = underlyingStore;
        this.oldVersionStore = oldVersionStore;
    }
    
    public Store getOldVersionStore() {
        return this.oldVersionStore;
    }

    public void checkin(Serializable key, Xid xid, boolean readOnly) {
       versionTable.checkin(key, xid, readOnly);
    }

    public long checkout(Serializable key, Xid xid) {
        return versionTable.checkout(key, xid);
    }
    
    public Xid storeXid2Transaction(Xid xid, Transaction transaction) {
        return localXidTable.putIfAbsent(transaction, xid);
    }

    public TransactionContext createTransactionContext(Transaction txn) {;
        Xid xid = localXidTable.get(txn);
        XaTransactionContext context = new XaTransactionContext(xid, this);
        context.initializeTransients(txn);
        XaTransactionContext previous = transactionContextXids.putIfAbsent(xid, context);
        if (previous != null) {
            context = previous;
        }
        return context;
    }

    public Xid[] getPreparedXids() {
        Set xidSet = prepareXids.keySet();
        return (Xid[])xidSet.toArray(new Xid[xidSet.size()]);
    }

    public TransactionContext getTransactionContext(Xid xid) {
        return transactionContextXids.get(xid);
    }

    public TransactionContext getTransactionContext(Transaction txn) {
        Xid xid = localXidTable.get(txn);
        return transactionContextXids.get(xid);
    }

    public boolean isValid(VersionAwareCommand command) {
        return versionTable.valid(command.getKey(), command.getVersion());
    }
    
    public void prepared(Xid xid) {
        prepareXids.put(xid, xid);
    }
    
    public Store getUnderlyingStore() {
        return underlyingStore;
    }


    public static class VersionTable {

        protected final ConcurrentMap<Serializable, Version> versionStore = new ConcurrentHashMap<Serializable, Version>();

        public synchronized boolean valid(Serializable key, long currentVersionNumber) {
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

        public synchronized long checkout(Serializable key, Xid xid) {
            long versionNumber = -1;
            Version version = versionStore.get(key);
            if (version == null) {
                version = new Version();
                versionStore.put(key, version);
            }
            versionNumber = version.checkout(xid);

            return versionNumber;
        }

        public synchronized void checkin(Serializable key, Xid xid, boolean readOnly) {
            Version version = versionStore.get(key);
            boolean removeEntry = false;
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

    public static class Version {

        final AtomicLong version = new AtomicLong(0);

        // TODO: We need to figure out a more compressed data-structure (need to performance test to confirm
        final ConcurrentMap<Xid, Long> txnVersions = new ConcurrentHashMap<Xid, Long>();

        public long getCurrentVersion() {
            return version.get();
        }

        public boolean hasTransaction(Xid xid) {
            return txnVersions.containsKey(xid);
        }

        public long getVersion(Xid xid) {
            try {
                return txnVersions.get(xid);
            } catch (NullPointerException e) {
                throw new AssertionError("Cannot get version for not existing transaction: " + xid);
            }
        }

        public long checkout(Xid xid) {
            long v = version.get();
            txnVersions.put(xid, v);
            return v;
        }

        public boolean checkinRead(Xid xid) {
            txnVersions.remove(xid);
            return txnVersions.isEmpty();
        }

        public boolean checkinWrite(Xid xid) {
            long v = txnVersions.remove(xid);
            version.incrementAndGet();
            return txnVersions.isEmpty();
        }
    }
}
