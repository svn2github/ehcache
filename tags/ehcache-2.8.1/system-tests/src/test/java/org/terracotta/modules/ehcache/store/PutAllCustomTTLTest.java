/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.Assert;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.HashSet;

public class PutAllCustomTTLTest extends AbstractCacheTestBase {
  public PutAllCustomTTLTest(TestConfig testConfig) {
    super(testConfig, App.class);
  }

  public static class App extends ClientBase {
    public App(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      Element element1 = new Element("key1", "value1");
      Collection<Element> elements = new HashSet<Element>();
      Element element2 = new Element("key2", "value2", 5, 5);
      elements.add(element1);
      elements.add(element2);
      cache.putAll(elements);
      Assert.assertEquals(element1, cache.get("key1"));
      Assert.assertEquals(element2, cache.get("key2"));
      ThreadUtil.reallySleep(6000);
      Assert.assertEquals(element1, cache.get("key1"));
      Assert.assertNull(cache.get("key2"));
    }

  }

}
