/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache;

import org.junit.Test;

/**
 * @author Ludovic Orban
 */
public class ClusteredInstanceFactoryAccessorTest {

  @Test
  public void testAccessorDoesNotThrowException() throws Exception {
    CacheManager cm = new CacheManager();
    try {
      ClusteredInstanceFactoryAccessor.getClusteredInstanceFactory(cm);
    } finally {
      cm.shutdown();
    }
  }

}
