package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class Client1 extends ClientBase {

  public Client1(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new Client1(args).run();
  }

  @Override
  protected void test(Cache cache) throws Throwable {
    cache.put(new Element("key", "value"));
  }
}