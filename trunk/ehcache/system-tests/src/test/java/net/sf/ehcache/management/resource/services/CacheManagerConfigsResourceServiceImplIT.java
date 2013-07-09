package net.sf.ehcache.management.resource.services;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.internal.path.xml.NodeChildrenImpl;
import com.jayway.restassured.internal.path.xml.NodeImpl;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.element.Node;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.given;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/config endpoint
 * works fine
 * @author Anthony Dahanne
 */
public class CacheManagerConfigsResourceServiceImplIT extends ResourceServiceImplITHelper {

  protected static final String EXPECTED_RESOURCE_LOCATION = "{baseUrl}/tc-management-api/agents{agentIds}/cacheManagers{cmIds}/configs";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(CacheManagerConfigsResourceServiceImplIT.class);
  }

  @Before
  public void setUp() throws UnsupportedEncodingException {
    cacheManagerProgrammatic = getCacheManagerProgrammatic();
  }

  @Test
  /**
   * - GET the list of cacheManagers configs
   *
   * @throws Exception
   */
  public void getCacheManagersTest() throws Exception {
    /*
    <configurations>
      <configuration agentId="embedded" cacheManagerName="testCacheManagerProgrammatic">
        <ehcache maxBytesLocalDisk="10M" maxBytesLocalHeap="5M" name="testCacheManagerProgrammatic">
          <managementRESTService bind="0.0.0.0:12121" enabled="true" securityServiceTimeout="0"/>
          <defaultCache/>
          <cache name="testCache2"/>
        </ehcache>
      </configuration>
    </configurations>
     */

    String agentsFilter = "";
    String cmsFilter = "";
    String xml = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter, cmsFilter).asString();
    XmlPath xmlPath = new XmlPath(xml).setRoot("configurations");
    NodeImpl configuration = xmlPath.get("configuration[0]");
    assertEquals("embedded", configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    Node ehcacheNode = configuration.get(0);
    assertEquals("10M", ehcacheNode.attributes().get("maxBytesLocalDisk"));
    assertEquals("5M", ehcacheNode.attributes().get("maxBytesLocalHeap"));
    assertEquals("testCacheManagerProgrammatic", ehcacheNode.attributes().get("name"));
    Node managementNode = ehcacheNode.get("managementRESTService");
    assertEquals("0.0.0.0:12121", managementNode.attributes().get("bind"));
    assertEquals("true", managementNode.attributes().get("enabled"));
    Node cacheNode = ehcacheNode.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));


/*
    configuration = xmlPath.get("configuration[1]");
    assertEquals("embedded", configuration.attributes().get("agentId"));
    assertEquals("testCacheManager", configuration.attributes().get("cacheManagerName"));
    ehcacheNode = configuration.get(0);
    assertEquals("testCacheManager", ehcacheNode.attributes().get("name"));
    managementNode = ehcacheNode.get("managementRESTService");
    assertEquals("0.0.0.0:12121", managementNode.attributes().get("bind"));
    assertEquals("true", managementNode.attributes().get("enabled"));
    assertEquals("0", managementNode.attributes().get("securityServiceTimeout"));
    cacheNode = ehcacheNode.get("cache");
    assertEquals("testCache", cacheNode.attributes().get("name"));
    assertEquals("10000", cacheNode.attributes().get("maxEntriesLocalHeap"));
*/


    //same thing but we specify only a given cacheManager
    agentsFilter = "";
    cmsFilter = ";names=testCacheManagerProgrammatic";
    xml = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, STANDALONE_BASE_URL, agentsFilter, cmsFilter).asString();
    xmlPath = new XmlPath(xml).setRoot("configurations");
    configuration = xmlPath.get("configuration[0]");
    assertEquals("embedded", configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    ehcacheNode = configuration.get(0);
    assertEquals("10M", ehcacheNode.attributes().get("maxBytesLocalDisk"));
    assertEquals("5M", ehcacheNode.attributes().get("maxBytesLocalHeap"));
    assertEquals("testCacheManagerProgrammatic", ehcacheNode.attributes().get("name"));
    managementNode = ehcacheNode.get("managementRESTService");
    assertEquals("0.0.0.0:12121", managementNode.attributes().get("bind"));
    assertEquals("true", managementNode.attributes().get("enabled"));
    cacheNode = ehcacheNode.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));

    NodeChildrenImpl configurationNull = xmlPath.get("configuration[1]");
    assertTrue(configurationNull.isEmpty());

  }

  @Test
  public void getCacheManagersTest__clustered() throws Exception {
    String agentId = getEhCacheAgentId();
    final String agentsFilter = ";ids=" + agentId;
    String cmsFilter = "";

    String xmlThroughClustered = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter, cmsFilter).asString();
    XmlPath xmlPath = new XmlPath(xmlThroughClustered).setRoot("configurations");
    NodeImpl configuration = xmlPath.get("configuration[0]");
    assertEquals(agentId, configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    Node ehcacheNode = configuration.get(0);
    assertEquals("10M", ehcacheNode.attributes().get("maxBytesLocalDisk"));
    assertEquals("5M", ehcacheNode.attributes().get("maxBytesLocalHeap"));
    assertEquals("testCacheManagerProgrammatic", ehcacheNode.attributes().get("name"));
    Node managementNode = ehcacheNode.get("managementRESTService");
    assertEquals("0.0.0.0:12121", managementNode.attributes().get("bind"));
    assertEquals("true", managementNode.attributes().get("enabled"));
    Node cacheNode = ehcacheNode.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));


/*
    configuration = xmlPath.get("configuration[1]");
    assertEquals(agentId, configuration.attributes().get("agentId"));
    assertEquals("testCacheManager", configuration.attributes().get("cacheManagerName"));
    ehcacheNode = configuration.get(0);
    assertEquals("testCacheManager", ehcacheNode.attributes().get("name"));
    managementNode = ehcacheNode.get("managementRESTService");
    assertEquals("0.0.0.0:12121", managementNode.attributes().get("bind"));
    assertEquals("true", managementNode.attributes().get("enabled"));
    assertEquals("0", managementNode.attributes().get("securityServiceTimeout"));
    cacheNode = ehcacheNode.get("cache");
    assertEquals("testCache", cacheNode.attributes().get("name"));
    assertEquals("10000", cacheNode.attributes().get("maxEntriesLocalHeap"));
*/


    //same thing but we specify only a given cacheManager
    cmsFilter = ";names=testCacheManagerProgrammatic";
    xmlThroughClustered = given().header("accept", "application/xml").get(EXPECTED_RESOURCE_LOCATION, CLUSTERED_BASE_URL, agentsFilter, cmsFilter).asString();
    xmlPath = new XmlPath(xmlThroughClustered).setRoot("configurations");
    configuration = xmlPath.get("configuration[0]");
    assertEquals(agentId, configuration.attributes().get("agentId"));
    assertEquals("testCacheManagerProgrammatic", configuration.attributes().get("cacheManagerName"));
    ehcacheNode = configuration.get(0);
    assertEquals("10M", ehcacheNode.attributes().get("maxBytesLocalDisk"));
    assertEquals("5M", ehcacheNode.attributes().get("maxBytesLocalHeap"));
    assertEquals("testCacheManagerProgrammatic", ehcacheNode.attributes().get("name"));
    managementNode = ehcacheNode.get("managementRESTService");
    assertEquals("0.0.0.0:12121", managementNode.attributes().get("bind"));
    assertEquals("true", managementNode.attributes().get("enabled"));
    cacheNode = ehcacheNode.get("cache");
    assertEquals("testCache2", cacheNode.attributes().get("name"));

    NodeChildrenImpl configurationNull = xmlPath.get("configuration[1]");
    assertTrue(configurationNull.isEmpty());

  }


  @After
  public void tearDown() {
    if (cacheManagerProgrammatic != null) {
      cacheManagerProgrammatic.shutdown();
    }
  }

}
