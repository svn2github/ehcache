package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import org.terracotta.ehcache.tests.AbstractWriteBehindClient;
import org.terracotta.ehcache.tests.WriteBehindCacheWriter;
import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.TimeUnit;

public class SerializationWriteBehindClient2 extends AbstractWriteBehindClient {
  public SerializationWriteBehindClient2(String[] args) {
    super(args);
  }

  @Override
  public long getSleepBetweenWrites() {
    return 10L;
  }

  @Override
  public long getSleepBetweenDeletes() {
    return 10L;
  }

  public static void main(String[] args) {
    new SerializationWriteBehindClient2(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    WriteBehindManager wbManager = ((WriteBehindManager) cache.getWriterManager());
    long size = wbManager.getQueueSize();
    while (size > 0) {
      System.out.println("write behind queue size " + size);
      TimeUnit.SECONDS.sleep(1L);
      size = wbManager.getQueueSize();
    }
    cache.dispose();
  }
}