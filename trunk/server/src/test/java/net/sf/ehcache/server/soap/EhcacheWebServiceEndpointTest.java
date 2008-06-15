/**
 * Tests for the Ehcache WebService
 * @author Greg Luck
 * @version $Id$
 */
package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.soap.jaxws.*;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import org.junit.Test;
import org.junit.Assert;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.util.List;

public class EhcacheWebServiceEndpointTest {
    private static EhcacheWebServiceEndpoint endpoint;

    @BeforeClass
  public static void setup() {
        endpoint = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();
  }

  @Test
  public void testPing() {
      //invoke business method
      String result = endpoint.ping();
      assertEquals("pong", result);
  }

  @Test
  public void testCacheNames() throws IllegalStateException_Exception {
      //invoke business method
      List cacheNames = endpoint.cacheNames();
      //Other tests add caches to the CacheManager
      assertTrue(cacheNames.size() >= 6);
  }

    

}