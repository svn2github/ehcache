package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class UnclusteredClient extends ClientBase {

  public UnclusteredClient(String[] args) {
    super("test-unclustered", args);
  }

  public static void main(String[] args) {
    new UnclusteredClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    Element element = cache.get("key");
    if (element != null) {
      throw new AssertionError();
    }

    cache.put(new Element("key", "value"));

    element = cache.get("key");
    Object value = element.getObjectValue();
    if (!"value".equals(value)) { throw new AssertionError("unexpected value: " + value); }
  }
}