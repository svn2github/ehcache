/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

import java.util.Date;

import junit.framework.Assert;

public class ServerMapElementTTLExpressTestClient extends ServerMapClientBase {

  public ServerMapElementTTLExpressTestClient(String[] args) {
    super("testWithElementEvictionTTL", args);
  }

  public static void main(String[] args) {
    new ServerMapElementTTLExpressTestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    int size = cache.getSize();
    assertEquals(0, size);
    System.out.println("Client populating cache.");

    for (int i = 0; i < 1500; i++) {
      cache.put(new Element("key-" + i, "value-" + i));
    }

    for (int i = 1500; i < 2000; i++) {
      Element element = new Element("key-" + i, "value-" + i);
      element.setTimeToLive(60 * 5);
      cache.put(element);
    }

    System.out.println("Cache populate. Size: " + cache.getSize());

    System.out.println("Sleeping for 2 mins (now=" + new Date() + ") ... ");
    // Sleep for TTL to kick in:
    Thread.sleep(2 * 60 * 1000);

    System.out.println("After sleeping 2 mins. Size: " + cache.getSize());

    for (int i = 0; i < 1500; i++) {
      Element element = cache.get("key-" + i);
      Assert.assertNull("Element should be null of key-" + i, element);
    }

    for (int i = 1500; i < 2000; i++) {
      Element element = cache.get("key-" + i);
      Assert.assertNotNull("element shouldn't be null for key-" + i, element);
      Assert.assertTrue(element.getValue().equals("value-" + i));
    }

    for (int i = 2000; i < 2500; i++) {
      Element element = new Element("key-" + i, "value-" + i);
      element.setTimeToLive(15 * 60);
      cache.put(element);
    }

    System.out.println("Sleeping for 5 mins (now=" + new Date() + ") ... ");
    // Sleep for TTL to kick in:
    Thread.sleep(5 * 60 * 1000);

    System.out.println("After sleeping 5 mins. Size: " + cache.getSize());

    for (int i = 1500; i < 2000; i++) {
      Element element = cache.get("key-" + i);
      Assert.assertNull("Element should be null of key-" + i, element);
    }

    for (int i = 2000; i < 2500; i++) {
      Element element = cache.get("key-" + i);
      Assert.assertNotNull("element shouldn't be null for key-" + i, element);
      Assert.assertTrue(element.getValue().equals("value-" + i));
    }
  }

}
