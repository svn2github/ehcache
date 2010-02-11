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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import junit.framework.TestCase;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.store.LruPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.StorePutCommand;
import net.sf.ehcache.transaction.TransactionContext;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * 
 * @author nelrahma
 *
 */
public class EhcacheXAResourceTest extends TestCase {

    public void test() {
        TestStore underlyingStore = new TestStore();
        TestStore oldVersionStore = new TestStore();
        TestTransactionManager txnManager = new TestTransactionManager();
        EhcacheXAStoreImpl store = new EhcacheXAStoreImpl(underlyingStore, oldVersionStore);
        Ehcache theCache = mock(Ehcache.class);
        when(theCache.getName()).thenReturn("testCache");
        EhcacheXAResourceImpl resource = new EhcacheXAResourceImpl(theCache, txnManager, store);
        TestXid xid1 = new TestXid(1L);
        try {
            resource.start(xid1, XAResource.TMNOFLAGS);
            fail("XAException if no transaction started by txnManager");
        } catch (XAException e) {
            assertTrue(e.getMessage().contains("Couldn't get to current Transaction"));
        }
        
        TestTransaction testTxn = new TestTransaction();
        txnManager.txn = testTxn;

        try {
            resource.start(xid1, XAResource.TMNOFLAGS);
        } catch (XAException e) {
            fail("XAException if no transaction started by txnManager");
        }

        try {
            resource.start(xid1, XAResource.TMJOIN);
        } catch (XAException e) {
            fail("shouldn't throw exception since txn is available.");
        }
        assertEquals(1, store.localTxn2XidTable.size());
        assertEquals(1, store.localXid2TxnTable.size());

        try {
            resource.end(xid1, XAResource.TMSUCCESS);
        } catch (XAException e) {
            fail("Shoudn't throw an exception..");
        }

        try {
            resource.end(xid1, XAResource.TMFAIL);
            assertEquals(0, store.localTxn2XidTable.size());
            assertEquals(0, store.localXid2TxnTable.size());
        } catch (XAException e) {
            fail("Shoudn't throw an exception..");
        }

        testTxn = new TestTransaction();
        txnManager.txn = testTxn;

        try {
            resource.start(xid1, XAResource.TMONEPHASE);
        } catch (XAException e) {
            fail("shouldn't throw exception since txn is available.");
        }
        assertEquals(1, store.localTxn2XidTable.size());
        assertEquals(1, store.localXid2TxnTable.size());

        try {
            resource.end(xid1, XAResource.TMSUCCESS);
        } catch (XAException e) {
            fail("Shoudn't throw an exception..");
        }

        TransactionContext context = null;
        try {
            context = resource.getOrCreateTransactionContext();
        } catch (SystemException e) {
            fail("Shoudn't throw an exception..");
        } catch (RollbackException e) {
            fail("Shoudn't throw an exception..");
        }
        
        assertEquals(1, store.transactionContextXids.size());

        Element element = new Element("key1", "value1");

        context.addCommand(new StorePutCommand(element), element);
        assertEquals(1, testTxn.resources.size());
        assertEquals(0, underlyingStore.getSize());
        assertEquals(0, oldVersionStore.getSize());
        assertEquals(1, context.getCommands().size());

        try {
            resource.prepare(xid1);
        } catch (XAException e) {
            fail("Shoudn't throw an exception..");
        }

        assertEquals(1, underlyingStore.getSize());
        assertEquals(0, oldVersionStore.getSize());
        assertEquals(1, store.prepareXids.size());
        
        Xid [] recoverXids = null;
        try {
            recoverXids = resource.recover(XAResource.TMSUCCESS);
        } catch (XAException e) {
            fail("Shoudn't throw an exception..");
        }

        
        assertEquals(1, recoverXids.length);
        try {
            resource.commit(xid1, false);
        } catch (XAException e) {
            assertTrue(e.getMessage().contains("has been heuristically rolled back"));
            try {
                resource.rollback(xid1);
            } catch (XAException e1) {
                fail("Shoudn't throw an exception..");
            }
        }

        assertEquals(1, underlyingStore.getSize());
        assertEquals(0, oldVersionStore.getSize());
        assertEquals(0, store.transactionContextXids.size());
        assertEquals(0, store.prepareXids.size());
        assertEquals(0, store.localTxn2XidTable.size());
        assertEquals(0, store.localXid2TxnTable.size());
        
    }

    public static final class TestXid implements Xid {
        private final Long id;
        private final byte[] globalTransactionId;
        private final byte[] branchQualifier;
        private final int formatId;

        public TestXid(Long id) {
            this.id = id;
            this.globalTransactionId = new byte[] { id.byteValue() };
            this.branchQualifier = new byte[] { 0, 1, 1 };
            this.formatId = 2;
        }

        public byte[] getBranchQualifier() {
            return this.branchQualifier;
        }

        public int getFormatId() {
            return formatId;
        }

        public byte[] getGlobalTransactionId() {
            return globalTransactionId;
        }

        @Override
        public int hashCode() {
            int result = formatId;
            result = 31 * result + (globalTransactionId != null ? Arrays.hashCode(globalTransactionId) : 0);
            result = 31 * result + (branchQualifier != null ? Arrays.hashCode(branchQualifier) : 0);
            return result;
        }

        @Override
        public String toString() {
            return "XidClustered{" + "formatId=" + formatId + ", globalTxId=" + Arrays.toString(globalTransactionId) + ", branchQualifier="
                    + Arrays.toString(branchQualifier) + '}';
        }
    }

    // Test Transaction
    private static final class TestTransaction implements Transaction {

        private int hashCode = UUID.randomUUID().hashCode();

        Set<XAResource> resources = new HashSet();

        @Override
        public int hashCode() {
            return hashCode;
        }

        public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SecurityException,
                SystemException {
            // TODO Auto-generated method stub

        }

        public boolean delistResource(XAResource xaresource, int i) throws IllegalStateException, SystemException {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean enlistResource(XAResource xaresource) throws IllegalStateException, RollbackException, SystemException {
            return resources.add(xaresource);
        }

        public int getStatus() throws SystemException {
            // TODO Auto-generated method stub
            return 0;
        }

        public void registerSynchronization(Synchronization synchronization) throws IllegalStateException, RollbackException,
                SystemException {
            // TODO Auto-generated method stub

        }

        public void rollback() throws IllegalStateException, SystemException {
            // TODO Auto-generated method stub

        }

        public void setRollbackOnly() throws IllegalStateException, SystemException {
            // TODO Auto-generated method stub

        }

    }

    // Test Transaction Manager
    private static final class TestTransactionManager implements TransactionManager {

        public TestTransaction txn;

        public void begin() throws NotSupportedException, SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }

        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException,
                SecurityException, SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }

        public int getStatus() throws SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }

        public Transaction getTransaction() throws SystemException {
            return txn;
        }

        public void resume(Transaction arg0) throws IllegalStateException, InvalidTransactionException, SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }

        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }

        public void setRollbackOnly() throws IllegalStateException, SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }

        public void setTransactionTimeout(int arg0) throws SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }

        public Transaction suspend() throws SystemException {
            throw new UnsupportedOperationException("method not supported for this unit test");
        }
    }

    private static class TestStore implements Store {

        final static LruPolicy LRU_POLICY = new LruPolicy();
        final ConcurrentMap<Object, Element> storeMap = new ConcurrentHashMap<Object, Element>();

        public boolean bufferFull() {

            return false;
        }

        public boolean containsKey(Object key) {
            return storeMap.containsKey(key);
        }

        public void dispose() {
            //
        }

        public void expireElements() {
            //
        }

        @SuppressWarnings("all")
        public void flush() throws IOException {
            if (false) {
                throw new IOException();
            }
        }

        public Element get(Object key) {
            return storeMap.get(key);
        }

        public Policy getEvictionPolicy() {
            return LRU_POLICY;
        }

        public Object getInternalContext() {
            return null;
        }

        public Object[] getKeyArray() {
            return storeMap.keySet().toArray();
        }

        public Element getQuiet(Object key) {
            return storeMap.get(key);
        }

        public int getSize() {
            return storeMap.size();
        }

        public long getSizeInBytes() {
            return storeMap.size();
        }

        public Status getStatus() {
            return null;
        }

        public int getTerracottaClusteredSize() {
            return storeMap.size();
        }

        public boolean isCacheCoherent() {
            return false;
        }

        public boolean isClusterCoherent() {
            return false;
        }

        public boolean isNodeCoherent() {
            return false;
        }

        public void put(Element element) throws CacheException {
            storeMap.put(element.getObjectKey(), element);
        }

        public Element remove(Object key) {
            return storeMap.remove(key);
        }

        public void removeAll() throws CacheException {
            storeMap.clear();
        }

        public void setEvictionPolicy(Policy policy) {
            //
        }

        public void putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
            //
        }

        public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
            return null;
        }

        public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
            // 
        }

        public void waitUntilClusterCoherent() throws UnsupportedOperationException {
            //
        }

    }

}
