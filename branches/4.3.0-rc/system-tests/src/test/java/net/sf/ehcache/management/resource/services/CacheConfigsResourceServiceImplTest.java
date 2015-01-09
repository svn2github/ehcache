package net.sf.ehcache.management.resource.services;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.path.xml.NodeImpl;
import com.jayway.restassured.path.xml.XmlPath;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.expect;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches/config endpoint
 * works fine
 * @author Anthony Dahanne
 */
public class CacheConfigsResourceServiceImplTest extends ResourceServiceImplITHelper {

  protected static final String EXPECTED_RESOURCE_LOCATION = "{baseUrl}/tc-management-api/agents{agentIds}/cacheManagers{cmIds}/caches{cacheIds}/configs";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(CacheConfigsResourceServiceImplTest.class);
  }

  @Before
  public void setUp() throws UnsupportedEncodingException {
    cacheManagerMaxBytes = getCacheManagerMaxbytes();
  }

  @Test
  /**
   * - GET the list of caches configs
   *
   * @throws Exception
   */
  public void getCacheConfigsTest() throws Exception {
    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";

    String xml = expect()
        .contentType(ContentType.JSON)
        .body("[0].agentId", equalTo("embedded"))
        .body("[0].cacheManagerName", equalTo("testCacheManagerProgrammatic"))
        .body("[0].cacheName", equalTo("testCache2"))
        .body("[1].agentId", equalTo("embedded"))
        .body("[1].cacheManagerName", equalTo("testCacheManager"))
        .body("[1].cacheName", equalTo("testCache"))
        .statusCode(200)
      .when()
        .get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter, cmsFilter, cachesFilter)
        .jsonPath().get("[1].xml").toString();

    XmlPath xmlPath = new XmlPath(xml);
    NodeImpl cache = xmlPath.get("cache");
    assertEquals("testCache", cache.attributes().get("name"));

    //same thing but we specify only a given cacheManager
    agentsFilter = "";
    cmsFilter = ";names=testCacheManagerProgrammatic";
    cachesFilter = ";names=testCache2";

    String filteredXml = expect()
        .contentType(ContentType.JSON)
        .body("[0].agentId", equalTo("embedded"))
        .body("[0].cacheManagerName", equalTo("testCacheManagerProgrammatic"))
        .body("[0].cacheName", equalTo("testCache2"))
        .statusCode(200)
      .when()
        .get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter, cmsFilter, cachesFilter)
        .jsonPath().get("[0].xml").toString();

    xmlPath = new XmlPath(filteredXml);
    cache = xmlPath.get("cache");
    assertEquals("testCache2", cache.attributes().get("name"));
  }

  @Test
  public void getCacheConfigsTest__clustered() throws Exception {
    String cmsFilter = "";
    String cachesFilter = "";
    String agentId = getEhCacheAgentId();
    String agentsFilter = ";ids=" + agentId;

    String xml = expect()
        .contentType(ContentType.JSON)
        .body("[0].agentId", equalTo(agentId))
        .body("[0].cacheManagerName", equalTo("testCacheManagerProgrammatic"))
        .body("[0].cacheName", equalTo("testCache2"))
        .body("[1].agentId", equalTo(agentId))
        .body("[1].cacheManagerName", equalTo("testCacheManager"))
        .body("[1].cacheName", equalTo("testCache"))
        .statusCode(200)
      .when()
        .get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter, cmsFilter, cachesFilter)
        .jsonPath().get("[1].xml").toString();

    XmlPath xmlPath = new XmlPath(xml);
    NodeImpl cache = xmlPath.get("cache");
    assertEquals("testCache", cache.attributes().get("name"));


    //same thing but we specify only a given cacheManager
    cmsFilter = ";names=testCacheManagerProgrammatic";
    cachesFilter = ";names=testCache2";

    String filteredXml = expect()
        .contentType(ContentType.JSON)
        .body("[0].agentId", equalTo(agentId))
        .body("[0].cacheManagerName", equalTo("testCacheManagerProgrammatic"))
        .body("[0].cacheName", equalTo("testCache2"))
        .statusCode(200)
      .when()
        .get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter, cmsFilter, cachesFilter)
        .jsonPath().get("[0].xml").toString();

    xmlPath = new XmlPath(filteredXml);
    cache = xmlPath.get("cache");
    assertEquals("testCache2", cache.attributes().get("name"));
  }

  @After
  public void tearDown() {
    if (cacheManagerMaxBytes != null) {
      cacheManagerMaxBytes.shutdown();
    }
  }

}
