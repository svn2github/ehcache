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
import net.sf.ehcache.constructs.web.AbstractWebTest;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class CachingFilterReentranceTest extends AbstractWebTest {

    /**
     * The {@link CachingFilter} uses the {@link net.sf.ehcache.constructs.blocking.BlockingCache}. It blocks until the thread which
     * did a get which results in a null does a put. {@link CachingFilter} is therefore
     * not reentrant.
     * <p/>
     * Reentrant filters could result in a request never completing, where the request
     * thread waits for monitor entry for ever. The {@link CachingFilter} includes a check for
     * reentrancy. If detected it will throw an Exception.
     * <p/>
     * This test uses a deliberately reentrant filter chain to:
     * <ol>
     * <li>Demonstrate how the problem occurs
     * <li>Check that an Exception is thrown rather than blocking forever.
     * </ol>
     * todo tomcat is not reentering the filter chain for the include. This test is not hitting fail() because tomcat is not caching the include.
     */
    public void testReentranceOfCachingFilterThrowsException() throws IOException, SAXException {
        try {
            WebResponse response = getResponseFromAcceptGzipRequest(
                    "/reentrant/MainPageAndIncludeBothGoThroughCachingFilter.jsp");
            String server = response.getHeaderField("SERVER");
            if (server.equals("Apache-Coyote/1.1")) {
                //this is broken in tomcat.
                return;
            }
            fail();
        } catch (HttpInternalErrorException e) {
            //noop
        }
    }

    /**
     * Checks that reentrance via a forward is still caught.
     *
     * @see #testReentranceOfCachingFilterThrowsException()
     * todo tomcat is not reentering the filter chain for the include. This test is not hitting fail() because tomcat is not caching the include.
     */
    public void testReentranceOfCachingFilterViaForwardThrowsException() throws IOException, SAXException {
        try {
            WebResponse response =
                    getResponseFromAcceptGzipRequest("/reentrant/MainPageAndForwardBothGoThroughCachingFilter.jsp");
            String server = response.getHeaderField("SERVER");
            if (server.equals("Apache-Coyote/1.1")) {
                //this is broken in tomcat.
                return;
            }
            fail();
        } catch (HttpInternalErrorException e) {
            //noop
        }
    }
}
