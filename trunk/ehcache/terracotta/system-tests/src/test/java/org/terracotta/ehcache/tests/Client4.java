package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class Client4 extends ClientBase {

  public Client4(String[] args) {
    super("test", new String[] { "${my.tc.server.topology}" });
  }

  public static void main(String[] args) {
    new Client4(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    Element element = cache.get("key");

    if (element == null) { throw new AssertionError(); }

    Object value = element.getObjectValue();
    if (!"value".equals(value)) { throw new AssertionError("unexpected value: " + value); }
  }
}