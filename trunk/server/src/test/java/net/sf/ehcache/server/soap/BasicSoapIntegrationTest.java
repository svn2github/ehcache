package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.HttpUtil;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.HttpURLConnection;


/**
 * Tests the Soap server.
 * <p/>
 * This test is an integration test which expects the Web Service to be deployed on a server running
 * on port 9090
 *
 * @author Greg Luck
 * @version $Id$
 */
public class BasicSoapIntegrationTest {

    @Test
    public void testEhcacheWebServiceEndPointExists() throws IOException, ParserConfigurationException, SAXException {

        HttpURLConnection response = HttpUtil.get("http://localhost:9090/ehcache/soap/EhcacheWebServiceEndpoint");
        assertEquals(200, response.getResponseCode());
        String responseBody = HttpUtil.inputStreamToText(response.getInputStream());
        assertTrue(responseBody.indexOf("Implementation class:") != 0);
    }

    @Test
    public void testEhcacheWebServiceEndPointWsdlExists() throws IOException, ParserConfigurationException, SAXException {

        HttpURLConnection response = HttpUtil.get("http://localhost:9090/ehcache/soap/EhcacheWebServiceEndpoint?wsdl");
        assertEquals(200, response.getResponseCode());
        String responseBody = HttpUtil.inputStreamToText(response.getInputStream());
        assertTrue(responseBody.indexOf("Implementation class:") != 0);
    }


}