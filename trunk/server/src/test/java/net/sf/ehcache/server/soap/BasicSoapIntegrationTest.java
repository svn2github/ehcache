/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.util.HttpUtil;
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
public class BasicSoapIntegrationTest extends AbstractSoapTest {

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