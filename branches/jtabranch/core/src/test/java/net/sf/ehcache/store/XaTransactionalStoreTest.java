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

package net.sf.ehcache.store;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import junit.framework.TestCase;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.transaction.TransactionContext;
import net.sf.ehcache.transaction.xa.EhCacheXAResource;
import net.sf.ehcache.transaction.xa.EhCacheXAStoreImpl.VersionTable;

public class XaTransactionalStoreTest extends TestCase {

    public void testCases() {
        TestEhCacheXAResource resource = new TestEhCacheXAResource();
        XaTransactionalStore store = new XaTransactionalStore(resource);
        TestTransaction txn = new TestTransaction();
    //    resource.txnContext = new XaTransactionContext(txn);
        
        Element element1  = new Element("key1", "value1");
        store.put(element1);
        
        assertEquals(1, resource.checkoutMap.size());
     //   assertEquals(1, resource.txnContext.getCommands().size());
        
        Element element2 = new Element("key2", "value2"); 
        
        
    }

    private static final class TestEhCacheXAResource implements EhCacheXAResource {

        private final TestStore store = new TestStore();
        public  final ConcurrentMap checkoutMap = new ConcurrentHashMap();
        public final VersionTable versionTable = new VersionTable();
        public TransactionContext txnContext;


        public long checkout(Element element, Xid xid) {
             checkoutMap.put(element, xid);
             return versionTable.checkout(element, xid);
        }

        public String getCacheName() {
            return null;
        }

        public TransactionContext getOrCreateTransactionContext() throws SystemException, RollbackException {
            return txnContext;
        }

        public void put(final Element element) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Element get(final Object key) {
            return null;
        }

        public Element getQuiet(final Object key) {
            return null;
        }

        public Store getStore() {
            return store;
        }

        public void commit(Xid xid, boolean onePhase) throws XAException {
        }

        public void end(Xid xid, int flags) throws XAException {
        }

        public void forget(Xid xid) throws XAException {
        }

        public int getTransactionTimeout() throws XAException {
            return 0;
        }

        public boolean isSameRM(XAResource xares) throws XAException {
            return false;
        }

        public int prepare(Xid xid) throws XAException {
            return 0;
        }

        public Xid[] recover(int flag) throws XAException {
            return null;
        }

        public void rollback(Xid xid) throws XAException {
        }

        public boolean setTransactionTimeout(int seconds) throws XAException {
            return false;
        }

        public void start(Xid xid, int flags) throws XAException {
        }

    }
    
    private static final class TestTransaction implements Transaction {
        
        private final ConcurrentMap resourceMap = new ConcurrentHashMap();

        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
                IllegalStateException, SystemException {
        }

        public boolean delistResource(XAResource resource, int arg1) throws IllegalStateException, SystemException {
            return resourceMap.remove(resource) != null;     
        }

        public boolean enlistResource(XAResource resource) throws RollbackException, IllegalStateException, SystemException {
            return resourceMap.put(resource, resource) != null;
        }

        public int getStatus() throws SystemException {
            return 0;
        }

        public void registerSynchronization(Synchronization arg0) throws RollbackException, IllegalStateException, SystemException {
        }

        public void rollback() throws IllegalStateException, SystemException {
        }

        public void setRollbackOnly() throws IllegalStateException, SystemException {
        }
        
    }

    private static final class TestStore implements Store {

        public static final Set putSet = new HashSet();
        public static final Set removeSet = new HashSet();
        public static final Set containsKeySet = new HashSet();
        public static final Set getSet = new HashSet();
        public static final Set getQuietSet = new HashSet();

        public void put(Element element) throws CacheException {
            putSet.add(element);
        }

        public Element remove(Object key) {
            removeSet.add(key);
            return null;
        }

        public boolean containsKey(Object key) {
            containsKeySet.add(key);
            return false;
        }

        public Element get(Object key) {
            getSet.add(key);
            return null;
        }

        public Element getQuiet(Object key) {
            getQuietSet.add(key);
            return null;
        }

        public boolean bufferFull() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public void dispose() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public void expireElements() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public void flush() throws IOException {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public Policy getEvictionPolicy() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public Object getInternalContext() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public Object[] getKeyArray() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public int getSize() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public long getSizeInBytes() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public Status getStatus() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public int getTerracottaClusteredSize() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public boolean isCacheCoherent() {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public void removeAll() throws CacheException {
            throw new UnsupportedOperationException("unsupported for this test");
        }

        public void setEvictionPolicy(Policy policy) {
            throw new UnsupportedOperationException("unsupported for this test");
        }
    }
}
