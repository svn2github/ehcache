/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.constructs.web.filter;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import java.net.HttpURLConnection;
import net.sf.ehcache.constructs.web.AbstractWebTest;

/**
 * Test cases for the Caching filter and Gzip.
 *
 * @version $Id$
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 */
public class GzipTest extends AbstractWebTest {

    /**
     * Fetch NoFiltersPage.jsp, which is excluded from all filters and check it is not gzipped.
     */
    public void testNegativeGzip() throws Exception {
        WebConversation client = createWebConversation(true);
        client.getClientProperties().setAcceptGzip(true);
        String url = buildUrl("/NoFiltersPage.jsp");
        WebResponse response = client.getResponse(url);

        assertNotNull(response);
        assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

        String responseURL = response.getURL().toString();
        assertEquals(url, responseURL);
        assertNotSame("gzip", response.getHeaderField("Content-Encoding"));
    }




    /**
     * Tests that a page which is storeGzipped is gzipped when the user agent accepts gzip encoding
     */
    public void testGzippedWhenAcceptEncodingHomePage() throws Exception {
        WebConversation client = createWebConversation(true);
        client.getClientProperties().setAcceptGzip(true);
        String url = buildUrl("/GzipOnlyPage.jsp");
        WebResponse response = client.getResponse(url);

        assertNotNull(response);
        assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

        String responseURL = response.getURL().toString();
        assertEquals(url, responseURL);
        assertEquals("gzip", response.getHeaderField("Content-Encoding"));
    }


    /**
     * Tests that a page which is storeGzipped is gzipped when the user agent does not accept gzip encoding
     */
    public void testNotGzippedWhenNotAcceptEncodingHomePage() throws Exception {
        WebConversation client = createWebConversation(false);
        String url = buildUrl("/GzipOnlyPage.jsp");
        WebResponse response = client.getResponse(url);

        assertNotNull(response);
        assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

        String responseURL = response.getURL().toString();
        assertEquals(url, responseURL);
        assertFalse("gzip".equals(response.getHeaderField("Content-Encoding")));
    }

    /**
     * Tests that hitting a page with a non gzip browser then a gzip browser causes the
     * right behaviours.
     */
    public void testNonGzipThenGzipBrowserHomePage() throws Exception {
        testNotGzippedWhenNotAcceptEncodingHomePage();
        testGzippedWhenAcceptEncodingHomePage();
    }


   
}

