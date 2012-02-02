package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class ClusteredEventsLocalClient1 extends ClientBase {

  public ClusteredEventsLocalClient1(String[] args) {
    super("testLocal", args);
  }

  public static void main(String[] args) {
    new ClusteredEventsLocalClient1(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    getBarrierForAllClients().await();
    cache.put(new Element("key1", "value1"));
    Thread.sleep(5000);
  }
}