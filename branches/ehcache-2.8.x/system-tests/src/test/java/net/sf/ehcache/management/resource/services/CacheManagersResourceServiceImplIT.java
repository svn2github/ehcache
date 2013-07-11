package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
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
public class CacheManagersResourceServiceImplIT extends ResourceServiceImplITHelper {

  protected static final String EXPECTED_RESOURCE_LOCATION = "{baseUrl}/tc-management-api/agents{agentIds}/cacheManagers{cmIds}";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(CacheManagersResourceServiceImplIT.class);
  }

  @Before
  public void setUp() throws UnsupportedEncodingException {
    cacheManagerProgrammatic = getCacheManagerProgrammatic();
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
    String agentsFilter = "";
    String cmsFilter = "";
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
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);

    cmsFilter = ";names=testCacheManagerProgrammatic";
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
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);

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
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("Searchable",Boolean.TRUE);
    attributes.put("Enabled", Boolean.FALSE);
    cacheManagerEntity.getAttributes().putAll(attributes);

    String agentsFilter = "";
    String cmsFilter = "";

    expect().statusCode(400)
            .body("details", equalTo(""))
            .body("error", equalTo("No cache manager specified. Unsafe requests must specify a single cache manager name."))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);

    cmsFilter = ";names=pif";
    expect().statusCode(400)
            .body("details", equalTo("CacheManager not found !"))
            .body("error", equalTo("Failed to update cache manager"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);


    cmsFilter = "";
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
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);
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

    String agentsFilter = "";
    String cmsFilter = ";names=testCacheManagerProgrammatic";

    expect().log().ifError()
            .statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);


    expect()
            .contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.MaxBytesLocalHeapAsString", equalTo("20M"))
            .body("get(0).attributes.MaxBytesLocalDiskAsString", equalTo("40M"))
            .body("size()",is(1))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);
  }


  @Test
  /**
   * - PUT an updated CacheManagerEntity
   * only 2 attributes are supported
   * @throws Exception
   */
  public void updateCacheManagersTest__clustered() throws Exception {

    // you have to specify a cacheManager when doing mutation
    CacheManagerEntity cacheManagerEntity = new CacheManagerEntity();
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxBytesLocalHeapAsString","12M");
    attributes.put("MaxBytesLocalDiskAsString", "6M");
    cacheManagerEntity.getAttributes().putAll(attributes);

    String agentId = getEhCacheAgentId();
    final String agentsFilter = ";ids=" + agentId;
    String cmsFilter = ";names=testCacheManagerProgrammatic";

    expect().log().ifError()
            .statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter,cmsFilter);


    expect()
            .contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo(agentId))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.MaxBytesLocalHeapAsString", equalTo("12M"))
            .body("get(0).attributes.MaxBytesLocalDiskAsString", equalTo("6M"))
            .body("size()",is(1))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter,cmsFilter);
  }


  @Test
  /**
   * - PUT an updated CacheManagerEntity, with attributes not allowed
   * only 2 attributes are supported, the others are forbidden because we do not allow them to be updated
   * @throws Exception
   */
  public void updateCacheManagersTest__FailWhenMutatingForbiddenAttributes() throws Exception {

    // you have to specify a cacheManager when doing mutation
    CacheManagerEntity cacheManagerEntity = new CacheManagerEntity();
    cacheManagerEntity.setName("superName");
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxBytesLocalHeap","20000");
    attributes.put("MaxBytesLocalDisk", "40000");
    cacheManagerEntity.getAttributes().putAll(attributes);

    String agentsFilter = "";
    String cmsFilter = ";names=testCacheManagerProgrammatic";

    expect().log().ifError()
            .statusCode(400)
            .body("details", equalTo("You are not allowed to update those attributes : name MaxBytesLocalDisk MaxBytesLocalHeap . " +
                    "Only MaxBytesLocalDiskAsString and MaxBytesLocalHeapAsString can be updated for a CacheManager."))
            .body("error", equalTo("Failed to update cache manager"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);

    // we check nothing has changed
    expect()
            .contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.MaxBytesLocalHeapAsString", equalTo("5M"))
            .body("get(0).attributes.MaxBytesLocalDiskAsString", equalTo("10M"))
            .body("size()",is(1))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);
  }


  @Test
  /**
   * - PUT an updated CacheManagerEntity
   * @throws Exception
   */
  public void updateCacheManagersTest__CacheManagerDoesNotExist() throws Exception {

    // you have to specify a cacheManager when doing mutation
    CacheManagerEntity cacheManagerEntity = new CacheManagerEntity();

    String agentsFilter = "";
    String cmsFilter = ";names=CacheManagerDoesNotExist";
    expect().log().ifStatusCodeIsEqualTo(404)
            .statusCode(400)
            .body("details", equalTo("CacheManager not found !"))
            .body("error", equalTo("Failed to update cache manager"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter);
  }

  @After
  public void  tearDown() {
    if (cacheManagerProgrammatic != null) {
      cacheManagerProgrammatic.shutdown();
    }
  }

}
