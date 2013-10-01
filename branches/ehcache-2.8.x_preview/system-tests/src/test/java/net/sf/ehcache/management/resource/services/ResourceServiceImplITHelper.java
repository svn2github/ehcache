package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.RestAssured;
import com.tc.test.config.builder.ClusterManager;
import com.tc.test.config.builder.TcConfig;
import com.tc.test.config.builder.TcMirrorGroup;
import com.tc.test.config.builder.TcServer;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.terracotta.test.util.TestBaseUtil;

import java.io.File;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.path.json.JsonPath.from;

/**
 * @author: Anthony Dahanne
 */
public abstract class ResourceServiceImplITHelper {

  static{
    // we must make sure this property is not set ( was set in a pom.xml)
    System.getProperties().remove("tc.config");
  }

  protected static String CLASSNAME;
  private static ClusterManager clusterManager;
  private static TcConfig tcConfig;

  public static int TSA_GROUP_PORT = 22222;
  public static int STANDALONE_REST_AGENT_PORT = 12121;

  protected static final String BASEURI = "http://localhost";
  protected static final String INFO = "/info";
  protected static CacheManager cacheManagerProgrammatic;
  protected static CacheManager cacheManagerXml;

  protected static final String STANDALONE_BASE_URL = BASEURI +":" + STANDALONE_REST_AGENT_PORT;
  protected static final String CLUSTERED_BASE_URL =  BASEURI +":" + TSA_GROUP_PORT;
  public static String CLUSTER_URL = "localhost:" + TSA_GROUP_PORT;


  protected static void setUpCluster(Class clazz) throws Exception {
    tcConfig = new TcConfig()
            .mirrorGroup(
                    new TcMirrorGroup()
                            .server(
                                    new TcServer().tsaGroupPort(TSA_GROUP_PORT)
                            )
            );

    TestBaseUtil.jarFor(ResourceServiceImplITHelper.class);
    tcConfig.fillUpConfig();

    clusterManager = new ClusterManager(clazz,tcConfig);
    clusterManager.start();

    cacheManagerXml = getCacheManagerXml();

  }


  @AfterClass
  public static void tearDownCluster() throws Exception {

    if (cacheManagerXml != null) {
      cacheManagerXml.shutdown();
    }
    clusterManager.stop();
  }


  protected static CacheManager getCacheManagerProgrammatic() {
    Configuration configuration = new Configuration();
    configuration.setName("testCacheManagerProgrammatic");
    configuration.setMaxBytesLocalDisk("10M");
    configuration.setMaxBytesLocalHeap("5M");
    TerracottaClientConfiguration terracottaConfiguration = new TerracottaClientConfiguration().url(CLUSTER_URL);
    configuration.addTerracottaConfig(terracottaConfiguration);
    CacheConfiguration myCache = new CacheConfiguration().eternal(false).name("testCache2").terracotta(new TerracottaConfiguration());
    configuration.addCache(myCache);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:"+STANDALONE_REST_AGENT_PORT);
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);

    CacheManager mgr = new CacheManager(configuration);
    Cache exampleCache = mgr.getCache("testCache2");
    assert (exampleCache != null);
    return mgr;
  }


  protected static CacheManager getCacheManagerXml() {
    CacheManager cacheManager = new CacheManager(ResourceServiceImplITHelper.class.getResource("/management/clustered-ehcache-rest-agent-test.xml"));
    return cacheManager;
  }


  protected ResourceServiceImplITHelper() {
    RestAssured.baseURI = BASEURI;
  }

  public static void main(String[] args) throws Exception {
    ClusterManager clusterManager;
    TcConfig tcConfig;

    tcConfig = new TcConfig()
            .mirrorGroup(
                    new TcMirrorGroup()
                            .server(
                                    new TcServer().tsaGroupPort(TSA_GROUP_PORT)
                            )
            );

    TestBaseUtil.jarFor(ResourceServiceImplITHelper.class);
    File workingDir = new File("./target/" + ResourceServiceImplITHelper.class.getSimpleName());
    tcConfig.fillUpConfig();

    clusterManager = new ClusterManager(ResourceServiceImplITHelper.class, tcConfig);
    clusterManager.start();


    ResourceServiceImplITHelper resourceServiceImplITHelper = new ResourceServiceImplITHelper() {

    };

    CacheManager cacheManagerXml = resourceServiceImplITHelper.getCacheManagerXml();
    CacheManager cacheManagerProgrammatic = resourceServiceImplITHelper.getCacheManagerProgrammatic();
    Cache exampleCache = cacheManagerProgrammatic.getCache("testCache2");
    for (int i=0; i<1000 ; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
    }

  }

  protected String getEhCacheAgentId() {
    // looking up the ehcache agent id, so that we ask for it throguh the tsa
    String agentsReponse = get(CLUSTERED_BASE_URL + "/tc-management-api/agents").asString();
    return from(agentsReponse).get("find{it.agencyOf == 'Ehcache'}.agentId");
  }

}
