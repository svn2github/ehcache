/**
 *  Copyright 2003-2009 Luck Consulting Pty Ltd
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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.net.HttpURLConnection;

/**
 * Test cases for the Caching filter and Gzip.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: GzipFilterTest.java 796 2008-10-09 02:39:03Z gregluck $
 */
public class GzipFilterTest extends AbstractWebTest {

    /**
     * Fetch NoFiltersPage.jsp, which is excluded from all filters and check it is not gzipped.
     */
    @Test
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
    @Test
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

        //Check that we are dealing with Cyrillic characters ok
        assertTrue(response.getText().indexOf("&#8593;") != -1);
        //Check non ascii symbol
        assertTrue(response.getText().indexOf("&#1052;") != -1);
    }

    /**
     * A 0 length body should give a 0 length gzip body and content length
     * <p/>
     * Manual test: wget -d --server-response --timestamping --header='If-modified-Since: Fri, 13 May 3006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/empty_gzip/empty.html
     */
    @Test
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
        assertNull(httpMethod.getResponseHeader("Content-Encoding"));
        checkNullOrZeroContentLength(httpMethod);
    }


    /**
     * JSPs and Servlets can send bodies when the response is SC_NOT_MODIFIED.
     * In this case there should not be a body but there is. Orion seems to kill the body
     * after is has left the Servlet filter chain. To avoid wget going into an inifinite
     * retry loop, and presumably some other web clients, the content length should be 0
     * and the body 0.
     * <p/>
     * Manual test: wget -d --server-response --header='If-modified-Since: Fri, 13 May 3006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/empty_gzip/SC_NOT_MODIFIED.jsp
     */
    @Test
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
        assertNull(httpMethod.getResponseHeader("Content-Encoding"));
        assertNotNull(httpMethod.getResponseHeader("Last-Modified").getValue());
        checkNullOrZeroContentLength(httpMethod);
    }

    /**
     * JSPs and Servlets can send bodies when the response is SC_NOT_MODIFIED.
     * In this case there should not be a body but there is. Orion seems to kill the body
     * after is has left the Servlet filter chain. To avoid wget going into an inifinite
     * retry loop, and presumably some other web clients, the content length should be 0
     * and the body 0.
     * <p/>
     * Manual test: wget -d --server-response --timestamping --header='If-modified-Since: Fri, 13 May 3006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/empty_gzip/SC_NO_CONTENT.jsp
     */
    @Test
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
        assertNull(httpMethod.getResponseHeader("Content-Encoding"));
        assertNotNull(httpMethod.getResponseHeader("Last-Modified").getValue());
        checkNullOrZeroContentLength(httpMethod);
    }

    /**
     * Tests that a page which is storeGzipped is not gzipped when the user agent does not accept gzip encoding
     */
    @Test
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
    @Test
    public void testNonGzipThenGzipBrowserHomePage() throws Exception {
        testNotGzippedWhenNotAcceptEncodingHomePage();
        testGzippedWhenAcceptEncodingHomePage();
    }

    /**
     * When the servlet container generates a 404 page not found, we want to pass
     * it through without caching and without adding anything to it.
     * <p/>
     * Manual Test: wget -d --server-response --header='Accept-Encoding: gzip'  http://localhost:8080/non_ok/PageNotFoundGzip.jsp
     */
    @Test
    public void testNotFound() throws Exception {

        String url = "http://localhost:8080/non_ok/PageNotFoundGzip.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("If-modified-Since", "Fri, 13 May 3006 23:54:18 GMT");
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseCode);
        String responseBody = httpMethod.getResponseBodyAsString();
        assertNotNull(responseBody);
        assertNull(httpMethod.getResponseHeader("Content-Encoding"));
    }

    /**
     * When the servlet container generates a 404 page not found, we want to pass
     * it through without caching and without adding anything to it.
     * <p/>
     * Manual Test: wget -d --server-response --header='Accept-Encoding: gzip'  http://localhost:8080/non_ok/SendRedirectGzip.jsp
     */
    @Test
    public void testRedirect() throws Exception {

        String url = "http://localhost:8080/non_ok/SendRedirectGzip.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        //httpclient follows redirects, so gets the home page.
        assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        String responseBody = httpMethod.getResponseBodyAsString();
        assertNotNull(responseBody);
        assertNull(httpMethod.getResponseHeader("Content-Encoding"));
    }

    /**
     * When the servlet container forwards to a page does it work?
     * <p/>
     * Manual Test: wget -d --server-response --header='Accept-Encoding: gzip'  http://localhost:8080/non_ok/ForwardFromGzip.jsp
     */
    @Test
    public void testForward() throws Exception {

        String url = "http://localhost:8080/non_ok/ForwardFromGzip.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        //httpclient follows redirects, so gets the home page.
        assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        String responseBody = httpMethod.getResponseBodyAsString();
        assertNotNull(responseBody);
        assertEquals("gzip", httpMethod.getResponseHeader("Content-Encoding").getValue());
    }

}

