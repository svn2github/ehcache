/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;


/**
 *
 * @author cdennis
 */
public class ExplicitlyUnclusteredStandaloneCacheTest extends AbstractStandaloneCacheTest {

  public ExplicitlyUnclusteredStandaloneCacheTest() {
    super("explicitly-unclustered-cache-test.xml", UnclusteredClient.class, UnclusteredClient.class);
  }

}
