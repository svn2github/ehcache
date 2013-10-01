/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import junit.framework.Test;

public class NonEternalSecondLevelCacheTest extends NonTransactionalCacheTest {

  private static final String CONFIG_FILE_FOR_TEST = "hibernate-config/ehcache-non-eternal.xml";

  public static Test suite() {
    return new NonTransactionalCacheTest.TestSetup(NonEternalSecondLevelCacheTest.class,
                                                   NonEternalSecondLevelCacheTestServlet.class, CONFIG_FILE_FOR_TEST);
  }

}
