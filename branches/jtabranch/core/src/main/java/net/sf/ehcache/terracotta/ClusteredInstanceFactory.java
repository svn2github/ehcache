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
package net.sf.ehcache.terracotta;

import javax.transaction.TransactionManager;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.xa.EhCacheXAResource;
import net.sf.ehcache.writebehind.WriteBehind;

/**
 * Factory for creating clustered instances
 *
 * @author teck
 * @author gbevin
 * @since 1.7
 */
public interface ClusteredInstanceFactory {

  /**
   * Create a Store instance for the given cache
   *
   * @param cache the cache will backed by the returned store
   * @return store instance
   */
  Store createStore(Ehcache cache);

  /**
   * Create an WriteBehind instance for the given cache
   *
   * @param cache the cache will backed by the returned store
   * @return write behind instance
   */
  WriteBehind createAsync(Ehcache cache);
  
  
  /**
   * 
   * @param cacheName
   * @param store
   * @param txnManager
   * @return
   */
  EhCacheXAResource createXAResource(Ehcache cache, Store store, TransactionManager txnManager);
}
