/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.NonstopConfiguration;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.store.ToolkitNonStopExceptionOnTimeoutConfiguration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NonStopCacheLockProvider implements CacheLockProvider {
  private volatile CacheLockProvider                          delegate;
  private final ToolkitNonStopExceptionOnTimeoutConfiguration toolkitNonStopConfiguration;
  private final ToolkitInstanceFactory                        toolkitInstanceFactory;
  private final CacheLockProviderFuture                       cacheLockProviderFuture = new CacheLockProviderFuture();

  public NonStopCacheLockProvider(NonstopConfiguration nonstopConfiguration,
                                  ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.toolkitNonStopConfiguration = nonstopConfiguration == null ? null
        : new ToolkitNonStopExceptionOnTimeoutConfiguration(nonstopConfiguration);
  }

  @Override
  public Sync getSyncForKey(Object key) {
    return new NonStopSyncWrapper(cacheLockProviderFuture, key, toolkitInstanceFactory, toolkitNonStopConfiguration);
  }

  public void init(CacheLockProvider cacheLockProviderParam) {
    this.delegate = cacheLockProviderParam;

    synchronized (this) {
      this.notifyAll();
    }
  }

  private synchronized void waitForTimeout(long timeout) throws InterruptedException {
    long timeInit = System.currentTimeMillis();
    while (delegate == null) {
      long timeRemaining = System.currentTimeMillis() - timeInit;
      if (timeRemaining >= timeout) { return; }
      this.wait(timeout);
    }
  }

  private class CacheLockProviderFuture implements Future<CacheLockProvider> {
    @Override
    public boolean cancel(boolean arg0) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CacheLockProvider get() throws InterruptedException, ExecutionException {
      try {
        return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        throw new ExecutionException(e);
      }
    }

    @Override
    public CacheLockProvider get(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
      if (delegate == null) {
        synchronized (NonStopCacheLockProvider.this) {
          if (delegate == null) {
            waitForTimeout(unit.toMillis(time));
          }
        }
        if (delegate == null) { throw new TimeoutException(); }
      }

      return delegate;
    }

    @Override
    public boolean isCancelled() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
      return delegate != null;
    }
  }
}
