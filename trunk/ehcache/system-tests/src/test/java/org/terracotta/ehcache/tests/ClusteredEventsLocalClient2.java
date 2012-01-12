package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;

public class ClusteredEventsLocalClient2 extends ClientBase {

  public ClusteredEventsLocalClient2(String[] args) {
    super("testLocal", args);
  }

  public static void main(String[] args) {
    new ClusteredEventsLocalClient2(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    final Barrier barrier = toolkit.getBarrier("ClusteredEventsLocalClientBarrier", 2);
    barrier.await();
    cache.put(new Element("key2", "value2"));
    Thread.sleep(5000);
  }
}