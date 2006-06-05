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

import javax.servlet.http.HttpServletRequest;

/**
 * A simple page fragment {@link CachingFilter} suitable for most uses.
 * <p/>
 * The meaning of <i>page fragment</i> is:
 * <ul>
 * <li>An include into an outer page.
 * <li>A content type suitable for suitable for inclusion into the outer page. e.g. text or text/html
 * </ul>
 * For full page see {@link SimplePageFragmentCachingFilter}.
 * <h3>Keys</h3>
 * Pages are cached based on their key. The key for this cache is the URI followed by the query string. An example
 * is <code>/admin/SomePage.jsp?id=1234&name=Beagle</code>.
 * <p/>
 * This key technique is suitable for a wide range of uses. It is independent of hostname and port number, so will
 * work well in situations where there are multiple domains which get the same content, or where users access
 * based on different port numbers.
 * <p/>
 * A problem can occur with tracking software, where unique ids are inserted into request query strings. Because
 * each request generates a unique key, there will never be a cache hit. For these situations it is better to
 * parse the request parameters and override {@link #calculateKey(javax.servlet.http.HttpServletRequest)} with
 * an implementation that takes account of only the significant ones.
 * <h3>Configuring Caching with ehcache</h3>
 * A cache entry in ehcache.xml should be configured with the name {@link #NAME}.
 * <p/>
 * Cache attributes including expiry are configured per cache name. To specify a different behaviour simply
 * subclass, specify a new name and create a separate cache entry for it.
 * <h3>Gzipping</h3>
 * Page fragments should never be gzipped.
 * <p/>
 * Page fragments are stored in the cache ungzipped.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class SimplePageFragmentCachingFilter extends PageFragmentCachingFilter {

    /**
     * This filter's name
     */
    public static final String NAME = "SimplePageFragmentCachingFilter";


    /**
     * CachingFilter works off a key.
     * <p/>
     * This test implementation has a single key.
     *
     * @param httpRequest
     * @return the key, generally the URL plus request parameters
     */
    protected String calculateKey(HttpServletRequest httpRequest) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(httpRequest.getRequestURI()).append(httpRequest.getQueryString());
        String key = stringBuffer.toString();
        return key;
    }

    /**
     * Returns the name of the cache to use for this filter.
     */
    protected String getCacheName() {
        return NAME;
    }
}

