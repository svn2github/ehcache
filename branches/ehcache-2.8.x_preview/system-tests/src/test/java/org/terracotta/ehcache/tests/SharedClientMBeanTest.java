/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.test.util.JMXUtils;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.toolkit.ToolkitFactory;

import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class SharedClientMBeanTest extends AbstractCacheTestBase {

  public SharedClientMBeanTest(TestConfig testConfig) {
    super(testConfig, SharedClientMBeanTestClient.class);

  }

  public static class SharedClientMBeanTestClient extends AbstractClientBase {

    public SharedClientMBeanTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTest() throws Throwable {
      String tcUrl = getTerracottaUrl();

      // first create a toolkit
      ToolkitFactory.createToolkit("toolkit:terracotta://" + tcUrl);

      // create ehcache cacheManager - will be shared client
      Configuration configuration = new Configuration().name("cm").terracotta(new TerracottaClientConfiguration()
                                                                                  .rejoin(false).url(tcUrl));

      configuration.addCache(new CacheConfiguration().name("mbeanCache")
          .terracotta(new TerracottaConfiguration().clustered(true)).maxEntriesLocalHeap(100));
      CacheManager.newInstance(configuration);

      checkEhcacheMBeansRegistered(getGroupData(0).getJmxPort(0), false);

    }

    private void checkEhcacheMBeansRegistered(int jmxPort, boolean afterRejoin) throws Exception {

      MBeanServerConnection mbs = getOrWaitForServerMbeanConnection("localhost", jmxPort);
      ObjectName name = new ObjectName("net.sf.ehcache:*");
      Set<ObjectInstance> queryMBeans = mbs.queryMBeans(name, null);
      System.out.println("=======================================");
      System.out.println("List of objectNames registered in L2");
      for (ObjectInstance oname : queryMBeans) {
        System.out.println(oname.getObjectName().getCanonicalName());
      }
      System.out.println("=======================================");

      Map<String, ObjectName> mbeanNames = new HashMap<String, ObjectName>();
      ObjectName cacheMgrMbeanName = null;

      for (ObjectInstance oi : queryMBeans) {
        ObjectName objectName = oi.getObjectName();
        String type = objectName.getKeyProperty("type");
        if ("SampledCache".equals(type)) {
          if (objectName.getKeyProperty("name").equals("mbeanCache")) {
            mbeanNames.put("exception", objectName);
          }
        }
        if ("SampledCacheManager".equals(type)) {
          cacheMgrMbeanName = objectName;
        }
      }
      Assert.assertNotNull("CacheManager mbean is not tunneled", cacheMgrMbeanName);
      Assert.assertEquals("Some mbeans for cache are missing", 1, mbeanNames.size());
      for (Entry<String, ObjectName> entry : mbeanNames.entrySet()) {
        Assert.assertNotNull("Mbean should be present for cache type : " + entry.getKey(), entry.getValue());
      }
    }

    private MBeanServerConnection getOrWaitForServerMbeanConnection(String host, int port) {
      MBeanServerConnection mbs = null;
      while (mbs == null) {
        try {
          final JMXConnector jmxConnector = JMXUtils.getJMXConnector(host, port);
          mbs = jmxConnector.getMBeanServerConnection();
        } catch (IOException e) {
          System.err.println("XXX getOrWaitForServerMbeanConnection " + host + ":" + port + " - " + e);
        }
        ThreadUtil.reallySleep(3000);
      }
      return mbs;
    }
  }

}
