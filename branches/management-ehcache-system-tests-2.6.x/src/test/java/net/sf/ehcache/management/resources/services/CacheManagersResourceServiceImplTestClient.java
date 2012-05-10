package net.sf.ehcache.management.resources.services;

import net.sf.ehcache.tests.AbstractEhcacheManagementClientBase;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Ludovic Orban
 */
public class CacheManagersResourceServiceImplTestClient extends AbstractEhcacheManagementClientBase {

  public CacheManagersResourceServiceImplTestClient(String[] args) {
    super(args);
 }

  public static void main(String[] args) {
    new CacheManagersResourceServiceImplTestClient(args).run();
  }

  @Test
  public void testGetCacheManagers() {
    given()
        .expect()
        .statusCode(200)
        .body("get(0).agentId", equalTo("embedded"))
        .body("get(0).name", equalTo("CM1"))
        .body("get(0).attributes.CacheNames", allOf(hasItem("cache1"), hasItem("cache2")))
        .when()
        .get("/agents;ids=embedded/cacheManagers");

    /*
     * Expect successful request with an empty response body if making a request that includes only invalid cache
     * manager names in the names matrix parameter. Its possible to identify a list of names, some of which are
     * currently valid, some of which are not. We return what we can find rather than throwing because one or more
     * cache manager names are invalid.
     */
    given()
        .expect()
        .statusCode(200)
        .when()
        .get("/agents;ids=embedded/cacheManagers;names=CM1,CM2");

    given()
        .expect()
        .statusCode(200)
        .when()
        .get("/agents;ids=embedded/cacheManagers;names=CM2");
  }

}
