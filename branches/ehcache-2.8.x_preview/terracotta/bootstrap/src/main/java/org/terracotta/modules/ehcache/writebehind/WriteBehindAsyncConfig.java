/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import org.terracotta.modules.ehcache.async.AsyncConfig;

public class WriteBehindAsyncConfig implements AsyncConfig {
  private final long    workDelay;
  private final long    maxFallBehind;
  private final boolean batchingEnabled;
  private final int     batchSize;
  private final boolean synchronousWrite;
  private final int     retryAttempts;
  private final long    retryAttemptDelay;
  private final int     rateLimit;
  private final int     maxQueueSize;

  public WriteBehindAsyncConfig(long workDelay, long maxAllowedFallBehind, boolean batchingEnabled, int batchSize,
                                boolean synchronousWrite, int retryAttempts, long retryAttemptDelay, int rateLimit,
                                final int maxQueueSize) {
    this.workDelay = workDelay;
    this.maxFallBehind = maxAllowedFallBehind;
    this.batchingEnabled = batchingEnabled;
    this.batchSize = batchSize;
    this.synchronousWrite = synchronousWrite;
    this.retryAttempts = retryAttempts;
    this.retryAttemptDelay = retryAttemptDelay;
    this.rateLimit = rateLimit;
    this.maxQueueSize = maxQueueSize;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + batchSize;
    result = prime * result + (batchingEnabled ? 1231 : 1237);
    result = prime * result + (int) (maxFallBehind ^ (maxFallBehind >>> 32));
    result = prime * result + maxQueueSize;
    result = prime * result + rateLimit;
    result = prime * result + (int) (retryAttemptDelay ^ (retryAttemptDelay >>> 32));
    result = prime * result + retryAttempts;
    result = prime * result + (synchronousWrite ? 1231 : 1237);
    result = prime * result + (int) (workDelay ^ (workDelay >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    WriteBehindAsyncConfig other = (WriteBehindAsyncConfig) obj;
    if (batchSize != other.batchSize) return false;
    if (batchingEnabled != other.batchingEnabled) return false;
    if (maxFallBehind != other.maxFallBehind) return false;
    if (maxQueueSize != other.maxQueueSize) return false;
    if (rateLimit != other.rateLimit) return false;
    if (retryAttemptDelay != other.retryAttemptDelay) return false;
    if (retryAttempts != other.retryAttempts) return false;
    if (synchronousWrite != other.synchronousWrite) return false;
    if (workDelay != other.workDelay) return false;
    return true;
  }

  @Override
  public String toString() {
    return "WriteBehindAsyncConfig [workDelay=" + workDelay + ", maxFallBehind=" + maxFallBehind + ", batchingEnabled="
           + batchingEnabled + ", batchSize=" + batchSize + ", synchronousWrite=" + synchronousWrite
           + ", retryAttempts=" + retryAttempts + ", retryAttemptDelay=" + retryAttemptDelay + ", rateLimit="
           + rateLimit + ", maxQueueSize=" + maxQueueSize + "]";
  }

  public long getWorkDelay() {
    return workDelay;
  }

  public long getMaxAllowedFallBehind() {
    return maxFallBehind;
  }

  public boolean isStealingEnabled() {
    return false;
  }

  public boolean isBatchingEnabled() {
    return batchingEnabled;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public int getMaxQueueSize() {
    return maxQueueSize;
  }

  public boolean isSynchronousWrite() {
    return synchronousWrite;
  }

  public int getRetryAttempts() {
    return retryAttempts;
  }

  public long getRetryAttemptDelay() {
    return retryAttemptDelay;
  }

  public int getRateLimit() {
    return rateLimit;
  }
}
