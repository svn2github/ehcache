package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.containsString;

/**
 * @author: Anthony Dahanne
 */
public class AgentsResourceServiceImplIT {

  public static final int PORT = 12121;
  public static final String BASEURI = "http://localhost";
  private CacheManager manager;
  private static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents/";

  @Before
  public void setUp() throws UnsupportedEncodingException {
    manager = new CacheManager(AgentsResourceServiceImplIT.class.getResource("/management/standalone-ehcache-rest-agent-test.xml"));
    RestAssured.baseURI = BASEURI;
    RestAssured.port =  PORT;
  }

  @Test
  /**
   * The aim of this test, is to simulate the user interface, via REST calls to the ehcache standalone agent:
   * - GET the list of agents
   *
   * @throws Exception
   */
  public void getAgentsTest__OneCacheManager() throws Exception {


    // let's check the agent was edited correctly server side
    expect().contentType(ContentType.JSON).body(containsString("\"agentId\":\"embedded\""),
                                                containsString("\"agencyOf\":\"Ehcache\""),
                                                containsString("\"rootRepresentables\":{\"cacheManagerNames\":\"testCacheManager\"}"))
                                          .statusCode(200)
                                          .when().get(EXPECTED_RESOURCE_LOCATION);
  }
  @Test
  /**
   * The aim of this test, is to simulate the user interface, via REST calls to the ehcache standalone agent:
   * - GET the list of agents
   *
   * @throws Exception
   */
  public void getAgentsTest__TwoCacheManagers() throws Exception {
    // we configure the second cache manager programmatically
    Configuration configuration = new Configuration();
    configuration.setName("testCacheManagerProgrammatic");

    CacheConfiguration myCache = new CacheConfiguration("testCache2", 10000)
            .eternal(false);
    configuration.addCache(myCache);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:12121");
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);

    CacheManager mgr = new CacheManager(configuration);
    Cache exampleCache = mgr.getCache("testCache2");
    assert (exampleCache != null);

    // let's check the agent was edited correctly server side
    expect().contentType(ContentType.JSON).body(containsString("\"agentId\":\"embedded\""),
            containsString("\"agencyOf\":\"Ehcache\""),
            containsString("\"rootRepresentables\":{\"cacheManagerNames\":\"testCacheManagerProgrammatic,testCacheManager\"}"))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION);
  }

  @After
  public void tearDown() {
    if (manager != null) {
      manager.shutdown();
    }
  }

}
