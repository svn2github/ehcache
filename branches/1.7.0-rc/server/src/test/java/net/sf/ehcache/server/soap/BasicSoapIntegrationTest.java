package net.sf.ehcache.server.soap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.xml.sax.SAXException;

import javax.xml.ws.Endpoint;
import javax.xml.parsers.ParserConfigurationException;
import java.net.HttpURLConnection;
import java.io.IOException;

import net.sf.ehcache.server.HttpUtil;


/**
 * Tests the Soap server.
 * <p/>
 * This test is an integration test which expects the Web Service to be deployed on a server running
 * on port 8080
 *
 * @author Greg Luck
 * @version $Id$
 */
public class BasicSoapIntegrationTest {

    @Test
    public void testEhcacheWebServiceEndPointExists() throws IOException, ParserConfigurationException, SAXException {

        HttpURLConnection response = HttpUtil.get("http://localhost:8080/ehcache/soap/EhcacheWebServiceEndpoint");
        assertEquals(200, response.getResponseCode());
        String responseBody = HttpUtil.inputStreamToText(response.getInputStream());
        assertTrue(responseBody.indexOf("Implementation class:") != 0);
    }

    @Test
    public void testEhcacheWebServiceEndPointWsdlExists() throws IOException, ParserConfigurationException, SAXException {

        HttpURLConnection response = HttpUtil.get("http://localhost:8080/ehcache/soap/EhcacheWebServiceEndpoint?wsdl");
        assertEquals(200, response.getResponseCode());
        String responseBody = HttpUtil.inputStreamToText(response.getInputStream());
        assertTrue(responseBody.indexOf("Implementation class:") != 0);
    }


}