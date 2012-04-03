package org.terracotta.ehcache.tests;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractWriteBehindClient extends ClientBase {
  private final AtomicLong writeCount = new AtomicLong();
  private final AtomicLong deleteCount = new AtomicLong();

  public AbstractWriteBehindClient(String[] args) {
    super("test", args);
  }

  public void incrementWriteCount() {
    writeCount.incrementAndGet();
  }

  public long getWriteCount() {
    return writeCount.longValue();
  }

  public void incrementDeleteCount() {
    deleteCount.incrementAndGet();
  }

  public long getDeleteCount() {
    return deleteCount.longValue();
  }

  public abstract long getSleepBetweenWrites();
  public abstract long getSleepBetweenDeletes();
}