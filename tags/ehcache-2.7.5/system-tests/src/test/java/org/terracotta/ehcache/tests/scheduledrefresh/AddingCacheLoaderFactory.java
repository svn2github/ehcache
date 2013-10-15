package org.terracotta.ehcache.tests.scheduledrefresh;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CacheLoaderFactory;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: cschanck
 * Date: 5/31/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddingCacheLoaderFactory extends CacheLoaderFactory {
  @Override
  public CacheLoader createCacheLoader(Ehcache cache, Properties properties) {
    return new AddingCacheLoader();
  }
}
