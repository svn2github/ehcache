/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

public class OtherClassLoaderEventTest extends AbstractStandaloneCacheTest {

  public OtherClassLoaderEventTest() {
    super("other-class-loader-event-test.xml", OtherClassLoaderEventClient1.class, OtherClassLoaderEventClient2.class);
    setParallelClients(true);
  }

}
