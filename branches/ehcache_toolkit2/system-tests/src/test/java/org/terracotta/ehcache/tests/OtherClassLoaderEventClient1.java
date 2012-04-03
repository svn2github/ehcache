package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import com.otherclassloader.Value;

public class OtherClassLoaderEventClient1 extends ClientBase {

  public OtherClassLoaderEventClient1(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new OtherClassLoaderEventClient1(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {

    getBarrierForAllClients().await();

    cache.put(new Element("put", new Value("put")));
  }
}
