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
import net.sf.ehcache.constructs.web.AbstractWebTest;

import java.net.HttpURLConnection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Test cases for the Caching filter and Gzip.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
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
     * The following response codes cannot have bodies:
     * <ol>
     * <li>100 Continue. Should neve see these in a filter
     * <li>204 No Content.
     * <li>304 Not Modified.
     * </ol>
     * Was throwing a java.io.EOFException when the content is empty.
     */
    public void testZeroLengthHTML() throws Exception {

        String url = "http://localhost:8080/empty_gzip/empty.html";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("If-modified-Since", "Fri, 13 May 3006 23:54:18 GMT");
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, responseCode);
        byte[] responseBody = httpMethod.getResponseBody();
        assertEquals(null, responseBody);
        assertEquals("gzip", httpMethod.getResponseHeader("Content-Encoding").getValue());
        assertEquals("0", httpMethod.getResponseHeader("Content-Length").getValue());
    }

    /**
     * The following response codes cannot have bodies:
     * <ol>
     * <li>100 Continue. Should neve see these in a filter
     * <li>204 No Content.
     * <li>304 Not Modified.
     * </ol>
     * Was throwing a java.io.EOFException when the content is empty.
     */
    public void testNotModifiedJSPGzipFilter() throws Exception {

        String url = "http://localhost:8080/empty_gzip/SC_NOT_MODIFIED.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("If-modified-Since", "Fri, 13 May 3006 23:54:18 GMT");
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, responseCode);
        byte[] responseBody = httpMethod.getResponseBody();
        assertEquals(null, responseBody);
        assertEquals("gzip", httpMethod.getResponseHeader("Content-Encoding").getValue());
        assertNotNull(httpMethod.getResponseHeader("Last-Modified").getValue());
        assertEquals("0", httpMethod.getResponseHeader("Content-Length").getValue());
    }

    /**
     * Orion can send content even when the response is set to no content for a JSP.
     *
     */
    public void testNoContentJSPGzipFilter() throws Exception {

        String url = "http://localhost:8080/empty_gzip/SC_NO_CONTENT.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("If-modified-Since", "Fri, 13 May 3006 23:54:18 GMT");
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, responseCode);
        byte[] responseBody = httpMethod.getResponseBody();
        assertEquals(null, responseBody);
        assertEquals("gzip", httpMethod.getResponseHeader("Content-Encoding").getValue());
        assertNotNull(httpMethod.getResponseHeader("Last-Modified").getValue());
        //Not 0.Sends body as well.
        assertEquals("70", httpMethod.getResponseHeader("Content-Length").getValue());
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

