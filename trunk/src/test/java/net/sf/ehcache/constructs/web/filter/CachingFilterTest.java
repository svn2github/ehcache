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

import com.meterware.httpunit.HttpInternalErrorException;
import com.meterware.httpunit.WebResponse;
import junit.framework.AssertionFailedError;
import net.sf.ehcache.constructs.web.AbstractWebTest;
import net.sf.ehcache.constructs.web.PageInfo;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class CachingFilterTest extends AbstractWebTest {

    private String cachedPageUrl = "/CachedLogin.jsp";

    private void resetCachedPageUrl() {
        cachedPageUrl = "/CachedLogin.jsp";
    }


    /**
     * Tests that a page which should be cached is cached.
     * Also, check that the pages returned are good.
     */
    public void testCachedPageIsCached() throws Exception {
        assertResponseGoodAndCached(cachedPageUrl, true);
    }

    /**
     * Tests whether the page is gzipped using the rawer HttpClient library.
     * Lets us check that the responseBody is really gzipped.
     */
    public void testCachedPageIsGzippedWhenEncodingHeaderSet() throws IOException {
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(buildUrl(cachedPageUrl));
        httpMethod.setRequestHeader(new Header("Accept-encoding", "gzip"));
        httpClient.executeMethod(httpMethod);
        byte[] responseBody = httpMethod.getResponseBody();
        assertTrue(PageInfo.isGzipped(responseBody));
    }

    /**
     * Tests whether the page is gzipped using the rawer HttpClient library.
     * Lets us check that the responseBody is really not gzipped.
     */
    public void testCachedPageIsNotGzippedWhenEncodingHeaderNotSet() throws IOException {
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(buildUrl(cachedPageUrl));
        //httpMethod.setRequestHeader(new Header("Accept-encoding", "gzip"));
        httpClient.executeMethod(httpMethod);
        byte[] responseBody = httpMethod.getResponseBody();
        assertFalse(PageInfo.isGzipped(responseBody));
    }

    /**
     * Checks sequence
     *
     * @throws IOException
     */
    public void testSequenceNonGzipThenGzipThenNonGzip() throws IOException {
        testCachedPageIsNotGzippedWhenEncodingHeaderNotSet();
        testCachedPageIsGzippedWhenEncodingHeaderSet();
        testCachedPageIsNotGzippedWhenEncodingHeaderNotSet();
    }

    /**
     * Checks sequence
     *
     * @throws IOException
     */
    public void testSequenceGzipThenNonGzipThenGzip() throws IOException {
        testCachedPageIsGzippedWhenEncodingHeaderSet();
        testCachedPageIsNotGzippedWhenEncodingHeaderNotSet();
        testCachedPageIsGzippedWhenEncodingHeaderSet();
    }


    /**
     * The reentry check instruments the thread name to indicate it has entered a CachingFilter.
     * The name is reset at the end. Check that this works for concurrent situations.
     */
    public void testCachedPageConcurrent() throws Exception {

        final List executables = new ArrayList();
        for (int i = 0; i < 40; i++) {
            final AbstractWebTest.Executable executable = new AbstractWebTest.Executable() {
                public void execute() throws Exception {
                    testCachedPageIsCached();
                }
            };
            executables.add(executable);
        }
        runThreads(executables);
    }


    /**
     * Tests that a cached full page can be included in another page which calls it with
     * a jsp:include
     * <p/>
     * Fails when client request gzip
     * wget -S  --header='Accept-encoding: gzip' http://localhost:8080/legaldispatchtocachedpage/Include.jsp
     * --15:01:49--  http://localhost:8080/legaldispatchtocachedpage/Include.jsp
     * => `Include.jsp.9'
     * Resolving localhost... done.
     * Connecting to localhost[127.0.0.1]:8080... connected.
     * HTTP request sent, awaiting response...
     * 1 HTTP/1.1 200 OK
     * 2 Date: Mon, 22 Nov 2004 05:01:48 GMT
     * 3 Server: Orion/2.0.3
     * 4 Set-Cookie: JSESSIONID=EFEIDKLICCGB; Path=/
     * 5 Cache-Control: private
     * 6 Connection: Close
     * 7 Content-Type: text/html
     * <p/>
     * [ <=>                                                                                                                                             ] 734          716.80K/s
     * <p/>
     * 15:01:49 (716.80 KB/s) - `Include.jsp.9' saved [734]
     *
     * @throws Exception
     */
    public void testFromJSPInclude() throws Exception {
        try {
            WebResponse response = getResponseFromAcceptGzipRequest("/legaldispatchtocachedpage/Include.jsp");
            assertPropertlyFormed(response);
            fail();
        } catch (AssertionFailedError e) {
            //noop Page is actually an error message in Orion
        } catch (HttpInternalErrorException e) {
            //noop 500 error in Tomcat. The log shows ResponseHeadersNotModifiableException when we try to set Gzip
            //because it is already gzipped.
        }

        //No point doing the rest of the tests because the page is always blank
    }

    /**
     * Tests that a cached full page can be included in another page which calls it with
     * a jsp:include, where the client does not accept gzip encoding.
     *
     * @throws Exception
     */
    public void testFromJSPIncludeNoGzip() throws Exception {
        try {
            WebResponse response = getResponseFromNonAcceptGzipRequest("/legaldispatchtocachedpage/Include.jsp");
            assertPropertlyFormed(response);
            fail();
        } catch (AssertionFailedError e) {
            //noop Page is actually an error message
        } catch (HttpInternalErrorException e) {
            //noop 500 error in Tomcat. The log shows ResponseHeadersNotModifiableException when we try to set Gzip
            //because it is already gzipped.
        }

        //No point doing the rest of the tests because the page is always blank
    }

    /**
     * Tests that a cached full page included using the RequestDispatcher in a servlet
     * throws a Server 500 error. In the server log a ResponseHeadersNotModifiableException
     * with the message "Failure when attempting to set Content-Encoding: gzip" will be thrown.
     * <p/>
     * If we did not throw an error the client would get a strange result:
     * <pre>
     * wget -S  --header='Accept-encoding: gzip' http://localhost:8080/servletdispatchtocachedpage/IncludeCachedPageServlet
     * --20:43:29--  http://localhost:8080/servletdispatchtocachedpage/IncludeCachedPageServlet
     * => `IncludeCachedPageServlet.2'
     * Resolving localhost... done.
     * Connecting to localhost[127.0.0.1]:8080... connected.
     * HTTP request sent, awaiting response...
     * 1 HTTP/1.1 200 OK
     * 2 Date: Mon, 22 Nov 2004 10:43:28 GMT
     * 3 Server: Orion/2.0.3
     * 4 Connection: Close
     * 5 Content-Type: text/plain
     * </pre>
     * Though gzip, the header is not set. The content-type is text/plain, because in our case the
     * test servlet did not set a type.
     *
     * @see {@link net.sf.ehcache.constructs.web.ResponseHeadersNotModifiableException}
     */
    public void testFromServletInclude() {
        String url = "/servletdispatchtocachedpage/IncludeCachedPageServlet";
        try {
            getResponseFromAcceptGzipRequest(url);
            fail();
        } catch (HttpInternalErrorException e) {
            //this is what should happen.
        } catch (IOException e) {
            fail();
        } catch (SAXException e) {
            fail();
        }

    }

    /**
     * Tests that a cached full page can be forwarded to using the RequestDispatcher in a servlet.
     *
     * @throws Exception
     */
    public void testFromServletForward() throws Exception {
        String url = "/servletdispatchtocachedpage/ForwardToCachedPageServlet";
        WebResponse response = getResponseFromAcceptGzipRequest(url);
        assertResponseOk(response);
        assertHeadersSane(response);
        assertPageNotBlank(response);
        assertPropertlyFormed(response);

        //runStandardTestsOnUrl(url);
    }

    /**
     * This sequence returns a valid page with 0 bytes!
     * This will be seen as an empty page in the browser.
     * wget response looks like:
     * wget -S  --header='Accept-encoding: gzip' http://localhost:8080//blankpageproblem/Include.jsp
     * --10:13:00--  http://localhost:8080/legaldispatchtocachedpage/Include.jsp
     * => `Include.jsp.2'
     * Resolving localhost... done.
     * Connecting to localhost[127.0.0.1]:8080... connected.
     * HTTP request sent, awaiting response...
     * 1 HTTP/1.1 200 OK
     * 2 Date: Mon, 22 Nov 2004 00:13:00 GMT
     * 3 Server: Orion/2.0.3
     * 4 Set-Cookie: JSESSIONID=JAPIBBCHOFKO; Path=/
     * 5 Cache-Control: private
     * 6 Connection: Close
     * 7 Content-Type: text/html
     * <p/>
     * [ <=>                                                                                                                                             ] 0             --.--K/s
     * <p/>
     * 10:13:03 (0.00 B/s) - `Include.jsp.2' saved [0]
     * <p/>
     * The problem is a filter inplementation which does not write the response wrapper content
     * when it detects a response already committed so nothing is writen by the filter.
     *
     * @throws Exception
     */
    public void testBlankPageProblemFilterFromJSPInclude() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/blankpageproblem/Include.jsp");
        assertResponseOk(response);
        assertHeadersSane(response);
        try {
            assertPageNotBlank(response);
            fail();
        } catch (AssertionFailedError e) {
            //noop Page is blank. Should fail.
        }

        //No point doing the rest of the tests because the page is always blank
    }


    /**
     * Tests including a page cached with SimplePageCachingFilter, after setting Filter.noFilter(true)
     * This works. It does not produce a blank page.
     * wget response looks like:
     * wget -S  --header='Accept-encoding: gzip' http://localhost:8080/legaldispatchtocachedpage/IncludeWithNoFilter.jsp
     * --13:19:12--  http://localhost:8080/legaldispatchtocachedpage/IncludeWithNoFilter.jsp
     * => `IncludeWithNoFilter.jsp'
     * Resolving localhost... done.
     * Connecting to localhost[127.0.0.1]:8080... connected.
     * HTTP request sent, awaiting response...
     * 1 HTTP/1.1 200 OK
     * 2 Date: Mon, 22 Nov 2004 03:19:12 GMT
     * 3 Server: Orion/2.0.3
     * 4 Set-Cookie: JSESSIONID=LCIFMEDBEJKP; Path=/
     * 5 Cache-Control: private
     * 6 Connection: Close
     * 7 Content-Type: text/html
     * <p/>
     * [ <=>                                                                                                                                             ] 1,846          1.76M/s
     * <p/>
     * 13:19:12 (1.76 MB/s) - `IncludeWithNoFilter.jsp' saved [1846]
     * The problem is that the response is already committed so nothing is writen by the filter.
     *
     * @throws Exception
     */
    public void testFromJSPIncludeWithNoFilter() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/legaldispatchtocachedpage/IncludeWithNoFilter.jsp");
        assertResponseOk(response);
        assertHeadersSane(response);
        assertPageNotBlank(response);

        assertResponseGoodAndNotCached("/legaldispatchtocachedpage/IncludeWithNoFilter.jsp", true);

        //No point doing the rest of the tests because the page is not cached
    }

    /**
     * This test calls a page which forwards to the cached page using jsp:forward.
     * <p/>
     * This works fine.
     * <p/>
     * wget response looks like:
     * wget -S  --header='Accept-encoding: gzip' http://localhost:8080/legaldispatchtocachedpage/Forward.jsp
     * --10:17:43--  http://localhost:8080/legaldispatchtocachedpage/Forward.jsp
     * => `Forward.jsp'
     * Resolving localhost... done.
     * Connecting to localhost[127.0.0.1]:8080... connected.
     * HTTP request sent, awaiting response...
     * 1 HTTP/1.1 200 OK
     * 2 Date: Mon, 22 Nov 2004 00:17:56 GMT
     * 3 Server: Orion/2.0.3
     * 4 Content-Location: http://localhost:8080/CachedLogin.jsp
     * 5 Content-Length: 735
     * 6 Set-Cookie: JSESSIONID=ICDJCBCHOFKO; Path=/
     * 7 Cache-Control: private
     * 8 Connection: Close
     * 9 Content-Type: text/html
     * 10 Content-Encoding: gzip
     * <p/>
     * 10:17:56 (717.77 KB/s) - `Forward.jsp' saved [735/735]
     *
     * @throws Exception
     */
    public void testFromJSPForward() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/legaldispatchtocachedpage/Forward.jsp");
        assertResponseGood(response, true);
        runStandardTestsOnUrl("/legaldispatchtocachedpage/Forward.jsp");
    }

    /**
     * This test calls a page which forwards to the cached page using jsp:forward.
     * <p/>
     * This works fine.
     * <p/>
     * wget response looks like:
     * wget -S  --header='Accept-encoding: gzip' http://localhost:8080/legaldispatchtocachedpage/Forward.jsp
     * --10:17:43--  http://localhost:8080/legaldispatchtocachedpage/Forward.jsp
     * => `Forward.jsp'
     * Resolving localhost... done.
     * Connecting to localhost[127.0.0.1]:8080... connected.
     * HTTP request sent, awaiting response...
     * 1 HTTP/1.1 200 OK
     * 2 Date: Mon, 22 Nov 2004 00:17:56 GMT
     * 3 Server: Orion/2.0.3
     * 4 Content-Location: http://localhost:8080/CachedLogin.jsp
     * 5 Content-Length: 735
     * 6 Set-Cookie: JSESSIONID=ICDJCBCHOFKO; Path=/
     * 7 Cache-Control: private
     * 8 Connection: Close
     * 9 Content-Type: text/html
     * 10 Content-Encoding: gzip
     * <p/>
     * 10:17:56 (717.77 KB/s) - `Forward.jsp' saved [735/735]
     *
     * @throws Exception
     */
    public void testCallCachedPageFromRedirect() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/legaldispatchtocachedpage/Redirect.jsp");
        assertResponseGood(response, true);

        runStandardTestsOnUrl("/legaldispatchtocachedpage/Redirect.jsp");
    }


    /**
     * @throws Exception Always reset the URL so we don't get dependencies
     */
    protected void tearDown() throws Exception {
        resetCachedPageUrl();
    }

    /**
     * Some standard tests that should work.
     *
     * @param url
     * @throws Exception
     */
    private void runStandardTestsOnUrl(String url) throws Exception {
        cachedPageUrl = url;
        testCachedPageIsCached();

        cachedPageUrl = url;
        testCachedPageIsGzippedWhenEncodingHeaderSet();

        cachedPageUrl = url;
        testCachedPageIsNotGzippedWhenEncodingHeaderNotSet();

        cachedPageUrl = url;
        testSequenceGzipThenNonGzipThenGzip();

        cachedPageUrl = url;
        testSequenceNonGzipThenGzipThenNonGzip();

        cachedPageUrl = url;
        testCachedPageConcurrent();
    }


    /**
     * A 0 length body should give a 0 length nongzipped body and content length
     * Manual Test: wget -d --server-response --timestamping --header='If-modified-Since: Fri, 13 May 3006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/empty_caching_filter/empty.html
     */
    public void testIfModifiedZeroLengthHTML() throws Exception {

        String url = "http://localhost:8080/empty_caching_filter/empty.html";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("If-modified-Since", "Fri, 13 May 3006 23:54:18 GMT");
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        assertTrue(HttpURLConnection.HTTP_OK == responseCode || HttpURLConnection.HTTP_NOT_MODIFIED == responseCode);
        String responseBody = httpMethod.getResponseBodyAsString();
        assertTrue("".equals(responseBody) || null == responseBody);
        checkNullOrZeroContentLength(httpMethod);
    }

    /**
     * Servlets and JSPs can send content even when the response is set to no content.
     * In this case there should not be a body but there is. Orion seems to kill the body
     * after is has left the Servlet filter chain. To avoid wget going into an inifinite
     * retry loop, and presumably some other web clients, the content length should be 0
     * and the body 0.
     * <p/>
     * wget -d --server-response --timestamping --header='If-modified-Since: Fri, 13 May 3006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/empty_caching_filter/SC_NOT_MODIFIED.jsp
     */
    public void testNotModifiedJSPGzipFilter() throws Exception {

        String url = "http://localhost:8080/empty_caching_filter/SC_NOT_MODIFIED.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("If-modified-Since", "Fri, 13 May 3006 23:54:18 GMT");
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, responseCode);
        String responseBody = httpMethod.getResponseBodyAsString();
        assertEquals(null, responseBody);
        assertNull(httpMethod.getResponseHeader("Content-Encoding"));
        assertNotNull(httpMethod.getResponseHeader("Last-Modified").getValue());
        checkNullOrZeroContentLength(httpMethod);

    }

    /**
     * Servlets and JSPs can send content even when the response is set to no content.
     * In this case there should not be a body but there is. Orion seems to kill the body
     * after is has left the Servlet filter chain. To avoid wget going into an inifinite
     * retry loop, and presumably some other web clients, the content length should be 0
     * and the body 0.
     * <p/>
     * Manual Test: wget -d --server-response --timestamping --header='If-modified-Since: Fri, 13 May 3006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/empty_caching_filter/SC_NO_CONTENT.jsp
     */
    public void testNoContentJSPGzipFilter() throws Exception {

        String url = "http://localhost:8080/empty_caching_filter/SC_NO_CONTENT.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("If-modified-Since", "Fri, 13 May 3006 23:54:18 GMT");
        httpMethod.addRequestHeader("Accept-Encoding", "gzip");
        int responseCode = httpClient.executeMethod(httpMethod);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, responseCode);
        String responseBody = httpMethod.getResponseBodyAsString();
        assertEquals(null, responseBody);
        assertNull(httpMethod.getResponseHeader("Content-Encoding"));
        assertNotNull(httpMethod.getResponseHeader("Last-Modified").getValue());
        checkNullOrZeroContentLength(httpMethod);

    }

    /**
     * When the servlet container generates a 404 page not found, we want to pass
     * it through without caching and without adding anything to it.
     * <p/>
     * Manual Test: wget -d --server-response --header='Accept-Encoding: gzip'  http://localhost:8080/non_ok/PageNotFound.jsp
     */
    public void test404() throws Exception {

        String url = "http://localhost:8080/non_ok/PageNotFound.jsp";
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
     * Manual Test: wget -d --server-response --header='Accept-Encoding: gzip'  http://localhost:8080/non_ok/SendRedirect.jsp
     */
    public void test302() throws Exception {

        String url = "http://localhost:8080/non_ok/SendRedirect.jsp";
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


}
