/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import junit.framework.Test;

public class QueryCacheInvalidationTest extends NonTransactionalCacheTest {

  public QueryCacheInvalidationTest() {
    //disableAllUntil("2011-09-25");
  }

  public static Test suite() {
    return new NonTransactionalCacheTest.TestSetup(QueryCacheInvalidationTest.class,
                                                   QueryCacheInvalidationServlet.class);
  }
}
