/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.ehcache.hibernate.nontransactional;

import org.terracotta.modules.ehcache.hibernate.BaseClusteredRegionFactoryTest;

import com.tc.test.server.appserver.deployment.DeploymentBuilder;

import javax.servlet.http.HttpServlet;

public abstract class NonTransactionalCacheTest extends BaseClusteredRegionFactoryTest {
  
  private static final String CONFIG_FILE_FOR_TEST = "/hibernate-config/tc-config.xml";

  static class TestSetup extends BaseClusteredRegionFactoryTest.BaseClusteredCacheProviderTestSetup {

    private final Class<? extends HttpServlet> servletClass;
    
    TestSetup(Class testClass, Class<? extends HttpServlet> servletClass) {
      super(testClass, CONFIG_FILE_FOR_TEST);
      this.servletClass = servletClass;
    }
    
    @Override
    protected void customizeWar(DeploymentBuilder builder) {
      builder.addResource("/hibernate-config/nontransactional/", "hibernate.cfg.xml", "WEB-INF/classes/hibernate-config");
      builder.addResource("/hibernate-config/nontransactional/", "hibernate-identity.cfg.xml", "WEB-INF/classes/hibernate-config");
      builder.addResource("/hibernate-config/nontransactional/", "hibernate-non-eternal.cfg.xml", "WEB-INF/classes/hibernate-config");
      builder.addResource("/hibernate-config/nontransactional/domain", "Item.hbm.xml", "WEB-INF/classes/hibernate-config/domain");
      builder.addResource("/hibernate-config/nontransactional/domain", "Event.hbm.xml", "WEB-INF/classes/hibernate-config/domain");
      builder.addResource("/hibernate-config/nontransactional/domain", "HolidayCalendar.hbm.xml", "WEB-INF/classes/hibernate-config/domain");
      builder.addResource("/hibernate-config/nontransactional/domain", "Person.hbm.xml", "WEB-INF/classes/hibernate-config/domain");
      builder.addResource("/hibernate-config/nontransactional/domain", "PhoneNumber.hbm.xml", "WEB-INF/classes/hibernate-config/domain");
      builder.addResource("/hibernate-config/nontransactional/domain", "Account.hbm.xml", "WEB-INF/classes/hibernate-config/domain");
    }

    @Override
    protected Class getServletClass() {
      return servletClass;
    }
  }
  
}
