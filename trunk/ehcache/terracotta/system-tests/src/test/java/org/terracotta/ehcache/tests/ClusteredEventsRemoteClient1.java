package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;

public class ClusteredEventsRemoteClient1 extends ClientBase {

  public ClusteredEventsRemoteClient1(String[] args) {
    super("testRemote", args);
  }

  public static void main(String[] args) {
    new ClusteredEventsRemoteClient1(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    final Barrier barrier = toolkit.getBarrier("ClusteredEventsRemoteClientBarrier", 2);
    barrier.await();
    cache.put(new Element("key1", "value1"));
    Thread.sleep(5000);
  }
}