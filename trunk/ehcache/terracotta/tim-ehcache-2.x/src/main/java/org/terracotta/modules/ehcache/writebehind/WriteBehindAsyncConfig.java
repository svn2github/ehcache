/**
 * Copyright 2003-2009 Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package org.terracotta.modules.ehcache.writebehind;

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.async.AsyncConfig2;

/**
 * An implementation of {@code AsyncConfig} that allows the configuration of the parameters that were set up for a
 * {@code CacheWriterManager}.
 * 
 * @author Geert Bevin
 * @version $Id: WriteBehindAsyncConfig.java 26461 2011-01-18 22:41:56Z gkeim $
 */
@InstrumentedClass
public class WriteBehindAsyncConfig implements AsyncConfig2 {
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
