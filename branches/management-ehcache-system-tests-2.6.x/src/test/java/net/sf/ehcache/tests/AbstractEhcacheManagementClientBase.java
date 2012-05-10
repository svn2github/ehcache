package net.sf.ehcache.tests;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.junit.Test;
import org.terracotta.tests.base.AbstractClientBase;

import com.jayway.restassured.RestAssured;
import com.tc.test.jmx.TestHandlerMBean;

import java.lang.reflect.Method;
import java.net.URL;

/**
 * @author Ludovic Orban
 */
public abstract class AbstractEhcacheManagementClientBase extends AbstractClientBase {
  public AbstractEhcacheManagementClientBase(String[] args) {
    super(args);
  }

  @Override
  public final void doTest() throws Exception {
    TestHandlerMBean controlMbean = getTestControlMbean();
    URL url = getClass().getResource("/ehcache-rest.xml");
    Configuration configuration = ConfigurationFactory.parseConfiguration(url);
    configuration.getTerracottaConfiguration().url(controlMbean.getTerracottaUrl());
    RestAssured.baseURI = "http://" + configuration.getManagementRESTService().getBind() + "/tc-management-api/";
    CacheManager cacheManager = new CacheManager(configuration);

    boolean fail = false;

    Method[] methods = getClass().getMethods();
    for (Method method : methods) {
      if (method.getAnnotation(Test.class) != null) {
        try {
          System.out.println("TEST: Executing " + method.getName());
          method.invoke(this);
          System.out.println("TEST: " + method.getName() + " passed");
        } catch (Exception e) {
          e.printStackTrace();
          fail = true;
          System.out.println("TEST: " + method.getName() + " failed");
        }
      }
    }

    cacheManager.shutdown();

    if (fail) {
    	throw new Exception("Test failed.");
    }
  }

}
