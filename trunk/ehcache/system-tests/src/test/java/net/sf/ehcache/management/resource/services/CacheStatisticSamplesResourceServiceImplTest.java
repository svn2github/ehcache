package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.http.ContentType;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.*;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches endpoint
 * works fine
 */
public class CacheStatisticSamplesResourceServiceImplTest extends ResourceServiceImplITHelper {


  protected static final String EXPECTED_RESOURCE_LOCATION = "{baseUrl}/tc-management-api/agents{agentIds}/cacheManagers{cmIds}/caches{cacheIds}/statistics/samples{sampleIds}";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(CacheStatisticSamplesResourceServiceImplTest.class);
    cacheManagerMaxBytes = getCacheManagerMaxbytes();
  }

  @Before
  public void setUp() throws UnsupportedEncodingException {
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

    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";
    String samplesFilter = "";

    Cache exampleCache = cacheManagerMaxElements.getCache("testCache");
    Cache exampleCache2 = cacheManagerMaxBytes.getCache("testCache2");

    for (int i = 0; i < 1000; i++) {
      exampleCache2.put(new Element("key" + i, "value" + i));
      exampleCache.put(new Element("key" + i, "value" + i));
      expect().statusCode(200).when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter, cmsFilter, cachesFilter, samplesFilter);
    }


    cmsFilter= ";names=testCacheManagerProgrammatic";
    cachesFilter = ";names=testCache2";
    samplesFilter = ";names=LocalHeapSize,LocalHeapSizeInBytes";
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
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter,cachesFilter,samplesFilter);

    cmsFilter= "";
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
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter,cachesFilter,samplesFilter);


    cachesFilter = "";
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
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL,agentsFilter,cmsFilter,cachesFilter,samplesFilter);

    samplesFilter = "";
    // we precise nothing : we get it all !
    expect().contentType(ContentType.JSON)
            .body("size()", greaterThan(40))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL,agentsFilter,cmsFilter,cachesFilter,samplesFilter);


  }


  @Test
  /**
   * - GET the list of cache statistics
   *
   * @throws Exception
   */
  public void getCacheStatisticSamples__clustered() throws Exception {

    String agentId = getEhCacheAgentId();
    final String agentsFilter = ";ids=" + agentId;
    String cmsFilter = "";
    String cachesFilter = "";
    String samplesFilter = "";

    cmsFilter= ";names=testCacheManagerProgrammatic";
    cachesFilter = ";names=testCache2";
    samplesFilter = ";names=LocalHeapSize,LocalHeapSizeInBytes";
    // we precise the cacheManager, cache and 2 stats we want to retrieve
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo(agentId))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).statName", equalTo("LocalHeapSizeInBytes"))
                    // we got samples
            .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
                    // LocalHeapSize > 0
            .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(1).agentId", equalTo(agentId))
            .body("get(1).name", equalTo("testCache2"))
            .body("get(1).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(1).statName", equalTo("LocalHeapSize"))
                    // we got samples
            .body("get(1).statValueByTimeMillis.size()", greaterThan(0))
                    // LocalHeapSize > 0
            .body("get(1).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("size()", is(2))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter,cmsFilter,cachesFilter,samplesFilter);

    cmsFilter= "";
    // we precise the cache and 2 stats we want to retrieve
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo(agentId))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).statName", equalTo("LocalHeapSizeInBytes"))
                    // we got samples
            .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
                    // LocalHeapSize > 0
            .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(1).agentId", equalTo(agentId))
            .body("get(1).name", equalTo("testCache2"))
            .body("get(1).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(1).statName", equalTo("LocalHeapSize"))
                    // we got samples
            .body("get(1).statValueByTimeMillis.size()", greaterThan(0))
                    // LocalHeapSize > 0
            .body("get(1).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("size()", is(2))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter,cmsFilter,cachesFilter,samplesFilter);


    cachesFilter = "";
    // we precise 2 stats we want to retrieve
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo(agentId))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).statName", equalTo("LocalHeapSizeInBytes"))
            .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(1).agentId", equalTo(agentId))
            .body("get(1).name", equalTo("testCache2"))
            .body("get(1).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(1).statName", equalTo("LocalHeapSize"))
            .body("get(1).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(1).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(2).agentId", equalTo(agentId))
            .body("get(2).name", equalTo("testCache"))
            .body("get(2).cacheManagerName", equalTo("testCacheManager"))
            .body("get(2).statName", equalTo("LocalHeapSizeInBytes"))
            .body("get(2).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(2).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("get(3).agentId", equalTo(agentId))
            .body("get(3).name", equalTo("testCache"))
            .body("get(3).cacheManagerName", equalTo("testCacheManager"))
            .body("get(3).statName", equalTo("LocalHeapSize"))
            .body("get(3).statValueByTimeMillis.size()", greaterThan(0))
            .body("get(3).statValueByTimeMillis.values()[0]", greaterThan(0))
            .body("size()", is(4))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL,agentsFilter,cmsFilter,cachesFilter,samplesFilter);

    samplesFilter = "";
    // we precise nothing : we get it all !
    expect().contentType(ContentType.JSON)
            .body("size()", greaterThan(40))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL,agentsFilter,cmsFilter,cachesFilter,samplesFilter);


  }


  @AfterClass
  public static void tearDown() {
    if (cacheManagerMaxBytes != null) {
      cacheManagerMaxBytes.shutdown();
    }
  }

}
