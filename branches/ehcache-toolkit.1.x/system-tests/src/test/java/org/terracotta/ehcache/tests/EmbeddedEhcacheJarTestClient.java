/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.junit.Assert;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.collections.ClusteredMap;

public class EmbeddedEhcacheJarTestClient extends ClientBase {

  public EmbeddedEhcacheJarTestClient(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    ClusteredMap<String, String> map = toolkit.getMap("testMap");
    Class ehcacheClass = map.getClass().getClassLoader().loadClass("net.sf.ehcache.Ehcache");
    // Verify that the Ehcache.class loaded from the ClusteredStateLoader is the same as that loaded from the app. Since
    // Ehcache will be on the classpath for this test, we want to verify that the app class loader version is used
    // rather than the embedded one from inside the toolkit runtime jar.
    System.out.println("Ehcache.class class loader: " + ehcacheClass.getClassLoader());
    Assert.assertTrue("Ehcache.class was not loaded from the app classloader.",
                      ehcacheClass.getClassLoader() == EmbeddedEhcacheJarTestClient.class.getClassLoader());
  }

  public static void main(String[] args) {
    new EmbeddedEhcacheJarTestClient(args).run();
  }
}
