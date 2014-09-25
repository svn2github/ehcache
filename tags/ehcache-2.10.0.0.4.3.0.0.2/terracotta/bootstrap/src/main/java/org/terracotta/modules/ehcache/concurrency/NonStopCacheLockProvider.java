/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.store.ToolkitNonStopExceptionOnTimeoutConfiguration;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopException;

public class NonStopCacheLockProvider implements CacheLockProvider {
  private volatile CacheLockProvider                          delegate;
  private final NonStopFeature                                nonStop;
  private final ToolkitNonStopExceptionOnTimeoutConfiguration toolkitNonStopConfiguration;
  private final ToolkitInstanceFactory                        toolkitInstanceFactory;

  public NonStopCacheLockProvider(NonStopFeature nonStop, NonstopConfiguration nonstopConfiguration,
                                  ToolkitInstanceFactory toolkitInstanceFactory) {
    this.nonStop = nonStop;
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.toolkitNonStopConfiguration = nonstopConfiguration == null ? null
        : new ToolkitNonStopExceptionOnTimeoutConfiguration(nonstopConfiguration);
  }

  @Override
  public Sync getSyncForKey(Object key) {
    nonStop.start(toolkitNonStopConfiguration);

    try {
      throwNonStopExceptionWhenClusterNotInit();
      return new NonStopSyncWrapper(delegate.getSyncForKey(key), toolkitInstanceFactory, toolkitNonStopConfiguration);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  public void init(CacheLockProvider cacheLockProviderParam) {
    this.delegate = cacheLockProviderParam;
    synchronized (this) {
      this.notifyAll();
    }
  }

  private void throwNonStopExceptionWhenClusterNotInit() throws NonStopException {
    if (delegate == null && toolkitNonStopConfiguration != null && toolkitNonStopConfiguration.isEnabled()) {
      if (toolkitNonStopConfiguration.isImmediateTimeoutEnabled()) {
        throw new NonStopException("Cluster not up OR still in the process of connecting ");
      } else {
        long timeout = toolkitNonStopConfiguration.getTimeoutMillis();
        waitForTimeout(timeout);
      }
    }
  }

  private void waitForTimeout(long timeout) {
    synchronized (this) {
      while (delegate == null) {
        try {
          this.wait(timeout);
        } catch (InterruptedException e) {
          // TODO: remove this ... Interrupted here means aborted
          throw new NonStopException("Cluster not up OR still in the process of connecting ");
        }
      }
    }
  }
}
