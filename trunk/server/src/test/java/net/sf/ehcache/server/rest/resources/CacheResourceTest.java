/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.server.rest.resources;

import net.sf.ehcache.server.HttpUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Map;
import java.net.HttpURLConnection;


/**
 * Tests the REST web resource using the lightweight http container
 * <p/>
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheResourceTest {

    public static final Logger LOG = Logger.getLogger(CachesResourceTest.class.getName());

    @Test
    public void testGet() throws IOException, ParserConfigurationException, SAXException {
        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/");
        assertEquals(200, result.getResponseCode());
    }


    @Test
    public void testPut() throws IOException, ParserConfigurationException, SAXException {
        HttpUtil.put("http://localhost:8080/ehcache/rest/testCache");
        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/testCache");
        assertEquals(200, result.getResponseCode());
    }


    @Test
    public void testHead() throws IOException, ParserConfigurationException, SAXException {
        HttpUtil.put("http://localhost:8080/ehcache/rest/testCache");
        HttpURLConnection result = HttpUtil.head("http://localhost:8080/ehcache/rest/testCache");
        LOG.info("Result of HEAD: " + result);
        byte[] bytes = HttpUtil.inputStreamToBytes(result.getInputStream());
        assertEquals(0, bytes.length);
        Map headers = result.getHeaderFields();
        assertNotNull(headers);
    }



}