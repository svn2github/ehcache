package org.terracotta.ehcache.tests;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;

import java.util.Properties;

public class EhcacheTerracottaEventListenerFactory extends CacheEventListenerFactory {
  @Override
  public CacheEventListener createCacheEventListener(Properties properties) {
    return new EhcacheTerracottaEventListener();
  }
}
