/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.jaxrs;

import junit.framework.Assert;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.terracotta.management.resource.AgentEntity;

import java.io.IOException;

/**
 * @author brandony
 */
public class EhcacheObjectMapperProviderTest {

  @Test
  public void testObjectMapping() throws IOException {
    EhcacheObjectMapperProvider provider = new EhcacheObjectMapperProvider();
    ObjectMapper om = provider.getContext(AgentEntity.class);
    String output = om.writer().writeValueAsString(new AgentEntity());

    Assert.assertEquals("{\"agentId\":null,\"cacheManagers\":null}", output);
  }
}
