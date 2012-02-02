/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.terracotta.InternalEhcache;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerMapLocalStoreTest {
  private ServerMapLocalStore serverMapLocalStore;

  private final String        unclusteredCacheName = "test-unclustered-cache";
  private final int           keyCount             = 1000;
  private final int           threadCount          = 5;
  private final CacheManager  cm                   = CacheManager.getInstance();
  private final Set<Object>   keySet               = prepareKeySet();

  private Set<Object> prepareKeySet() {
    Set<Object> keys = new HashSet<Object>();
    for (int i = 0; i < keyCount; ++i) {
      keys.add("key-" + i);
    }
    return keys;
  }

  @After
  public void remove() throws Exception {
    cm.removeCache(unclusteredCacheName);
  }

  @Test
  public void testParallelReadWriteWithoutOffHeap() throws Throwable {
    Ehcache localStoreCache = createCache(unclusteredCacheName, false);
    serverMapLocalStore = new EhcacheSMLocalStore((InternalEhcache) localStoreCache);
    parallelReadWrite(serverMapLocalStore);
    System.out.println("testParallelReadWriteWithoutOffHeap is complete");
  }

  private void parallelReadWrite(ServerMapLocalStore smLocalStore) throws InterruptedException, ExecutionException {
    ScheduledExecutorService service = new ScheduledThreadPoolExecutor(threadCount);
    ScheduledFuture<?> readerFuture = service.scheduleWithFixedDelay(new Reader(smLocalStore, false), 0, 500,
                                                                     TimeUnit.MILLISECONDS);
    ScheduledFuture<?> writerFuture = service.schedule(new Writer(smLocalStore, "writer1"), 500, TimeUnit.MILLISECONDS);
    writerFuture.get();

    writerFuture = service.schedule(new Writer(smLocalStore, "writer2"), 500, TimeUnit.MILLISECONDS);
    writerFuture.get();

    ScheduledFuture<?> verifierFuture = service.schedule(new Reader(smLocalStore, true), 500, TimeUnit.MILLISECONDS);
    verifierFuture.get();

    readerFuture.cancel(false);
  }

  private InternalEhcache createCache(String cacheName, boolean overflowToOffHeap) {
    CacheConfiguration cacheConfiguration = new CacheConfiguration(cacheName, 0);
    cacheConfiguration.overflowToOffHeap(overflowToOffHeap);
    cacheConfiguration.setMaxBytesLocalOffHeap(MemoryUnit.MEGABYTES.toBytes(100));
    InternalEhcache cache = new Cache(cacheConfiguration);
    cm.addCache(cache);
    return cache;
  }

  private class Reader extends Thread {
    private final ServerMapLocalStore ehcache;
    private final boolean             doVerify;

    public Reader(ServerMapLocalStore ehcache, boolean doVerify) {
      this.ehcache = ehcache;
      this.doVerify = doVerify;
    }

    @Override
    public void run() {
      try {
        for (Object key : ehcache.getKeys()) {
          Object value = ehcache.get(key);
          if (doVerify) {
            System.err.println("Verifier key " + key + " value " + value);
            Assert.assertEquals("value-writer2", ehcache.get(key));
          } else {
            System.err.println("Reader key " + key + " value " + value);
          }
          Thread.sleep(10);
        }
      } catch (InterruptedException e) {
        System.err.println("Reader/Verifier Interrupted " + e);
      }
    }
  }

  private class Writer extends Thread {
    private final ServerMapLocalStore smLocalStore;
    private final String              valueSuffix;

    public Writer(ServerMapLocalStore serverMapLocalStore, String valueSuffix) {
      this.smLocalStore = serverMapLocalStore;
      this.valueSuffix = valueSuffix;
    }

    @Override
    public void run() {
      try {
        for (Object key : keySet) {
          System.err.println("Writer key " + key + " value " + "value-" + valueSuffix);
          smLocalStore.put(key, "value-" + valueSuffix);
          Thread.sleep(10);
        }
      } catch (InterruptedException e) {
        System.err.println("Writer Interrupted " + e);
      } catch (ServerMapLocalStoreFullException e) {
        System.err.println("Writer filled up local cache " + e);
      }
    }
  }
}
