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

import javax.transaction.xa.Xid;

import junit.framework.TestCase;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.xa.EhcacheXAStoreImpl.Version;
import net.sf.ehcache.transaction.xa.EhcacheXAStoreImpl.VersionTable;

public class VersionTableTest extends TestCase {
    
    public void testCases() {
        
        Element element1 = new Element("key1", "value1");
        TestVersionTable table = new TestVersionTable();
        ConcurrentMap versionStore = table.getVersionStore();
        //validate clean state
        assertEquals(0, versionStore.size());
        TestXid txn1 = new TestXid();
        
        //checkout
        table.checkout(element1.getKey(), txn1);
        
        //validate readonly
        assertEquals(1, versionStore.size());
        Version version = (Version)versionStore.get(element1.getObjectKey());
        long currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        long txn1Version = version.getVersion(txn1);
        assertEquals(0, txn1Version);
        assertEquals(1, version.txnVersions.size());
        
        //checkout again
        table.checkout(element1.getKey(), txn1);
        
       //validate again
        assertEquals(1, versionStore.size());
        version = (Version)versionStore.get(element1.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        assertEquals(0, txn1Version);
        assertEquals(1, version.txnVersions.size());
        
       
         
        //now lets add the same element and new transaction
        TestXid txn2 = new TestXid();
        table.checkout(element1, txn2);
        
        assertEquals(1, versionStore.size());
        version = (Version)versionStore.get(element1.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        long txn2Version = version.getVersion(txn2);
        assertEquals(0, txn1Version);
        assertEquals(0, txn2Version);
        assertEquals(2, version.txnVersions.size());
        
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
        assertEquals(2, version.txnVersions.size());
        
        //Lets introduce a new element but still txn 1
        Element element2 = new Element("key2", "value2");
        table.checkout(element2, txn1);
        
        assertEquals(2, versionStore.size());
        version = (Version)versionStore.get(element2.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(0, currentVersionNumber);
        txn1Version = version.getVersion(txn1);
        assertEquals(0, txn1Version);
        assertEquals(1, version.txnVersions.size());
        
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
        assertEquals(2, version.txnVersions.size()); 
        
        //lets try out the checkins now, element1 txn1
        table.checkin(element1, txn1, false);
        
        assertEquals(2, versionStore.size());
        version = (Version)versionStore.get(element1.getObjectKey());
        currentVersionNumber = version.getCurrentVersion();
        assertEquals(1, currentVersionNumber);
        assertFalse(version.hasTransaction(txn1));
        txn2Version = version.getVersion(txn2);
        assertEquals(0, txn2Version);
        assertEquals(1, version.txnVersions.size()); 
        
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
        assertEquals(1, version.txnVersions.size()); 
        
     
    }
    
    private static final class TestVersionTable extends VersionTable {
        
      
        public ConcurrentMap getVersionStore() {
            return versionStore;
        }
    }
    
    
    private static final class TestXid implements Xid {
        
        public ConcurrentMap xaResourceMap = new ConcurrentHashMap();
        
        private final int hashCode;
        
        public TestXid() {
            hashCode =  UUID.randomUUID().hashCode();
        }
      
        
        

        @Override
        public int hashCode() {
            return hashCode;
        }




        public byte[] getBranchQualifier() {
            // TODO Auto-generated method stub
            return null;
        }




        public int getFormatId() {
            // TODO Auto-generated method stub
            return 0;
        }




        public byte[] getGlobalTransactionId() {
            // TODO Auto-generated method stub
            return null;
        }
        
        
        
        
        
    }

}
