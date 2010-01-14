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

package net.sf.ehcache;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class XACacheTest extends TestCase {

    CacheManager manager;

    public void testXACache() throws IllegalStateException, SecurityException, SystemException {
        Cache cache = createTestCache();
        TransactionManager txnManager = cache.getTransactionManagerLookup().getTransactionManager();
        try {
           txnManager.begin(); 
           Element element1 = new Element("key1", "value1");
           cache.put(element1);
           txnManager.commit();
        } catch (Exception e) {
            txnManager.rollback();
        }

    }

    @Override
    protected void setUp() throws Exception {
        manager = CacheManager.create();
    }

    /**
     * Creates a cache
     * 
     * @return
     */
    protected Cache createTestCache() {
        Cache cache = new Cache("sampleCache", 1000, MemoryStoreEvictionPolicy.LRU, false, null, false, 0, 0, false, 0, null, null, 0, 0,
                false, false, "SERIALIZATION", true, "xa", "net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup",
                TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION, TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION_PERIOD,
                TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE, TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE_SIZE,
                TerracottaConfiguration.DEFAULT_COPY_ON_READ);
        manager.addCache(cache);
        return cache;
    }

}