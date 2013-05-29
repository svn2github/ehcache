/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import junit.framework.Test;

public class KeyDeserializationSecondLevelCacheEntryTest extends NonTransactionalCacheTest {

  public static Test suite() {
    return new NonTransactionalCacheTest.TestSetup(KeyDeserializationSecondLevelCacheEntryTest.class,
                                                   KeyDeserializationSecondLevelCacheEntryTestServlet.class);
  }
}
