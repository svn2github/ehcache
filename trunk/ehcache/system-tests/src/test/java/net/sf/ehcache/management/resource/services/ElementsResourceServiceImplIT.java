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
import static org.hamcrest.Matchers.equalTo;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches/elements endpoint
 * works fine
 */
public class ElementsResourceServiceImplIT extends ResourceServiceImplITHelper{

  public static final int PORT = 12121;
  public static final String BASEURI = "http://localhost";
  private CacheManager manager;
  private static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents/cacheManagers/caches/elements";
  private CacheManager managerProgrammatic;

  @Before
  public void setUp() throws UnsupportedEncodingException {
    RestAssured.baseURI = BASEURI;
    RestAssured.port =  PORT;
    manager = new CacheManager(ElementsResourceServiceImplIT.class.getResource("/management/standalone-ehcache-rest-agent-test.xml"));
    // we configure the second cache manager programmatically
    managerProgrammatic = getCacheManagerProgramatically();
  }

  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest__notSpecifyingCacheOrCacheManager() throws Exception {

    expect().statusCode(400)
            .body("details", equalTo(""))
            .body("error", equalTo("No cache specified. Unsafe requests must specify a single cache name."))
            .given()
            .contentType(ContentType.JSON)
            .when().delete(EXPECTED_RESOURCE_LOCATION);

    expect().statusCode(400)
            .body("details", equalTo(""))
            .body("error", equalTo("No cache manager specified. Unsafe requests must specify a single cache manager name."))
            .given()
            .contentType(ContentType.JSON)
            .when().delete("/tc-management-api/agents/cacheManagers/caches;names=testCache2/elements");

  }

  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest() throws Exception {
    Cache exampleCache = managerProgrammatic.getCache("testCache2");
    for (int i=0; i<1000 ; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
    }


    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).attributes.InMemorySize", equalTo(1000))
            .statusCode(200)
            .when().get("/tc-management-api/agents/cacheManagers/caches");

    expect().statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .when().delete("/tc-management-api/agents/cacheManagers;names=testCacheManagerProgrammatic/caches;names=testCache2/elements");

    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).attributes.InMemorySize", equalTo(0))
            .statusCode(200)
            .when().get("/tc-management-api/agents/cacheManagers/caches");

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
