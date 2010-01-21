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

import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;


public interface EhCacheXAStore {
    
    Xid storeXid2Transaction(Xid xid, Transaction transaction);
    
    TransactionContext createTransactionContext(Transaction txn);
    
    TransactionContext getTransactionContext(Xid xid);
   
    TransactionContext getTransactionContext(Transaction txn);
    
    void checkin(Object key, Xid xid, boolean readOnly);
    
    long checkout(Object key, Xid xid);
    
    boolean isValid(VersionAwareCommand command);
    
    void prepared(Xid xid);

    Xid[] getPreparedXids();
    
    Store getUnderlyingStore();
    
    Store getOldVersionStore();
}
