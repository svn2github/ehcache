package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class Client1 extends ClientBase {
  public static void main(String[] args) {
    new Client1().run();
  }

  @Override
  protected void test(Cache cache) throws Throwable {
    cache.put(new Element("key", "value"));
  }
}