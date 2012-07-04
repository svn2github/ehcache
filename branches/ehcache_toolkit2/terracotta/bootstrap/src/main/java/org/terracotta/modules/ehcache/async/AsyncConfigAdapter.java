/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

public abstract class AsyncConfigAdapter implements AsyncConfig {

  private final AsyncConfig delegate;

  public AsyncConfigAdapter(AsyncConfig delegate) {
    this.delegate = delegate;
  }

  @Override
  public long getWorkDelay() {
    return delegate.getWorkDelay();
  }

  @Override
  public long getMaxAllowedFallBehind() {
    return delegate.getMaxAllowedFallBehind();
  }

  @Override
  public int getBatchSize() {
    return delegate.getBatchSize();
  }

  @Override
  public boolean isBatchingEnabled() {
    return delegate.isBatchingEnabled();
  }

  @Override
  public boolean isSynchronousWrite() {
    return delegate.isSynchronousWrite();
  }

  @Override
  public int getRetryAttempts() {
    return delegate.getRetryAttempts();
  }

  @Override
  public long getRetryAttemptDelay() {
    return delegate.getRetryAttemptDelay();
  }

  @Override
  public int getRateLimit() {
    return delegate.getRateLimit();
  }

  @Override
  public int getMaxQueueSize() {
    return delegate.getMaxQueueSize();
  }


}
