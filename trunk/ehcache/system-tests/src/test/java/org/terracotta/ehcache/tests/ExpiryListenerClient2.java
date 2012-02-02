package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;

import junit.framework.Assert;


public class ExpiryListenerClient2 extends ClientBase {

  public ExpiryListenerClient2(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ExpiryListenerClient2(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    Assert.assertEquals(0, cache.getSize());
  }
}