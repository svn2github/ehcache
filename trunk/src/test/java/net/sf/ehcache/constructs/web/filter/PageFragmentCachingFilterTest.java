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

import com.meterware.httpunit.WebResponse;
import net.sf.ehcache.constructs.web.AbstractWebTest;
import net.sf.ehcache.constructs.web.PageInfo;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class PageFragmentCachingFilterTest extends AbstractWebTest {

    /**
     * Tests that we can get the page successfully
     */
    public void testBasic() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/include/Footer.jsp");
        assertResponseOk(response);
        assertIncludeHeadersSane(response);
        assertPageNotBlank(response);
    }

    /**
     * Tests that a page which is in the cache fragment filter pattern is cached.
     */
    public void testCachedPageFragmentIsCached() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/fragment/CachedFragment.jsp");
        assertResponseOk(response);
        assertIncludeHeadersSane(response);
        assertPageNotBlank(response);
        String firstResponse = response.getText();

        response = getResponseFromAcceptGzipRequest("/fragment/CachedFragment.jsp");
        assertResponseOk(response);
        assertIncludeHeadersSane(response);
        assertPageNotBlank(response);
        String secondResponse = response.getText();

        assertEquals(firstResponse, secondResponse);
    }

    /**
     * Tests that a page which is not storeGzipped is not gzipped when the user agent accepts gzip encoding
     */
    public void testNotGzippedWhenAcceptEncodingPageFragment() throws Exception {
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(buildUrl("/include/Footer.jsp"));
        //httpMethod.setRequestHeader(new Header("Accept-encoding", "gzip"));
        httpMethod.setStrictMode(true);
        httpClient.executeMethod(httpMethod);
        byte[] responseBody = httpMethod.getResponseBody();
        assertFalse(PageInfo.isGzipped(responseBody));
        assertNotSame("gzip", httpMethod.getResponseHeader("Accept-encoding"));
    }

    /**
     * Tests that a page which is not storeGzipped is not gzipped when the user agent does not accept gzip encoding
     */
    public void testNotGzippedWhenNotAcceptEncodingPageFragment() throws Exception {
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(buildUrl("/include/Footer.jsp"));
        httpMethod.setRequestHeader(new Header("Accept-encoding", "gzip"));
        httpMethod.setStrictMode(true);
        httpClient.executeMethod(httpMethod);
        byte[] responseBody = httpMethod.getResponseBody();
        assertFalse(PageInfo.isGzipped(responseBody));
        assertNotSame("gzip", httpMethod.getResponseHeader("Accept-encoding"));
    }

    /**
     * Checks that a cached fragment contained in a non-cached out page works.
     *
     * @throws Exception
     */
    public void testFromJSPInclude() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/fragment/NonCachedPageIncludingCachedFragment.jsp");
        assertResponseOk(response);
        assertHeadersSane(response);
        assertPageNotBlank(response);
        assertTrue(response.getText().indexOf("</html>") != -1);

    }


}
