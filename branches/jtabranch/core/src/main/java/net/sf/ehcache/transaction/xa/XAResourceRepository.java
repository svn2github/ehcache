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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

public class XAResourceRepository {
    
    private final Map<String, EhCacheXAResource> xaResources = new ConcurrentHashMap<String, EhCacheXAResource>();
    private final TransactionManagerLookup transactionManagerLookup;

    public XAResourceRepository(TransactionManagerLookup transactionManagerLookup) {
        this.transactionManagerLookup = transactionManagerLookup;
    }
    
    
    public EhCacheXAResource getOrCreateXAResource(String cacheName, Store store) {
        EhCacheXAResource resource = xaResources.get(cacheName);
        if(resource == null) {
            resource = new EhCacheXAResourceImpl(cacheName, store, transactionManagerLookup.getTransactionManager());
            transactionManagerLookup.register(resource);
            xaResources.put(cacheName, resource);
        }
        return resource;
    }
}
