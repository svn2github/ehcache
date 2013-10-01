/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.constructs.nonstop.concurrency.InvalidLockStateAfterRejoinException;
import net.sf.ehcache.constructs.nonstop.concurrency.LockOperationTimedOutNonstopException;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.store.ToolkitNonStopExceptionOnTimeoutConfiguration;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.rejoin.RejoinException;

public class NonStopSyncWrapper implements Sync {
  private final Sync                                          delegate;
  private final NonStopFeature                                nonStop;
  private final ToolkitNonStopExceptionOnTimeoutConfiguration toolkitNonStopConfiguration;

  public NonStopSyncWrapper(Sync delegate, ToolkitInstanceFactory toolkitInstanceFactory,
                            ToolkitNonStopExceptionOnTimeoutConfiguration toolkitNonStopConfiguration) {
    this.delegate = delegate;
    this.nonStop = toolkitInstanceFactory.getToolkit().getFeature(ToolkitFeatureType.NONSTOP);
    this.toolkitNonStopConfiguration = toolkitNonStopConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void lock(LockType type) {
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.lock(type);
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
      this.delegate.unlock(type);
    } catch (RejoinException e) {
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
      return this.delegate.tryLock(type, msec);
    } catch (NonStopException e) {
      throw new LockOperationTimedOutNonstopException("try lock timed out");
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
      return this.delegate.isHeldByCurrentThread(type);
    } catch (NonStopException e) {
      throw new LockOperationTimedOutNonstopException("isHeldByCurrentThread timed out");
    } finally {
      nonStop.finish();
    }
  }
}
