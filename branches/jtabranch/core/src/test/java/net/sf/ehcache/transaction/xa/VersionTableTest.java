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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.xa.EhCacheXAResourceImpl.Version;
import net.sf.ehcache.transaction.xa.EhCacheXAResourceImpl.VersionTable;
import junit.framework.TestCase;

public class VersionTableTest extends TestCase {
    
    public void testCases() {
        
        Element element1 = new Element("key1", "value1");
        TestVersionTable table = new TestVersionTable();
        ConcurrentMap versionStore = table.getVersionStore();
        //validate clean state
        assertEquals(0, versionStore.size());
        TestTransaction txn1 = new TestTransaction();
        
        //checkout
        table.checkout(element1, txn1);
        
        //validate readonly
        assertEquals(1, versionStore.size());
        Version version = (Version)versionStore.get(element1.getObjectKey());
        long currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        long txn1Version = version.getVersion(txn1);
        assertEquals(0, txn1Version);
        assertEquals(1, version.txnVersionMap.size());
        
        //checkout again
        table.checkout(element1, txn1);
        
       //validate again
        assertEquals(1, versionStore.size());
        version = (Version)versionStore.get(element1.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        assertEquals(0, txn1Version);
        assertEquals(1, version.txnVersionMap.size());
        
       
         
        //now lets add the same element and new transaction
        TestTransaction txn2 = new TestTransaction();
        table.checkout(element1, txn2);
        
        assertEquals(1, versionStore.size());
        version = (Version)versionStore.get(element1.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        long txn2Version = version.getVersion(txn2);
        assertEquals(0, txn1Version);
        assertEquals(0, txn2Version);
        assertEquals(2, version.txnVersionMap.size());
        
        //txn2 write
        table.checkout(element1, txn2);
        
        assertEquals(1, versionStore.size());
        version = (Version)versionStore.get(element1.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        txn2Version = version.getVersion(txn2);
        assertEquals(0, txn1Version);
        assertEquals(0, txn2Version);
        assertEquals(2, version.txnVersionMap.size());
        
        //Lets introduce a new element but still txn 1
        Element element2 = new Element("key2", "value2");
        table.checkout(element2, txn1);
        
        assertEquals(2, versionStore.size());
        version = (Version)versionStore.get(element2.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        assertEquals(0, txn1Version);
        assertEquals(1, version.txnVersionMap.size());
        
        //txn2 element2 write
        table.checkout(element2, txn2);
        
        assertEquals(2, versionStore.size());
        version = (Version)versionStore.get(element2.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        txn2Version = version.getVersion(txn2);
        assertEquals(0, txn1Version);
        assertEquals(0, txn2Version);
        assertEquals(2, version.txnVersionMap.size()); 
        
        //lets try out the checkins now, element1 txn1
        table.checkin(element1, txn1, false);
        
        assertEquals(2, versionStore.size());
        version = (Version)versionStore.get(element1.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(1, currentVersionNumber);
        assertFalse(version.hasTransaction(txn1));
        txn2Version = version.getVersion(txn2);
        assertEquals(0, txn2Version);
        assertEquals(1, version.txnVersionMap.size()); 
        
        //checkin txn2
        table.checkin(element1, txn2, false);
        
        assertEquals(1, versionStore.size());
        assertFalse(versionStore.containsKey(element1.getObjectKey())); 
        
        //checkin element 2
        table.checkin(element2, txn1, true);
        
        assertEquals(1, versionStore.size());
        version = (Version)versionStore.get(element2.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        assertFalse(version.hasTransaction(txn1));
        txn2Version = version.getVersion(txn2);
        assertEquals(0, txn2Version);
        assertEquals(1, version.txnVersionMap.size()); 
        
     
    }
    
    private static final class TestVersionTable extends VersionTable {
        
        public ConcurrentMap getVersionStore() {
            return versionStore;
        }
    }
    
    
    private static final class TestTransaction implements Transaction {
        
        public ConcurrentMap xaResourceMap = new ConcurrentHashMap();
        
        private final int hashCode;
        
        public TestTransaction() {
            hashCode =  UUID.randomUUID().hashCode();
        }
        
        
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
                IllegalStateException, SystemException {
            throw new UnsupportedOperationException("not used for this test");
        }

        public boolean delistResource(XAResource resource, int arg1) throws IllegalStateException, SystemException {
            return (xaResourceMap.remove(resource) == null) ? true : false;
        }

        public boolean enlistResource(XAResource resource) throws RollbackException, IllegalStateException, SystemException {
            return (xaResourceMap.put(resource, resource) == null) ? true : false;
        }

        public int getStatus() throws SystemException {
            throw new UnsupportedOperationException("not used for this test");
        }

        public void registerSynchronization(Synchronization arg0) throws RollbackException, IllegalStateException, SystemException {
            throw new UnsupportedOperationException("not used for this test");
        }

        public void rollback() throws IllegalStateException, SystemException {
            throw new UnsupportedOperationException("not used for this test");
        }

        public void setRollbackOnly() throws IllegalStateException, SystemException {
            throw new UnsupportedOperationException("not used for this test");
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
        
        
        
        
        
    }

}
