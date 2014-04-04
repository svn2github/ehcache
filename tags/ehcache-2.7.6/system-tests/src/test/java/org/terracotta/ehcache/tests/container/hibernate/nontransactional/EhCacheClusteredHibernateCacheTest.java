/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import junit.framework.Test;

public class EhCacheClusteredHibernateCacheTest extends NonTransactionalCacheTest {

  public static Test suite() {
    return new NonTransactionalCacheTest.TestSetup(EhCacheClusteredHibernateCacheTest.class,
                                                   EhCacheClusteredHibernateCacheServlet.class);
  }

}
