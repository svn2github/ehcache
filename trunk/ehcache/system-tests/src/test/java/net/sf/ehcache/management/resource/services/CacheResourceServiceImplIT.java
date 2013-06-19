package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.resource.CacheEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.*;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches endpoint
 * works fine
 */
public class CacheResourceServiceImplIT extends ResourceServiceImplITHelper{

  public static final int PORT = 12121;
  public static final String BASEURI = "http://localhost";
  private CacheManager manager;
  private static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents/cacheManagers/caches";
  private CacheManager managerProgrammatic;

  @Before
  public void setUp() throws UnsupportedEncodingException {
    RestAssured.baseURI = BASEURI;
    RestAssured.port =  PORT;
    manager = new CacheManager(CacheResourceServiceImplIT.class.getResource("/management/standalone-ehcache-rest-agent-test.xml"));
    // we configure the second cache manager programmatically
    managerProgrammatic = getCacheManagerProgramatically();
  }

  @Test
  /**
   * - GET the list of caches
   *
   * @throws Exception
   */
  public void getCachesTest() throws Exception {
    /*
 [
     {
        "version": null,
        "agentId": "embedded",
        "name": "testCache2",
        "cacheManagerName": "testCacheManagerProgrammatic",
        "attributes": {
            "OnDiskSize": 1000,
            "CacheOnDiskHitRate": 0,
            "MostRecentRejoinTimestampMillisSample": 0,
            "LocalOffHeapSizeInBytesSample": 0,
            "LocalHeapSizeInBytes": 247920,
            "Searchable": false,
            "CacheHitMostRecentSample": 0,
            "MostRecentRejoinTimeStampMillis": 0,
            "CacheInMemoryHitRate": 0,
            "NonStopFailureSample": 0,
            "CacheHitOffHeapMostRecentSample": 0,
            "NonstopTimeoutRatio": 0,
            "CacheClusterOnlineSample": 0,
            "NonStopTimeoutSample": 0,
            "NonStopFailureRate": 0,
            "LocalHeapCountBased": false,
            "CacheElementPutSample": 0,
            "LoggingEnabled": false,
            "CacheHitRatioMostRecentSample": 0,
            "MaxBytesLocalHeap": 0,
            "XaRecoveredCount": 0,
            "NonStopSuccessRate": 0,
            "EvictedCount": 0,
            "MinGetTimeNanos": null,
            "NodeBulkLoadEnabled": false,
            "MaxBytesLocalOffHeapAsString": "0",
            "CacheSearchRate": 0,
            "CacheElementRemovedMostRecentSample": 0,
            "InMemorySize": 1000,
            "WriterMaxQueueSize": 0,
            "TerracottaConsistency": "na",
            "NonStopRejoinTimeoutSample": 0,
            "CacheHitInMemoryMostRecentSample": 0,
            "CacheElementEvictedSample": 0,
            "WriterConcurrency": 1,
            "CacheMissInMemoryMostRecentSample": 0,
            "CacheHitRatioSample": 0,
            "LocalDiskSize": 1000,
            "OverflowToDisk": true,
            "CacheMissMostRecentSample": 0,
            "LocalOffHeapSize": 0,
            "UpdateCount": 0,
            "InMemoryMissCount": 0,
            "CacheMissExpiredMostRecentSample": 0,
            "CachePutRate": 0,
            "OffHeapMissCount": 0,
            "CacheHitOnDiskMostRecentSample": 0,
            "CacheMissOffHeapMostRecentSample": 0,
            "CacheOnDiskMissRate": 0,
            "DiskPersistent": false,
            "MemoryStoreEvictionPolicy": "LRU",
            "LocalHeapSize": 1000,
            "TimeToIdleSeconds": 0,
            "AverageGetTime": 0,
            "WriterQueueLength": 0,
            "NonStopFailureMostRecentSample": 0,
            "CacheMissOnDiskSample": 0,
            "TransactionCommitRate": 0,
            "NonStopSuccessCount": 0,
            "CacheElementExpiredSample": 0,
            "CacheClusterOfflineMostRecentSample": 0,
            "InMemoryHitCount": 0,
            "XaRollbackCount": 0,
            "SizeSample": 1000,
            "CacheInMemoryMissRate": 0,
            "CacheClusterRejoinMostRecentSample": 0,
            "DiskExpiryThreadIntervalSeconds": 120,
            "NonStopFailureCount": 0,
            "AverageSearchTimeNanos": 0,
            "CacheMissCount": 0,
            "CacheMissOffHeapSample": 0,
            "NonStopRejoinTimeoutRate": 0,
            "MaxBytesLocalOffHeap": 0,
            "CacheClusterOfflineSample": 0,
            "CacheClusterOnlineCount": 0,
            "CacheXaCommitsSample": 0,
            "MaxBytesLocalHeapAsString": "0",
            "CacheClusterOnlineMostRecentSample": 0,
            "CacheMissRate": 0,
            "SearchesPerSecondSample": 0,
            "CacheElementPutMostRecentSample": 0,
            "CacheClusterOfflineCount": 0,
            "WriterQueueLengthSample": 0,
            "CacheElementEvictedMostRecentSample": 0,
            "HasWriteBehindWriter": false,
            "LocalHeapSizeInBytesSample": 247920,
            "MaxBytesLocalDiskAsString": "0",
            "OverflowToOffHeap": false,
            "CacheMissOnDiskMostRecentSample": 0,
            "CacheElementExpiredMostRecentSample": 0,
            "LocalDiskSizeSample": 1000,
            "CacheRemoveRate": 0,
            "CacheElementUpdatedMostRecentSample": 0,
            "CacheMissNotFoundMostRecentSample": 0,
            "LocalDiskSizeInBytes": 246780,
            "AverageGetTimeNanosMostRecentSample": 0,
            "MaxEntriesLocalHeap": 0,
            "CacheOffHeapMissRate": 0,
            "RemoteSizeSample": 0,
            "ClusterBulkLoadEnabled": null,
            "XaCommitCount": 0,
            "Transactional": false,
            "CacheMissCountExpired": 0,
            "CacheUpdateRate": 0,
            "CacheElementUpdatedSample": 0,
            "PinnedToStore": "na",
            "Size": 1000,
            "TerracottaClustered": false,
            "TransactionRollbackRate": 0,
            "CacheHitInMemorySample": 0,
            "LocalHeapSizeSample": 1000,
            "NonStopSuccessSample": 0,
            "CacheMissInMemorySample": 0,
            "NonStopTimeoutRate": 0,
            "CacheMissNotFoundSample": 0,
            "TimeToLiveSeconds": 0,
            "AverageGetTimeSample": 0,
            "CacheHitCount": 0,
            "MaxBytesLocalDisk": 0,
            "CacheHitSample": 0,
            "ExpiredCount": 0,
            "NonStopRejoinTimeoutCount": 0,
            "CacheXaRollbacksSample": 0,
            "CacheMissSample": 0,
            "PutCount": 1000,
            "AverageSearchTimeSample": 0,
            "CacheClusterRejoinSample": 0,
            "Enabled": true,
            "CacheXaCommitsMostRecentSample": 0,
            "CacheXaRollbacksMostRecentSample": 0,
            "CacheHitOffHeapSample": 0,
            "CacheOffHeapHitRate": 0,
            "RemovedCount": 0,
            "CacheHitOnDiskSample": 0,
            "CacheClusterRejoinCount": 0,
            "AverageSearchTime": 0,
            "LocalOffHeapSizeInBytes": 0,
            "MaxEntriesLocalDisk": 0,
            "MaxGetTimeNanos": null,
            "MaxElementsOnDisk": 0,
            "CacheHitRate": 0,
            "LocalOffHeapSizeSample": 0,
            "OffHeapHitCount": 0,
            "CacheExpirationRate": 0,
            "Pinned": false,
            "Eternal": false,
            "NonStopTimeoutMostRecentSample": 0,
            "CacheHitRatio": 0,
            "OffHeapSize": 0,
            "CacheEvictionRate": 0,
            "NonStopTimeoutCount": 0,
            "SearchesPerSecond": 0,
            "MaxEntriesInCache": 0,
            "CacheMissExpiredSample": 0,
            "LocalDiskSizeInBytesSample": 246780,
            "Status": "STATUS_ALIVE",
            "OnDiskMissCount": 0,
            "NonStopRejoinTimeoutMostRecentSample": 0,
            "OnDiskHitCount": 0,
            "NonStopSuccessMostRecentSample": 0,
            "PersistenceStrategy": "",
            "AverageGetTimeNanos": 0,
            "CacheElementRemovedSample": 0
        }
    },
]
     */


    Cache exampleCache = managerProgrammatic.getCache("testCache2");
    for (int i=0; i<1000 ; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
    }

    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.LocalHeapSizeInBytes", greaterThan(0))
            .body("get(0).attributes.InMemorySize", equalTo(1000))
            .body("get(0).attributes.LocalDiskSize", greaterThan(0))
            .body("get(0).attributes.LocalHeapSize", equalTo(1000))
            .body("get(0).attributes.SizeSample", equalTo(1000))
            .body("get(0).attributes.DiskExpiryThreadIntervalSeconds", equalTo(120))
            .body("get(0).attributes.LocalHeapSizeInBytesSample", greaterThan(0))
            .body("get(0).attributes.LocalDiskSizeSample", greaterThan(0))
            .body("get(0).attributes.LocalDiskSizeInBytes", greaterThan(0))
            .body("get(0).attributes.Size", equalTo(1000))
            .body("get(0).attributes.LocalHeapSizeSample", equalTo(1000))
            .body("get(0).attributes.PutCount", equalTo(1000))
            .body("get(0).attributes.LocalDiskSizeInBytesSample", greaterThan(0))
            .body("get(0).attributes.Status", equalTo( "STATUS_ALIVE"))
            .body("get(1).name", equalTo("testCache"))
            .body("size()", is(2))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION);


    // we filter to return only the attribute CacheNames, and working only on the testCache2 Cache
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
            .body("get(0).attributes.PutCount", equalTo(1000))
            .body("get(0).attributes.Size", equalTo(1000))
            .body("get(0).attributes.LocalHeapSizeSample", nullValue())
            .body("size()",is(1))
            .statusCode(200)
            .given()
              .queryParam("show", "Size")
              .queryParam("show", "PutCount")
            .when().get(EXPECTED_RESOURCE_LOCATION + ";names=testCache2");


  }

  @Test
  /**
   * - PUT an updated CacheEntity
   *
   * @throws Exception
   */
  public void updateCachesTest__FailWhenNotSpecifyingACache() throws Exception {
    // you have to specify a cache when doing mutation
    CacheEntity cacheManagerEntity = new CacheEntity();
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxEntriesLocalHeap",20000);
    attributes.put("Enabled", Boolean.FALSE);
    cacheManagerEntity.getAttributes().putAll(attributes);

    expect().statusCode(400)
            .body("details", equalTo(""))
            .body("error", equalTo("No cache specified. Unsafe requests must specify a single cache name."))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put(EXPECTED_RESOURCE_LOCATION);

    expect().statusCode(400)
            .body("details", equalTo(""))
            .body("error", equalTo("No cache manager specified. Unsafe requests must specify a single cache manager name."))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put("/tc-management-api/agents/cacheManagers/caches;names=testCache2");

    expect().statusCode(400)
            .body("details", equalTo("Cache not found !"))
            .body("error", equalTo("Failed to create or update cache"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put("/tc-management-api/agents/cacheManagers;names=testCacheManagerProgrammatic/caches;names=boups");

    expect().statusCode(400)
            .body("details", equalTo("CacheManager not found !"))
            .body("error", equalTo("Failed to create or update cache"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put("/tc-management-api/agents/cacheManagers;names=pif/caches;names=testCache2");

    // we check nothing has changed
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache2"))
            .body("get(0).attributes.MaxEntriesLocalHeap",equalTo(0) )
            .body("get(0).attributes.Enabled", equalTo(Boolean.TRUE))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION);
  }

  @Test
  /**
   * - PUT an updated CacheEntity
   *
   * Those are the mutable attributes from the rest agent, followed by Gary's comments
   * ENABLED_ATTR: the user can change directly from the management panel
   * LOGGING_ENABLED: just never had this in the DevConsole and nobody's ever asked for it
   * BULK_LOAD_ENABLED: will probably be adding this with the management panel overhaul
   * MAX_ENTRIES_LOCAL_HEAP: we do support this, but not when you've already specified MAX_BYTES_LOCAL_HEAP
   * MAX_ELEMENTS_ON_DISK: same as above, except you've already specified MAX_BYTES_LOCAL_HEAP
   * MAX_ENTRIES_IN_CACHE: if it's a Terracotta-clustered cache, we support this
   * MAX_BYTES_LOCAL_DISK_STRING
   * MAX_BYTES_LOCAL_HEAP_STRING
   * TIME_TO_IDLE_SEC
   * TIME_TO_LIVE_SEC
   *
   * @throws Exception
   */
  public void updateCachesTest() throws Exception {
    // you have to specify a cache when doing mutation
    CacheEntity cacheEntity = new CacheEntity();
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxEntriesInCache", 30000);
    attributes.put("MaxEntriesLocalHeap",20000);
    attributes.put("LoggingEnabled", Boolean.TRUE);
    attributes.put("MaxElementsOnDisk",40000);
    attributes.put("TimeToIdleSeconds", 20);
    attributes.put("TimeToLiveSeconds", 43);
//    attributes.put("ClusterBulkLoadEnabled", Boolean.TRUE); ONLY FOR CLUSTERED !!!
    attributes.put("Enabled", Boolean.FALSE);

    cacheEntity.getAttributes().putAll(attributes);
    expect().statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .body(cacheEntity)
            .when().put("/tc-management-api/agents/cacheManagers;names=testCacheManager/caches;names=testCache");


    // we check the properties were changed
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache"))
            .body("get(0).attributes.MaxEntriesInCache", equalTo(30000))
            .body("get(0).attributes.MaxEntriesLocalHeap", equalTo(20000))
            .body("get(0).attributes.LoggingEnabled", equalTo(Boolean.TRUE))
            .body("get(0).attributes.MaxElementsOnDisk", equalTo(40000))
//            .body("get(0).attributes.ClusterBulkLoadEnabled",equalTo(Boolean.TRUE) ) ONLY FOR CLUSTERED !!!
            .body("get(0).attributes.TimeToIdleSeconds", equalTo(20))
            .body("get(0).attributes.TimeToLiveSeconds", equalTo(43))
            .body("get(0).attributes.Enabled", equalTo(Boolean.FALSE))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION + ";names=testCache");

    // I need another cache that does not have set MaxBytesLocalHeap nor MaxBytesLocalDisk
    Configuration configuration = new Configuration();
    configuration.setName("cacheManagerNew");

    CacheConfiguration myCache = new CacheConfiguration()
            .eternal(false).name("CacheNew");
    myCache.setMaxBytesLocalHeap("5M");
    myCache.setMaxBytesLocalDisk("3M");
    configuration.addCache(myCache);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:12121");
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);
    CacheManager mgr = new CacheManager(configuration);

    cacheEntity = new CacheEntity();
    attributes = new HashMap<String, Object>();
    attributes.put("MaxBytesLocalDiskAsString", "30M");
    attributes.put("MaxBytesLocalHeapAsString","20M");

    cacheEntity.getAttributes().putAll(attributes);
    expect().log().ifStatusCodeIsEqualTo(400)
            .statusCode(204)
            .given()
            .contentType(ContentType.JSON)
            .body(cacheEntity)
            .when().put("/tc-management-api/agents/cacheManagers;names=cacheManagerNew/caches;names=CacheNew");


    // we check the properties were changed
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("CacheNew"))
            .body("get(0).attributes.MaxBytesLocalDiskAsString", equalTo("30M"))
            .body("get(0).attributes.MaxBytesLocalHeapAsString", equalTo("20M"))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION + ";names=CacheNew");
    mgr.removeAllCaches();
    mgr.shutdown();


  }


  @Test
  /**
   * - PUT an updated CacheEntity, with attributes not allowed
   * only 6 attributes are supported (cf previosu test), the others are forbidden because we do not allow them to be updated
   * @throws Exception
   */
  public void updateCachesTest__FailWhenMutatingForbiddenAttributes() throws Exception {

    CacheEntity cacheManagerEntity = new CacheEntity();
    cacheManagerEntity.setName("superName");
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("LocalOffHeapSizeInBytes","20000");
    attributes.put("Pinned", Boolean.TRUE);
    cacheManagerEntity.getAttributes().putAll(attributes);
    expect().statusCode(400)
            .body("details", equalTo("You are not allowed to update those attributes : name LocalOffHeapSizeInBytes Pinned . " +
                    "Only TimeToIdleSeconds Enabled MaxBytesLocalDiskAsString MaxBytesLocalHeapAsString MaxElementsOnDisk " +
                    "ClusterBulkLoadEnabled TimeToLiveSeconds MaxEntriesLocalHeap LoggingEnabled MaxEntriesInCache can be updated for a Cache."))
            .body("error", equalTo("Failed to create or update cache"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheManagerEntity)
            .when().put("/tc-management-api/agents/cacheManagers;names=testCacheManager/caches;names=testCache");

    // we check nothing has changed
    expect().contentType(ContentType.JSON)
            .body("get(0).agentId", equalTo("embedded"))
            .body("get(0).name", equalTo("testCache"))
            .body("get(0).attributes.LocalOffHeapSizeInBytes", equalTo(0))
            .body("get(0).attributes.Pinned", equalTo(Boolean.FALSE))
            .body("size()",is(1))
            .statusCode(200)
            .when().get(EXPECTED_RESOURCE_LOCATION + ";names=testCache");
  }



  @Test
  /**
   * - PUT an updated CacheEntity
   * @throws Exception
   */
  public void updateCachesTest__CacheManagerDoesNotExist() throws Exception {

    CacheEntity cacheEntity = new CacheEntity();
    expect().statusCode(400)
            .body("details", equalTo("CacheManager not found !"))
            .body("error", equalTo("Failed to create or update cache"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheEntity)
            .when().put("/tc-management-api/agents/cacheManagers;names=cachemanagerDoesNotExist/caches;names=testCache");
  }


  @Test
  /**
   * - PUT a CacheEntity, not matching any known caches : creating is not allowed
   *
   * @throws Exception
   */
  public void updateCachesTest__CantCreateCache() throws Exception {

    CacheEntity cacheEntity = new CacheEntity();
    expect().statusCode(400)
            .body("details", equalTo("Cache not found !"))
            .body("error", equalTo("Failed to create or update cache"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheEntity)
            .when().put("/tc-management-api/agents/cacheManagers;names=testCacheManager/caches;names=cacheThatDoesNotExist");
  }

  @Test
  /**
   * - DELETE a CacheEntity
   *
   * @throws Exception
   */
  public void deleteCachesTest__NotYetImplemented() throws Exception {

    CacheEntity cacheEntity = new CacheEntity();
    expect().statusCode(501)
            .body("details", equalTo(""))
            .body("error", equalTo("Not yet implemented"))
            .given()
            .contentType(ContentType.JSON)
            .body(cacheEntity)
            .when().delete("/tc-management-api/agents/cacheManagers;names=testCacheManager/caches;names=testCache");
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
