package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.*;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches endpoint
 * works fine
 */
public class CacheStatisticSamplesResourceServiceImplIT extends ResourceServiceImplITHelper {

  public static final int PORT = 12121;
  public static final String BASEURI = "http://localhost";
  private CacheManager manager;
  private static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents/cacheManagers/caches/statistics/samples";
  private CacheManager managerProgrammatic;

  @Before
  public void setUp() throws UnsupportedEncodingException {
    RestAssured.baseURI = BASEURI;
    RestAssured.port = PORT;
    manager = new CacheManager(CacheStatisticSamplesResourceServiceImplIT.class.getResource("/management/standalone-ehcache-rest-agent-test.xml"));
    // we configure the second cache manager programmatically
    managerProgrammatic = getCacheManagerProgramatically();
  }

  @Test
  /**
   * - GET the list of cache statistics
   *
   * @throws Exception
   */
  public void getCacheStatisticSamples() throws Exception {
    /*
[
  {
    "version": null,
    "agentId": "embedded",
    "name": "testCache2",
    "cacheManagerName": "testCacheManagerProgrammatic",
    "statName": "LocalHeapSize",
    "statValueByTimeMillis": {
      "1371850582455": 1000,
      "1371850583455": 1000,
      "1371850584455": 1000,
      "1371850585455": 1000
    }
  }
]
     */


    Cache exampleCache = manager.getCache("testCache");
    Cache exampleCache2 = managerProgrammatic.getCache("testCache2");

    for (int i = 0; i < 1000; i++) {
      exampleCache2.put(new Element("key" + i, "value" + i));
      exampleCache.put(new Element("key" + i, "value" + i));
      expect().statusCode(200).when().get(EXPECTED_RESOURCE_LOCATION);
    }

    // we precise the cacheManager, cache and 2 stats we want to retrieve
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).statName", equalTo("LocalHeapSizeInBytes"))
            // we got samples
            .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
            // LocalHeapSize > 0
            .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(1).agentId", equalTo("embedded"))
            .body("get(1).name", equalTo("testCache2"))
            .body("get(1).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(1).statName", equalTo("LocalHeapSize"))
            // we got samples
            .body("get(1).statValueByTimeMillis.size()", greaterThan(0))
            // LocalHeapSize > 0
            .body("get(1).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("size()", is(2))
            .statusCode(200)
            .when().get("/tc-management-api/agents/cacheManagers;names=testCacheManagerProgrammatic/caches;names=testCache2/statistics/samples;names=LocalHeapSize,LocalHeapSizeInBytes");


    // we precise the cache and 2 stats we want to retrieve
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).statName", equalTo("LocalHeapSizeInBytes"))
                    // we got samples
            .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
                    // LocalHeapSize > 0
            .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(1).agentId", equalTo("embedded"))
            .body("get(1).name", equalTo("testCache2"))
            .body("get(1).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(1).statName", equalTo("LocalHeapSize"))
                    // we got samples
            .body("get(1).statValueByTimeMillis.size()", greaterThan(0))
                    // LocalHeapSize > 0
            .body("get(1).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("size()", is(2))
            .statusCode(200)
            .when().get("/tc-management-api/agents/cacheManagers/caches;names=testCache2/statistics/samples;names=LocalHeapSize,LocalHeapSizeInBytes");


    // we precise 2 stats we want to retrieve
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).statName", equalTo("LocalHeapSizeInBytes"))
            .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(1).agentId", equalTo("embedded"))
            .body("get(1).name", equalTo("testCache2"))
            .body("get(1).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(1).statName", equalTo("LocalHeapSize"))
            .body("get(1).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(1).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(2).agentId", equalTo("embedded"))
            .body("get(2).name", equalTo("testCache"))
            .body("get(2).cacheManagerName", equalTo("testCacheManager"))
            .body("get(2).statName", equalTo("LocalHeapSizeInBytes"))
            .body("get(2).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(2).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(3).agentId", equalTo("embedded"))
            .body("get(3).name", equalTo("testCache"))
            .body("get(3).cacheManagerName", equalTo("testCacheManager"))
            .body("get(3).statName", equalTo("LocalHeapSize"))
            .body("get(3).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(3).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("size()", is(4))
            .statusCode(200)
            .when().get("/tc-management-api/agents/cacheManagers/caches/statistics/samples;names=LocalHeapSize,LocalHeapSizeInBytes");

    // we precise nothing : we get it all !
    expect().contentType(ContentType.JSON)
            .body("size()", greaterThan(40))
            .statusCode(200)
            .when().get("/tc-management-api/agents/cacheManagers/caches/statistics/samples");


  }


  @After
  public void tearDown() {
    if (manager != null) {
      manager.shutdown();
    }
    if (managerProgrammatic != null) {
      managerProgrammatic.shutdown();
    }
  }

}
