package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.terracotta.toolkit.Toolkit;

import junit.framework.Assert;


public class ExpiryListenerClient2 extends ClientBase {

  public ExpiryListenerClient2(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ExpiryListenerClient2(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    Assert.assertEquals(0, cache.getSize());
  }
}