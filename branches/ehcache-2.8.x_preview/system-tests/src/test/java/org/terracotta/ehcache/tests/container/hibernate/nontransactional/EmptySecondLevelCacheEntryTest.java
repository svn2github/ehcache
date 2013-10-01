/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import junit.framework.Test;

public class EmptySecondLevelCacheEntryTest extends NonTransactionalCacheTest {

  public static Test suite() {
    return new NonTransactionalCacheTest.TestSetup(EmptySecondLevelCacheEntryTest.class,
                                                   EmptySecondLevelCacheEntryServlet.class);
  }

}
