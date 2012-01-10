package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.AbstractCacheWriter;

import com.tc.simulator.app.Application;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class WriteBehindCacheWriter extends AbstractCacheWriter {
  private final String name;
  private final Application app;
  private final long sleepBetweenOperations;
  private final AtomicLong writeCount = new AtomicLong();
  private final AtomicLong deleteCount = new AtomicLong();
  private final boolean silent;

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

  public WriteBehindCacheWriter(String name, Application app, long sleepBetweenOperations) {
    this(name, app, sleepBetweenOperations, false);
  }

  public WriteBehindCacheWriter(String name, Application app, long sleepBetweenOperations, boolean silent) {
    this.name = name;
    this.app = app;
    this.sleepBetweenOperations = sleepBetweenOperations;
    this.silent = silent;
  }

  public void write(Element element) throws CacheException {
    incrementWriteCount();
    if (!silent) {
      System.err.println("[" + name + " written " + getWriteCount() + " for " + app.getClass().getName() + " " + app.getApplicationId() + "]");
    }
    try {
      Thread.sleep(sleepBetweenOperations);
    } catch (InterruptedException e) {
      // no-op
    }
  }

  public void writeAll(Collection<Element> elements) throws CacheException {
    throw new UnsupportedOperationException();
  }

  public void delete(CacheEntry entry) throws CacheException {
    incrementDeleteCount();
    System.err.println("[" + name + " deleted " + getDeleteCount() + " for " + app.getClass().getName() + " " + app.getApplicationId() + "]");
    try {
      Thread.sleep(sleepBetweenOperations);
    } catch (InterruptedException e) {
      // no-op
    }
  }

  @Override
  public void dispose() throws CacheException {
    System.err.println("Shutting " + this.getClass().getSimpleName() + " named '" + name + "' down...");
  }
}