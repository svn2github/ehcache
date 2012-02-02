package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.AbstractCacheWriter;

import java.util.Collection;

public class WriteBehindCacheWriter extends AbstractCacheWriter {
  private final AbstractWriteBehindClient writeBehindClient;

  public WriteBehindCacheWriter(AbstractWriteBehindClient writeBehindClient) {
    this.writeBehindClient = writeBehindClient;
  }

  public void write(Element element) throws CacheException {
    writeBehindClient.incrementWriteCount();
    System.err.println("[WriteBehindCacheWriter written " + writeBehindClient.getWriteCount() + " for " + writeBehindClient.getClass().getName() + "]");
    try {
      Thread.sleep(writeBehindClient.getSleepBetweenWrites());
    } catch (InterruptedException e) {
      // no-op
    }
  }

  public void writeAll(Collection<Element> elements) throws CacheException {
    throw new UnsupportedOperationException();
  }

  public void delete(CacheEntry entry) throws CacheException {
    writeBehindClient.incrementDeleteCount();
    System.err.println("[WriteBehindCacheWriter deleted " + writeBehindClient.getDeleteCount() + " for " + writeBehindClient.getClass().getName() + "]");
    try {
      Thread.sleep(writeBehindClient.getSleepBetweenDeletes());
    } catch (InterruptedException e) {
      // no-op
    }
  }
}