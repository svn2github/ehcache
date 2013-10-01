package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/ endpoint
 * works fine
 */
public class AgentsResourceServiceImplIT extends ResourceServiceImplITHelper {

  protected static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(AgentsResourceServiceImplIT.class);
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
            .when().get(STANDALONE_BASE_URL + EXPECTED_RESOURCE_LOCATION);


    // [{"version":null,"agentId":"embedded","agencyOf":"Ehcache","rootRepresentables":{"cacheManagerNames":"testCacheManager"}}]
    expect().contentType(ContentType.JSON)
            .rootPath("get(0)")
            .body("agentId", equalTo("embedded"))
            .body("agencyOf", equalTo("Ehcache"))
            .body("rootRepresentables.cacheManagerNames", equalTo("testCacheManager"))
            .statusCode(200)
            .when().get(STANDALONE_BASE_URL + EXPECTED_RESOURCE_LOCATION +";ids=embedded");


    // [{"version":null,"agentId":"embedded","agencyOf":"Ehcache","rootRepresentables":{"cacheManagerNames":"testCacheManager"}}]
    expect().contentType(ContentType.JSON)
            .statusCode(400)
            .when().get(STANDALONE_BASE_URL + EXPECTED_RESOURCE_LOCATION +";ids=w00t");

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
            .when().get(STANDALONE_BASE_URL + EXPECTED_RESOURCE_LOCATION + INFO);

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
            .when().get(STANDALONE_BASE_URL + EXPECTED_RESOURCE_LOCATION  +";ids=embedded"+ INFO);
  }

  @Test
  /**
   * - GET the list of agents
   *
   * @throws Exception
   */
  public void getAgentsTest__TwoCacheManagers() throws Exception {
    // we configure the second cache manager programmatically
    cacheManagerProgrammatic = getCacheManagerProgrammatic();
    // let's check the agent was edited correctly server side
    expect().contentType(ContentType.JSON)
            .rootPath("get(0)")
            .body("agentId", equalTo("embedded"))
            .body("agencyOf", equalTo("Ehcache"))
            .body("rootRepresentables.cacheManagerNames", equalTo("testCacheManagerProgrammatic,testCacheManager"))
            .statusCode(200)
            .when().get(STANDALONE_BASE_URL + EXPECTED_RESOURCE_LOCATION);
    cacheManagerProgrammatic.clearAll();
    cacheManagerProgrammatic.shutdown();
  }


  @Test
  public void getAgentsTest__clustered() throws Exception {

    // [{"version":null,"agentId":"embedded","agencyOf":"Ehcache","rootRepresentables":{"cacheManagerNames":"testCacheManager"}}]
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", Matchers.equalTo("embedded"))
            .body("get(0).agencyOf", Matchers.equalTo("TSA"))
            .body("get(0).rootRepresentables.urls", Matchers.equalTo("http://localhost:"+TSA_GROUP_PORT))
            .body("get(1).agentId", Matchers.containsString("localhost_"))
            .body("get(1).agencyOf", Matchers.equalTo("Ehcache"))
            .body("get(1).rootRepresentables.isEmpty()", Matchers.is(Boolean.TRUE))
            .statusCode(200)
            .when().get(CLUSTERED_BASE_URL + EXPECTED_RESOURCE_LOCATION);


  }

}
