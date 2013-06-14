package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.*;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/ endpoint
 * works fine
 */
public class CacheManagersResourceServiceImplIT extends ResourceServiceImplITHelper{

  public static final int PORT = 12121;
  public static final String BASEURI = "http://localhost";
  private CacheManager manager;
  private static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents/cacheManagers";
  private CacheManager managerProgrammatic;

  @Before
  public void setUp() throws UnsupportedEncodingException {
    RestAssured.baseURI = BASEURI;
    RestAssured.port =  PORT;
    manager = new CacheManager(CacheManagersResourceServiceImplIT.class.getResource("/management/standalone-ehcache-rest-agent-test.xml"));
    // we configure the second cache manager programmatically
    managerProgrammatic = getCacheManagerProgramatically();
  }

  @Test
  /**
   * - GET the list of cacheManagers
   *
   * @throws Exception
   */
  public void getCacheManagersTest() throws Exception {
    /*
      [
      {
        "version": null,
        "name": "testCacheManagerProgrammatic",
        "agentId": "embedded",
        "attributes": {
          "ClusterUUID": "",
          "Enabled": true,
          "HasWriteBehindWriter": false,
          "MaxBytesLocalDiskAsString": "0",
          "CacheAverageSearchTime": 0,
          "CacheOnDiskHitRate": 0,
          "CachePutRate": 0,
          "CacheMetrics": {
            "testCache2": [
              0,
              0,
              0,
              0
            ]
          },
          "CacheRemoveRate": 0,
          "CacheOffHeapHitRate": 0,
          "Searchable": false,
          "CacheOnDiskMissRate": 0,
          "CacheNames": [
            "testCache2"
          ],
          "TransactionRolledBackCount": 0,
          "CacheInMemoryHitRate": 0,
          "WriterQueueLength": 0,
          "CacheOffHeapMissRate": 0,
          "Transactional": false,
          "CacheHitRate": 0,
          "TransactionCommitRate": 0,
          "CacheExpirationRate": 0,
          "CacheUpdateRate": 0,
          "MaxBytesLocalHeap": 0,
          "CacheAverageGetTime": 0,
          "TransactionRollbackRate": 0,
          "CacheEvictionRate": 0,
          "CacheInMemoryMissRate": 0,
          "MaxBytesLocalDisk": 0,
          "MaxBytesLocalOffHeapAsString": "0",
          "CacheSearchRate": 0,
          "TransactionTimedOutCount": 0,
          "TransactionCommittedCount": 0,
          "Status": "STATUS_ALIVE",
          "MaxBytesLocalOffHeap": 0,
          "WriterMaxQueueSize": 0,
          "MaxBytesLocalHeapAsString": "0",
          "CacheMissRate": 0
        }
      },
      {
        "version": null,
        "name": "testCacheManager",
        "agentId": "embedded",
        "attributes": {
          "ClusterUUID": "",
          "Enabled": true,
          "HasWriteBehindWriter": false,
          "MaxBytesLocalDiskAsString": "0",
          "CacheAverageSearchTime": 0,
          "CacheOnDiskHitRate": 0,
          "CachePutRate": 0,
          "CacheMetrics": {
            "testCache": [
              0,
              0,
              0,
              0
            ]
          },
          "CacheRemoveRate": 0,
          "CacheOffHeapHitRate": 0,
          "Searchable": false,
          "CacheOnDiskMissRate": 0,
          "CacheNames": [
            "testCache"
          ],
          "TransactionRolledBackCount": 0,
          "CacheInMemoryHitRate": 0,
          "WriterQueueLength": 0,
          "CacheOffHeapMissRate": 0,
          "Transactional": false,
          "CacheHitRate": 0,
          "TransactionCommitRate": 0,
          "CacheExpirationRate": 0,
          "CacheUpdateRate": 0,
          "MaxBytesLocalHeap": 0,
          "CacheAverageGetTime": 0,
          "TransactionRollbackRate": 0,
          "CacheEvictionRate": 0,
          "CacheInMemoryMissRate": 0,
          "MaxBytesLocalDisk": 0,
          "MaxBytesLocalOffHeapAsString": "0",
          "CacheSearchRate": 0,
          "TransactionTimedOutCount": 0,
          "TransactionCommittedCount": 0,
          "Status": "STATUS_ALIVE",
          "MaxBytesLocalOffHeap": 0,
          "WriterMaxQueueSize": 0,
          "MaxBytesLocalHeapAsString": "0",
          "CacheMissRate": 0
        }
      }
    ]
     */

    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.CacheMetrics.testCache2", hasItems(0, 0, 0, 0))
            .body("get(0).attributes.CacheNames.get(0)", equalTo("testCache2"))
            .body("get(1).agentId", equalTo("embedded"))
            .body("get(1).name", equalTo("testCacheManager"))
            .body("get(1).attributes.CacheMetrics.testCache", hasItems(0, 0, 0, 0))
            .body("get(1).attributes.CacheNames.get(0)", equalTo("testCache"))
            .body("size()", is(2))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION);

    // we filter to return only the attribute CacheNames, and working only on the testCacheManagerProgrammatic CM
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.CacheMetrics.testCache2", hasItems(0, 0, 0, 0))
            .body("get(0).attributes.CacheNames.get(0)", equalTo("testCache2"))
            .body("size()",is(1))
            .statusCode(200)
            .given()
              .queryParam("show", "CacheMetrics")
              .queryParam("show", "CacheNames")
            .when().get(EXPECTED_RESOURCE_LOCATION + ";names=testCacheManagerProgrammatic");

  }

  @Test
  /**
   * - PUT an updated CacheManagerEntity
   *
   * @throws Exception
   */
  public void updateCacheManagersTest__FailWhenNotSpecifyingACacheManager() throws Exception {
    // you have to specify a cacheManager when doing mutation
    CacheManagerEntity cacheManagerEntity = new CacheManagerEntity();
    cacheManagerEntity.setAgentId("superId");
    cacheManagerEntity.setName("superName");
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("Searchable",Boolean.TRUE);
    attributes.put("Enabled", Boolean.FALSE);
    cacheManagerEntity.getAttributes().putAll(attributes);
    expect().contentType(ContentType.JSON)
            .statusCode(400)
            .given()
              .contentType(ContentType.JSON)
              .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION);


    // we check nothing has changed
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.CacheMetrics.testCache2", hasItems(0,0,0,0))
            .body("get(0).attributes.CacheNames.get(0)", equalTo("testCache2"))
            .body("get(1).agentId", equalTo("embedded"))
            .body("get(1).name", equalTo("testCacheManager"))
            .body("get(1).attributes.CacheMetrics.testCache", hasItems(0,0,0,0))
            .body("get(1).attributes.CacheNames.get(0)", equalTo("testCache"))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION);
  }


  @Test
  /**
   * - PUT an updated CacheManagerEntity
   * only 2 attributes are supported
   * @throws Exception
   */
  public void updateCacheManagersTest() throws Exception {

    // you have to specify a cacheManager when doing mutation
    CacheManagerEntity cacheManagerEntity = new CacheManagerEntity();
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxBytesLocalHeapAsString","20M");
    attributes.put("MaxBytesLocalDiskAsString", "40M");
    cacheManagerEntity.getAttributes().putAll(attributes);
    expect().log().ifError()
            .statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION + ";names=testCacheManagerProgrammatic");


    expect()
            .contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.MaxBytesLocalHeapAsString", equalTo("20M"))
            .body("get(0).attributes.MaxBytesLocalDiskAsString", equalTo("40M"))
            .body("size()",is(1))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION + ";names=testCacheManagerProgrammatic");
  }


  @Test
  @Ignore
  /**
   * - PUT an updated CacheManagerEntity, with attributes not allowed
   * only 2 attributes are supported, the others are forbidden because we do not allow them to be updated
   * @throws Exception
   */
  public void updateCacheManagersTest__FailWhenSpecifyingForbiddenAttributes() throws Exception {

    // you have to specify a cacheManager when doing mutation
    CacheManagerEntity cacheManagerEntity = new CacheManagerEntity();
    cacheManagerEntity.setName("superName");
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxBytesLocalHeap","20000");
    attributes.put("MaxBytesLocalDisk", "40000");
    cacheManagerEntity.getAttributes().putAll(attributes);
    expect().log().ifError()
            .statusCode(400)
            .body("details", equalTo("You are not allowed to update those attributes : name MaxBytesLocalDisk MaxBytesLocalHeap . " +
                    "Only MaxBytesLocalDiskAsString and MaxBytesLocalHeapAsString can be updated for a CacheManager."))
            .body("error", equalTo("Failed to update cache manager"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION + ";names=testCacheManagerProgrammatic");

    // we check nothing has changed
    expect()
            .contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.MaxBytesLocalHeapAsString", equalTo("5M"))
            .body("get(0).attributes.MaxBytesLocalDiskAsString", equalTo("10M"))
            .body("size()",is(1))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION + ";names=testCacheManagerProgrammatic");
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
