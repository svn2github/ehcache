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
import net.sf.ehcache.server.TestServer;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.File;
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
    private static TestServer server;

    @Test
    public void testGet() throws IOException, ParserConfigurationException, SAXException {
        Object result = HttpUtil.get("http://localhost:9998/ehcache");
        LOG.info("Result of Get: " + result);
    }


    @Test
    public void testPut() throws IOException, ParserConfigurationException, SAXException {
        HttpUtil.put("http://localhost:9998/ehcache/testCache");
        Object result = HttpUtil.get("http://localhost:9998/ehcache");
        LOG.info("Result of Get: " + result);
    }


    @Test
    public void testHead() throws IOException, ParserConfigurationException, SAXException {
        HttpUtil.put("http://localhost:9998/ehcache/testCache");
        HttpURLConnection result = HttpUtil.head("http://localhost:9998/ehcache/testCache");
        LOG.info("Result of HEAD: " + result);
        byte[] bytes = HttpUtil.inputStreamToBytes(result.getInputStream());
        assertEquals(0, bytes.length);
        Map headers = result.getHeaderFields();
        assertNotNull(headers);
    }


    @BeforeClass
    public static void setupBeforeAll() throws InterruptedException {
        LOG.info("Starting Server");
        File war = new File("/Users/gluck/work/ehcache/server/target/ehcache-server-1.5.0-beta1.war");
        server = new TestServer(9998, null);
        server.init();
        Thread.sleep(3000);
    }

    @AfterClass
    public static void teardownAfterAll() {
        LOG.info("Stopping Server");
        server.destroy();
    }


}