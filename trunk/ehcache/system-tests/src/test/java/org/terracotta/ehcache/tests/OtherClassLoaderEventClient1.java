package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

import com.otherclassloader.Value;

public class OtherClassLoaderEventClient1 extends ClientBase {

  public OtherClassLoaderEventClient1(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new OtherClassLoaderEventClient1(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {

    getBarrierForAllClients().await();

    cache.put(new Element("put", new Value("put")));
  }
}
