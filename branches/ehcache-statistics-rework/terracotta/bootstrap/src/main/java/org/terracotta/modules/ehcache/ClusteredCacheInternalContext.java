/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.Sync;

import org.terracotta.toolkit.Toolkit;

public class ClusteredCacheInternalContext implements ToolkitLookup, CacheLockProvider {
  private final Toolkit           toolkit;
  private final CacheLockProvider cacheLockProvider;

  public ClusteredCacheInternalContext(Toolkit toolkit, CacheLockProvider cacheLockProvider) {
    this.toolkit = toolkit;
    this.cacheLockProvider = cacheLockProvider;
  }

  @Override
  public Sync getSyncForKey(Object key) {
    return cacheLockProvider.getSyncForKey(key);
  }

  @Override
  public Toolkit getToolkit() {
    return toolkit;
  }

  public CacheLockProvider getCacheLockProvider() {
    return cacheLockProvider;
  }
}
