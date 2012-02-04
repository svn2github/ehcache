package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class ClusteredEventsRemoteClient2 extends ClientBase {

  public ClusteredEventsRemoteClient2(String[] args) {
    super("testRemote", args);
  }

  public static void main(String[] args) {
    new ClusteredEventsRemoteClient2(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    getBarrierForAllClients().await();
    cache.put(new Element("key2", "value2"));
    Thread.sleep(5000);
  }
}