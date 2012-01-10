/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.hibernate.nontransactional;

import junit.framework.Test;

public class KeyDeserializationSecondLevelCacheEntryTest extends NonTransactionalCacheTest {

  public static Test suite() {
    return new TestSetup(KeyDeserializationSecondLevelCacheEntryTest.class,
                         KeyDeserializationSecondLevelCacheEntryTestServlet.class);
  }
}
