/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.xa;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.TerracottaTransactionalCopyingCacheStore;
import net.sf.ehcache.transaction.xa.XATransactionStore;

import org.terracotta.modules.ehcache.store.nonstop.NonStopStoreWrapper;

import java.lang.reflect.Field;

import javax.transaction.xa.XAResource;

public class EhCacheXAResourceExtractor {

  public static XAResource extractXAResource(Cache cache) {
    try {
      CacheStoreHelper helper = new CacheStoreHelper(cache);
      Store store = helper.getStore();
      if (store instanceof NonStopStoreWrapper) {
        store = (Store) getPrivateField(store, "delegate");
      }
      return ((XATransactionStore) ((TerracottaTransactionalCopyingCacheStore)store).getUnderlyingStore()).getOrCreateXAResource();
    } catch (Exception e) {
      throw new RuntimeException("cannot extract XAResource out of cache " + cache, e);
    }
  }

  private static Object getPrivateField(Object o, String fieldName) throws NoSuchFieldException, IllegalAccessException {
    Field field = o.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(o);
  }

}
