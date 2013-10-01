/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.osgi;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshCacheExtension;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshConfiguration;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.extension.CacheExtensionFactory;

import java.util.Properties;

public class TestScheduledRefreshFactory extends CacheExtensionFactory {

  @Override
  public CacheExtension createCacheExtension(Ehcache cache, Properties properties) {
    ScheduledRefreshConfiguration conf = new ScheduledRefreshConfiguration().fromProperties(properties).build();
    return new ScheduledRefreshCacheExtension(conf, cache);
  }
}
