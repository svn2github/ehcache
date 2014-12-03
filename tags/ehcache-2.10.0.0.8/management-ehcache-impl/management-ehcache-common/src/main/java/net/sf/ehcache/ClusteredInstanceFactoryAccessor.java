/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache;

import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;

import java.lang.reflect.Field;

/**
 * @author Ludovic Orban
 */
public class ClusteredInstanceFactoryAccessor {

  public static ClusteredInstanceFactory getClusteredInstanceFactory(CacheManager cacheManager) {
    try {
      Field field = CacheManager.class.getDeclaredField("terracottaClient");
      field.setAccessible(true);
      TerracottaClient terracottaClient = (TerracottaClient)field.get(cacheManager);
      return terracottaClient.getClusteredInstanceFactory();
    } catch (NoSuchFieldException nsfe) {
      throw new CacheException(nsfe);
    } catch (IllegalAccessException iae) {
      throw new CacheException(iae);
    }
  }

  public static void setTerracottaClient(CacheManager cacheManager, TerracottaClient terracottaClient) {
    try {
      Field field = CacheManager.class.getDeclaredField("terracottaClient");
      field.setAccessible(true);
      field.set(cacheManager, terracottaClient);
    } catch (NoSuchFieldException nsfe) {
      throw new CacheException(nsfe);
    } catch (IllegalAccessException iae) {
      throw new CacheException(iae);
    }
  }

}
