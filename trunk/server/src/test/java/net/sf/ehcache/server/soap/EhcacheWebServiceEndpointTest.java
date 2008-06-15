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

public class EhcacheWebServiceEndpointTest {

  @Test
  public void testPing() {

      EhcacheWebServiceEndpoint endpoint = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();

      //invoke business method
      String result = endpoint.ping();
      Assert.assertEquals("pong", result);


  }
}