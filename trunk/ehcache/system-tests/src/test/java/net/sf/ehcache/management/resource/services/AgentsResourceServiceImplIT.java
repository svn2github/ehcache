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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/ endpoint
 * works fine
 */
public class AgentsResourceServiceImplIT extends ResourceServiceImplITHelper {

  public static final int PORT = 12121;
  public static final String BASEURI = "http://localhost";
  private static final String INFO = "info/";
  private CacheManager manager;
  private static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents/";

  @Before
  public void setUp() throws UnsupportedEncodingException {
    manager = new CacheManager(AgentsResourceServiceImplIT.class.getResource("/management/standalone-ehcache-rest-agent-test.xml"));
    RestAssured.baseURI = BASEURI;
    RestAssured.port = PORT;
  }

  @Test
  /**
   * - GET the list of agents
   * - GET the subresource /info
   *
   * @throws Exception
   */
  public void getAgentsTest__OneCacheManager() throws Exception {


    // [{"version":null,"agentId":"embedded","agencyOf":"Ehcache","rootRepresentables":{"cacheManagerNames":"testCacheManager"}}]
    expect().contentType(ContentType.JSON)
            .rootPath("get(0)")
            .body("agentId", equalTo("embedded"))
            .body("agencyOf", equalTo("Ehcache"))
            .body("rootRepresentables.cacheManagerNames", equalTo("testCacheManager"))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION);

    // /info
    //[{"agentId":"embedded","agencyOf":"Ehcache","available":true,"secured":false,"sslEnabled":false,"needClientAuth":false,"licensed":false,"sampleHistorySize":30,"sampleIntervalSeconds":1,"enabled":true,"restAPIVersion":null}]
    expect().contentType(ContentType.JSON)
            .rootPath("get(0)")
            .body("agentId", equalTo("embedded"))
            .body("agencyOf", equalTo("Ehcache"))
            .body("available", equalTo(true))
            .body("secured", equalTo(false))
            .body("sslEnabled", equalTo(false))
            .body("needClientAuth", equalTo(false))
            .body("licensed", equalTo(false))
            .body("sampleHistorySize", equalTo(30))
            .body("sampleIntervalSeconds", equalTo(1))
            .body("enabled", equalTo(true))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION + INFO);
  }

  @Test
  /**
   * - GET the list of agents
   *
   * @throws Exception
   */
  public void getAgentsTest__TwoCacheManagers() throws Exception {
    // we configure the second cache manager programmatically
    CacheManager mgr = getCacheManagerProgramatically();

    // let's check the agent was edited correctly server side
    expect().contentType(ContentType.JSON)
            .rootPath("get(0)")
            .body("agentId", equalTo("embedded"))
            .body("agencyOf", equalTo("Ehcache"))
            .body("rootRepresentables.cacheManagerNames", equalTo("testCacheManagerProgrammatic,testCacheManager"))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION);
    mgr.clearAll();
    mgr.shutdown();
  }

  @After
  public void tearDown() {
    if (manager != null) {
      manager.shutdown();
    }
  }

}
