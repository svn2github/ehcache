package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class Client2 extends ClientBase {

  public Client2(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new Client2(args).run();
  }

  @Override
  protected void test(Cache cache) throws Throwable {
    Element element = cache.get("key");

    if (element == null) { throw new AssertionError(); }

    Object value = element.getObjectValue();
    if (!"value".equals(value)) { throw new AssertionError("unexpected value: " + value); }
  }
}