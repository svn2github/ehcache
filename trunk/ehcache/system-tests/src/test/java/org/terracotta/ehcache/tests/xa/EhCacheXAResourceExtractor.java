/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.xa;

import net.sf.ehcache.Cache;
import net.sf.ehcache.transaction.xa.XATransactionStore;

import javax.transaction.xa.XAResource;
import java.lang.reflect.Field;

public class EhCacheXAResourceExtractor {

    public static XAResource extractXAResource(Cache cache) {
        try {
            XATransactionStore store = (XATransactionStore) getPrivateField(cache, "compoundStore");
            return store.getOrCreateXAResource();
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
