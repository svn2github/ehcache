package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

public class Client4 extends ClientBase {

  public Client4(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    Element element = cache.get("key");

    if (element == null) { throw new AssertionError(); }

    Object value = element.getObjectValue();
    if (!"value".equals(value)) { throw new AssertionError("unexpected value: " + value); }
  }
}