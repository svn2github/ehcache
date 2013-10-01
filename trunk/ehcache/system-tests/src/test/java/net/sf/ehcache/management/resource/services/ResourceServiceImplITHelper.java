package net.sf.ehcache.management.resource.services;

import com.jayway.restassured.RestAssured;
import com.tc.test.config.builder.ClusterManager;
import com.tc.test.config.builder.TcConfig;
import com.tc.test.config.builder.TcMirrorGroup;
import com.tc.test.config.builder.TcServer;
import com.tc.util.PortChooser;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.*;
import org.junit.AfterClass;
import org.terracotta.test.util.TestBaseUtil;

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

  private static ClusterManager clusterManager;
  private static TcConfig tcConfig;

  public static int TSA_GROUP_PORT = new PortChooser().chooseRandomPort();
  public static int STANDALONE_REST_AGENT_PORT = new PortChooser().chooseRandomPort();

  protected static final String BASEURI = "http://localhost";
  protected static final String INFO = "/info";
  protected static CacheManager cacheManagerMaxBytes;
  protected static CacheManager cacheManagerMaxElements;

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

    cacheManagerMaxElements = getCacheManagerMaxEntries();

  }


  @AfterClass
  public static void tearDownCluster() throws Exception {

    if (cacheManagerMaxElements != null) {
      cacheManagerMaxElements.shutdown();
    }
    clusterManager.stop();
  }


  protected static CacheManager getCacheManagerMaxbytes() {
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


  protected static CacheManager getCacheManagerMaxEntries() {
    Configuration configuration = new Configuration();
    configuration.setName("testCacheManager");
    configuration.addTerracottaConfig(new TerracottaClientConfiguration().url(CLUSTER_URL));
    CacheConfiguration defaultCacheConfiguration = new CacheConfiguration().eternal(true).terracotta(new TerracottaConfiguration()).maxEntriesLocalHeap(10000);
    CacheConfiguration cacheConfiguration = new CacheConfiguration().name("testCache").terracotta(new TerracottaConfiguration()).maxEntriesLocalHeap(10000);
    configuration.setDefaultCacheConfiguration(defaultCacheConfiguration);
    configuration.addCache(cacheConfiguration);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:"+STANDALONE_REST_AGENT_PORT);
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);

    CacheManager mgr = new CacheManager(configuration);
    Cache exampleCache = mgr.getCache("testCache");
    assert (exampleCache != null);
    return mgr;
  }


  protected ResourceServiceImplITHelper() {
    RestAssured.baseURI = BASEURI;
  }


  protected String getEhCacheAgentId() {
    // looking up the ehcache agent id, so that we ask for it throguh the tsa
    String agentsReponse = get(CLUSTERED_BASE_URL + "/tc-management-api/agents").asString();
    return from(agentsReponse).get("find{it.agencyOf == 'Ehcache'}.agentId");
  }

}
