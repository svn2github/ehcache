/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class HttpUrlConfigCacheTest extends AbstractCacheTestBase {

  public HttpUrlConfigCacheTest(TestConfig testConfig) {
    super("http-url-config-cache-test.xml", testConfig, Client.class);
  }

  public static class Client extends ClientBase {

    public Client(String[] args) {
      super("test", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
      cache.put(new Element("key", "value"));
      Assert.assertEquals("value", cache.get("key").getObjectValue());
    }

  }

}
