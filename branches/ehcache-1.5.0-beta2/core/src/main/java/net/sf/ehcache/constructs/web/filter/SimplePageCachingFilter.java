/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

import net.sf.ehcache.CacheManager;

import javax.servlet.http.HttpServletRequest;

/**
 * A simple page {@link CachingFilter} suitable for most uses.
 * <p/>
 * It uses a Singleton CacheManager created with the default factory method. Override to use a different CacheManager
 * <p/>
 * The meaning of <i>page</i> is:
 * <ul>
 * <li>A complete response i.e. not a fragment.
 * <li>A content type suitable for gzipping. e.g. text or text/html
 * </ul>
 * For jsp:included page fragments see {@link SimplePageFragmentCachingFilter}.
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
 * Significant network efficiencies can be gained by gzipping responses.
 * <p/>
 * Whether a response can be gzipped depends on:
 * <ul>
 * <li>Whether the user agent can accept GZIP encoding. This feature is part of HTTP1.1.
 * If a browser accepts GZIP encoding it will advertise this by including in its HTTP header:
 * All common browsers except IE 5.2 on Macintosh are capable of accepting gzip encoding. Most search engine
 * robots do not accept gzip encoding.
 * <li>Whether the user agent has advertised its acceptance of gzip encoding. This is on a per request basis. If they
 * will accept a gzip response to their request they must include the following in the HTTP request header:
 * <code>
 * Accept-Encoding: gzip
 * </code>
 * </ul>
 * Responses are automatically gzipped and stored that way in the cache. For requests which do not accept gzip
 * encoding the page is retrieved from the cache, ungzipped and returned to the user agent. The ungzipping is
 * high performance.
 * @author Greg Luck
 * @version $Id$
 */
public class SimplePageCachingFilter extends CachingFilter {

    /**
     * The name of the filter. This should match a cache name in ehcache.xml
     */
    public static final String NAME = "SimplePageCachingFilter";

    /**
     * A meaningful name representative of the JSP page being cached.
     * <p/>
     * The name must match the name of a configured cache in ehcache.xml
     *
     * @return the name of the cache to use for this filter.
     */
    protected String getCacheName() {
        return NAME;
    }

    /**
     * Gets the CacheManager for this CachingFilter. It is therefore up to subclasses what CacheManager to use.
     * <p/>
     * This method was introduced in ehcache 1.2.1. Older versions used a singleton CacheManager instance created with
     * the default factory method.
     *
     * @return the CacheManager to be used
     * @since 1.2.1
     */
    protected CacheManager getCacheManager() {
        return CacheManager.getInstance();
    }


    /**
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
     * <p/>
     * The key should be unique.
     *
     * Implementers should differentiate between GET and HEAD requests otherwise blank pages
     * can result.
     *
     * @param httpRequest
     * @return the key, generally the URI plus request parameters
     */
    protected String calculateKey(HttpServletRequest httpRequest) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(httpRequest.getMethod()).append(httpRequest.getRequestURI()).append(httpRequest.getQueryString());
        String key = stringBuffer.toString();
        return key;
    }

}
