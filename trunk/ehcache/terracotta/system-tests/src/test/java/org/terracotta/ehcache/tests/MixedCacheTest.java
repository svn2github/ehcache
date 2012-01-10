/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;


public class MixedCacheTest extends AbstractStandaloneCacheTest {

  public MixedCacheTest() {
    super("mixed-cache-test.xml", Client1.class, Client2.class, UnclusteredClient.class, UnclusteredClient.class);
  }
  

}
