/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.util.Assert;

public class StandaloneEhcacheRestAgentTest {
  private CacheManager manager;

  @Before
  public void setUp() {
    manager = new CacheManager(
                               StandaloneEhcacheRestAgentTest.class
                                   .getResource("/standalone-ehcache-rest-agent-test.xml"));
  }

  @After
  public void tearDown() {
    if (manager != null) {
      manager.shutdown();
    }
  }

  @Test
  public void testRestAgent() throws Exception {
    Cache testCache = manager.getCache("testCache");
    testCache.put(new Element("k", "v"));
    Assert.assertEquals(1, testCache.getSize());
    
    WebConversation wc = new WebConversation();
    WebResponse response = wc.getResponse("http://localhost:9888/tc-management-api/agents");
    System.out.println(response.getText());
    assertThat(response.getText(), containsString("\"agentId\":\"embedded\""));
  }
}
