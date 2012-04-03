package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

public class ClusteredEventsRemoteClient1 extends ClientBase {

  public ClusteredEventsRemoteClient1(String[] args) {
    super("testRemote", args);
  }

  public static void main(String[] args) {
    new ClusteredEventsRemoteClient1(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    getBarrierForAllClients().await();
    cache.put(new Element("key1", "value1"));
    Thread.sleep(5000);
  }
}