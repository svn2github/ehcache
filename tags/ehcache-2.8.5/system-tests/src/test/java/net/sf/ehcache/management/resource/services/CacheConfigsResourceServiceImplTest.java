package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.internal.path.xml.NodeChildrenImpl;
import com.jayway.restassured.internal.path.xml.NodeImpl;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.element.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.given;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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
    /*
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <configurations>
        <configuration agentId="embedded" cacheManagerName="testCacheManagerProgrammatic" cacheName="testCache2">
          <cache name="testCache2"/>
        </configuration>
        <configuration agentId="embedded" cacheManagerName="testCacheManager" cacheName="testCache">
          <cache maxEntriesLocalHeap="10000" name="testCache"/>
        </configuration>
    </configurations>

     */
    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";

    String xml = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter,cachesFilter).asString();
    System.out.println(xml);
    XmlPath xmlPath = new XmlPath(xml).setRoot("configurations");
    NodeImpl configuration = xmlPath.get("configuration[0]");
    assertEquals("embedded", configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    assertEquals("testCache2", configuration.attributes().get("cacheName"));
    Node cacheNode = configuration.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));


/*
    configuration = xmlPath.get("configuration[1]");
    assertEquals("embedded", configuration.attributes().get("agentId"));
    assertEquals("testCacheManager", configuration.attributes().get("cacheManagerName"));
    assertEquals("testCache", configuration.attributes().get("cacheName"));
    cacheNode = configuration.get("cache");
    assertEquals("testCache", cacheNode.attributes().get("name"));
    assertEquals("10000", cacheNode.attributes().get("maxEntriesLocalHeap"));
*/


    //same thing but we specify only a given cacheManager
    agentsFilter = "";
    cmsFilter = ";names=testCacheManagerProgrammatic";
    cachesFilter = ";names=testCache2";
    xml = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter,cmsFilter,cachesFilter).asString();
    System.out.println(xml);
    xmlPath = new XmlPath(xml).setRoot("configurations");
    configuration = xmlPath.get("configuration[0]");
    assertEquals("embedded", configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    assertEquals("testCache2", configuration.attributes().get("cacheName"));
    cacheNode = configuration.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));

    NodeChildrenImpl configurationNull = xmlPath.get("configuration[1]");
    assertTrue(configurationNull.isEmpty());

  }

  @Test
  public void getCacheConfigsTest__clustered() throws Exception {

    String cmsFilter = "";
    String cachesFilter = "";
    String agentId = getEhCacheAgentId();

    final String agentsFilter = ";ids=" + agentId;

    String xmlThroughClustered = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter,cmsFilter,cachesFilter).asString();

    System.out.println(xmlThroughClustered);
    XmlPath xmlPath = new XmlPath(xmlThroughClustered).setRoot("configurations");
    NodeImpl configuration = xmlPath.get("configuration[0]");
    assertEquals(agentId, configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    assertEquals("testCache2", configuration.attributes().get("cacheName"));
    Node cacheNode = configuration.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));


/*
    configuration = xmlPath.get("configuration[1]");
    assertEquals(agentId, configuration.attributes().get("agentId"));
    assertEquals("testCacheManager", configuration.attributes().get("cacheManagerName"));
    assertEquals("testCache", configuration.attributes().get("cacheName"));
    cacheNode = configuration.get("cache");
    assertEquals("testCache", cacheNode.attributes().get("name"));
    assertEquals("10000", cacheNode.attributes().get("maxEntriesLocalHeap"));
*/


    //same thing but we specify only a given cacheManager
    cmsFilter = ";names=testCacheManagerProgrammatic";
    cachesFilter = ";names=testCache2";
    xmlThroughClustered = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter,cmsFilter,cachesFilter).asString();
    System.out.println(xmlThroughClustered);
    xmlPath = new XmlPath(xmlThroughClustered).setRoot("configurations");
    configuration = xmlPath.get("configuration[0]");
    assertEquals(agentId, configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    assertEquals("testCache2", configuration.attributes().get("cacheName"));
    cacheNode = configuration.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));

    NodeChildrenImpl configurationNull = xmlPath.get("configuration[1]");
    assertTrue(configurationNull.isEmpty());


  }

  @After
  public void tearDown() {
    if (cacheManagerMaxBytes != null) {
      cacheManagerMaxBytes.shutdown();
    }
  }

}
