package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class Client3 extends ClientBase {

  public Client3(String[] args) {
    super("test", new String[] { "${my.tc.server.topology}" });
  }

  public static void main(String[] args) {
    new Client3(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    cache.put(new Element("key", "value"));
  }
}