/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.io.IOUtils;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

public class CopyOnReadTest extends AbstractCacheTestBase {

  public CopyOnReadTest(TestConfig testConfig) {
    super("basic-copy-on-read-cache-test.xml", testConfig, App.class);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(IOUtils.class));
    return extraJars;
  }

  public static class App extends ClientBase {
    public App(String[] args) {
      super(args);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      Loader loader1 = new Loader("1");
      Loader loader2 = new Loader("2");

      // // hack-tastic! Since system test framework picks manager based on TCCL we need to register these new loaders
      // ClassProcessorHelper.setContext(loader1, ClassProcessorHelper.getContext(getClass().getClassLoader()));
      // ClassProcessorHelper.setContext(loader2, ClassProcessorHelper.getContext(getClass().getClassLoader()));

      cache.put(new Element("key", loader1.loadClass(ValueHolder.class.getName()).newInstance()));

      Thread.currentThread().setContextClassLoader(loader1);
      Object value = cache.get("key").getObjectValue();
      // read should be done with TCCL
      Assert.assertEquals(loader1, value.getClass().getClassLoader());
      // repeated read should get a different object
      Assert.assertNotSame(value, cache.get("key").getObjectValue());

      Thread.currentThread().setContextClassLoader(loader2);
      // read with TCCL set to loader2 should get value defined in loader2
      value = cache.get("key").getObjectValue();
      Assert.assertEquals(loader2, value.getClass().getClassLoader());
    }

  }

  public static class ValueHolder implements Serializable {
    private volatile String data;

    public ValueHolder() {
      //
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }
  }

  private static class Loader extends ClassLoader {
    private final String id;

    Loader(String id) {
      super(null);
      this.id = id;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (name.equals(ValueHolder.class.getName())) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/').concat(".class"));
        byte[] b;
        try {
          b = IOUtils.toByteArray(in);
        } catch (IOException e) {
          throw new ClassNotFoundException(name, e);
        }
        return defineClass(name, b, 0, b.length);
      }

      return super.loadClass(name);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "(" + id + ")";
    }

  }

}
