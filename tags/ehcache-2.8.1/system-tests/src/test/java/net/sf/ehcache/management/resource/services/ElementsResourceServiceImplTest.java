package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.http.ContentType;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches/elements endpoint
 * works fine
 */
public class ElementsResourceServiceImplTest extends ResourceServiceImplITHelper {
  protected static final String EXPECTED_RESOURCE_LOCATION = "{baseUrl}/tc-management-api/agents{agentIds}/cacheManagers{cmIds}/caches{cacheIds}/elements";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(ElementsResourceServiceImplTest.class);
  }

  @Before
  public void setUp() throws UnsupportedEncodingException {
    cacheManagerMaxBytes = getCacheManagerMaxbytes();
  }

  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest__notSpecifyingCacheOrCacheManager() throws Exception {
    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";
    expect().statusCode(400)
            .body("details", equalTo(""))
            .body("error", equalTo("No cache specified. Unsafe requests must specify a single cache name."))
            .given()
            .contentType(ContentType.JSON)
            .when().delete(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter,cachesFilter);

    cachesFilter = ";names=testCache2";
    expect().statusCode(400)
            .body("details", equalTo(""))
            .body("error", equalTo("No cache manager specified. Unsafe requests must specify a single cache manager name."))
            .given()
            .contentType(ContentType.JSON)
            .when().delete(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter,cachesFilter);

  }

  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest() throws Exception {
    Cache exampleCache = cacheManagerMaxBytes.getCache("testCache2");
    for (int i=0; i<1000 ; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
    }


    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).attributes.InMemorySize", equalTo(1000))
            .statusCode(200)
            .when().get(STANDALONE_BASE_URL + "/tc-management-api/agents/cacheManagers/caches");

    String agentsFilter = "";
    String cachesFilter = ";names=testCache2";
    String cmsFilter = ";names=testCacheManagerProgrammatic";
    expect().statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .when().delete(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter,cachesFilter);

    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).attributes.InMemorySize", equalTo(0))
            .statusCode(200)
            .when().get(STANDALONE_BASE_URL + "/tc-management-api/agents/cacheManagers/caches");

  }


  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest__clustered() throws Exception {
    Cache exampleCache = cacheManagerMaxBytes.getCache("testCache2");
    for (int i=0; i<1000 ; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
    }
    String agentId = getEhCacheAgentId();
    final String agentsFilter = ";ids=" + agentId;

    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo(agentId))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).attributes.InMemorySize", equalTo(1000))
            .statusCode(200)
            .when().get(CLUSTERED_BASE_URL + "/tc-management-api/agents" + agentsFilter + "/cacheManagers/caches");

    String cachesFilter = ";names=testCache2";
    String cmsFilter = ";names=testCacheManagerProgrammatic";
    expect().statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .when().delete(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter,cmsFilter,cachesFilter);

    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo(agentId))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).attributes.InMemorySize", equalTo(0))
            .statusCode(200)
            .when().get(CLUSTERED_BASE_URL + "/tc-management-api/agents" + agentsFilter + "/cacheManagers/caches");

  }


  @After
  public void  tearDown() {
    if (cacheManagerMaxBytes != null) {
      cacheManagerMaxBytes.shutdown();
    }
  }

}
