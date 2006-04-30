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
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.constructs.web.AbstractWebTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class SpeedTest extends AbstractWebTest {
    private static final Log LOG = LogFactory.getLog(SpeedTest.class.getName());

    /**
     * Time to get 100 Cached Pages
     * Test time: 5.1s
     * StopWatch time:3.9s
     */
    public void testSpeedHttpClientNotCached() throws IOException {
        StopWatch stopWatch = new StopWatch();
        String url = "http://localhost:8080/Login.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        stopWatch.getElapsedTime();
        for (int i = 0; i < 200; i++) {
            httpMethod.recycle();
            httpClient.executeMethod(httpMethod);
            httpMethod.getResponseBodyAsString();
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for 200 uncached page requests: " + time);
    }

    /**
     * Time to get 100 Cached Pages
     * Test time: 5.3s
     * StopWatch time: 4.1s
     */
    public void testSpeedHttpClientCached() throws IOException {
        StopWatch stopWatch = new StopWatch();
        String url = "http://localhost:8080/CachedLogin.jsp";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        stopWatch.getElapsedTime();
        for (int i = 0; i < 200; i++) {
            httpMethod.recycle();
            httpClient.executeMethod(httpMethod);
            httpMethod.getResponseBodyAsString();
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for 200 cached page requests: " + time);
    }

    /**
     * Test time 14.5s
     * StopWatch time: 11.1s
     */
    public void testSpeedNoDom() throws Exception {
        StopWatch stopWatch = new StopWatch();
        final WebConversation conversation = createWebConversation(true);

        String requestUrl = "http://localhost:8080/CachedLogin.jsp";
        stopWatch.getElapsedTime();
        for (int i = 0; i < 200; i++) {
            WebResponse response = conversation.getResponse(requestUrl);
            response.getText().indexOf("timestamp");
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for 200 cached page requests: " + time);

    }

    /**
     * Test time: 10.2s, 11.5    4.2 without JavaScript library
     * StopWatch time: 7.7s, 8.5    2.7 without JavaScript library
     * This test gets the CacheLogin twice so we need half as many
     */
    public void testSpeedDom() throws Exception {
        StopWatch stopWatch = new StopWatch();
        CachingFilterTest cachingFilterTest = new CachingFilterTest();
        cachingFilterTest.testCachedPageIsCached();
        stopWatch.getElapsedTime();
        for (int i = 0; i < 100; i++) {
            cachingFilterTest.testCachedPageIsCached();
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for 200 cached page requests: " + time);

    }
}
