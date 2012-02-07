package org.terracotta.ehcache.tests;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.CacheDecoratorFactory;

import java.util.Properties;

public class NonHibernateCacheDecoratorFactory extends CacheDecoratorFactory {

  @Override
  public Ehcache createDecoratedEhcache(Ehcache cache, Properties properties) {
    NonHibernateCacheDecorator cacheDecorator = new NonHibernateCacheDecorator(cache);
    return cacheDecorator;
  }

  @Override
  public Ehcache createDefaultDecoratedEhcache(Ehcache cache, Properties properties) {
    NonHibernateCacheDecorator cacheDecorator = new NonHibernateCacheDecorator(cache);
    return cacheDecorator;
  }

}
