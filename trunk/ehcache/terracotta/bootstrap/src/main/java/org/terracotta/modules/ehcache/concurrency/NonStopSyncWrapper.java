/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.constructs.nonstop.concurrency.InvalidLockStateAfterRejoinException;
import net.sf.ehcache.constructs.nonstop.concurrency.LockOperationTimedOutNonstopException;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.store.ToolkitNonStopExceptionOnTimeoutConfiguration;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class NonStopSyncWrapper implements Sync {
  private volatile Sync                                       delegate;
  private final Future<CacheLockProvider>                     cacheLockProvider;
  private final NonStopFeature                                       nonStop;
  private final ToolkitNonStopExceptionOnTimeoutConfiguration toolkitNonStopConfiguration;
  private volatile Object                                     key;

  public NonStopSyncWrapper(Future<CacheLockProvider> cacheLockProvider, Object key,
                            ToolkitInstanceFactory toolkitInstanceFactory,
                            ToolkitNonStopExceptionOnTimeoutConfiguration toolkitNonStopConfiguration) {
    this.cacheLockProvider = cacheLockProvider;
    this.nonStop = toolkitInstanceFactory.getToolkit().getFeature(ToolkitFeatureType.NONSTOP);
    this.toolkitNonStopConfiguration = toolkitNonStopConfiguration;
  }

  private Sync getDelegate() {
    if (delegate == null) {
      synchronized (this) {
        if (delegate == null) {
          delegate = getCacheLockProvider().getSyncForKey(key);
          key = null;
        }
      }
    }

    return delegate;
  }

  private CacheLockProvider getCacheLockProvider() {
    boolean isInterrupted = false;
    while (true) {

      try {
        if (!cacheLockProvider.isDone() && toolkitNonStopConfiguration.isEnabled()
            && toolkitNonStopConfiguration.isImmediateTimeoutEnabled()) { throw new NonStopException(); }

        if (isInterrupted) {
          Thread.currentThread().interrupt();
        }
        return cacheLockProvider.get();
      } catch (InterruptedException e) {
        if (toolkitNonStopConfiguration.isEnabled()) {
          throw new NonStopException();
        } else {
          // TODO: do this properly
          isInterrupted = true;
        }
      } catch (ExecutionException e) {
        if (toolkitNonStopConfiguration.isEnabled()) {
          throw new NonStopException();
        } else {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void lock(LockType type) {
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.getDelegate().lock(type);
    } catch (NonStopException e) {
      throw new LockOperationTimedOutNonstopException("Lock timed out");
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlock(LockType type) {
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.getDelegate().unlock(type);
    } catch (org.terracotta.toolkit.rejoin.InvalidLockStateAfterRejoinException e) {
      throw new InvalidLockStateAfterRejoinException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean tryLock(LockType type, long msec) throws InterruptedException {
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.getDelegate().tryLock(type, msec);
    } catch (NonStopException e) {
      return false;
      // throw new LockOperationTimedOutNonstopException("try lock timed out");
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isHeldByCurrentThread(LockType type) {
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.getDelegate().isHeldByCurrentThread(type);
    } catch (NonStopException e) {
      throw new LockOperationTimedOutNonstopException("isHeldByCurrentThread timed out");
    } finally {
      nonStop.finish();
    }
  }
}
