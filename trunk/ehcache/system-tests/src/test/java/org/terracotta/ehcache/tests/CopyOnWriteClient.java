package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

import java.io.Serializable;

public class CopyOnWriteClient extends ClientBase {

  public CopyOnWriteClient(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    final Foo foo = new Foo();
    Element e = new Element("foo", foo);
    cache.put(e);
    Object o = cache.get("foo").getObjectValue();

    if (o == foo) { throw new AssertionError(); }
  }

  private static class Foo implements Serializable {
    //
  }

}
