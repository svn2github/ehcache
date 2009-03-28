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

/*
 * Based on a contribution from Craig Andrews which has been released also under the Apache 2 license at
 * http://candrews.integralblue.com/2009/02/http-caching-header-aware-servlet-filter/. Copyright notice follows.
 *
 * Copyright 2009 Craig Andrews
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

import java.util.Date;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.ResponseHeadersNotModifiableException;
import net.sf.ehcache.constructs.web.HttpDateFormatter;
import net.sf.ehcache.config.CacheConfiguration;


/**
 * This Filter extends {@link SimplePageCachingFilter}, adding support for
 * the HTTP cache headers: ETag, Last-Modified and Expires.
 * <p/>
 * Because browsers and other HTTP clients have the expiry information returned in the response headers,
 * they do not even need to request the page again. Even once the local browser copy has expired, the browser
 * will do a conditional GET.
 * <p/>
 * So why would you ever want to use SimplePageCachingFilter, which does not set these headers? Because in some caching
 * scenarios you may wish to remove a page before its natural expiry. Consider a scenario where a web page shows dynamic
 * data. Under ehcache the Element can be removed at any time. However if a browser is holding expiry information, those
 * browsers will have to wait until the expiry time before getting updated. The caching in this scenario is more about
 * defraying server load rather than minimising browser calls.
 * <p/>
 * It uses a Singleton CacheManager created with the default factory method. Override to use a different CacheManager
 * <p/>
 * The meaning of <i>page</i> is:
 * <ul>
 * <li>A complete response i.e. not a fragment.
 * <li>A content type suitable for gzipping. e.g. text or text/html
 * </ul>
 * For jsp:included page fragments see {@link SimplePageFragmentCachingFilter}.
 * <h3>calculateKey</h3>
 * Pages are cached based on their key. The key for this cache is the URI followed by the query string. An example
 * is <code>/admin/SomePage.jsp?id=1234&name=Beagle</code>.
 * <p/>
 * This key technique is suitable for a wide range of uses. It is independent of hostname and port number, so will
 * work well in situations where there are multiple domains which get the same content, or where users access
 * based on different port numbers.
 * <p/>
 * A problem can occur with tracking software such as Google AdWords, where unique ids are inserted into request query strings. Because
 * each request generates a unique key, there will never be a cache hit. For these situations it is better to
 *  override {@link #calculateKey(javax.servlet.http.HttpServletRequest)} with
 * an implementation that takes account of only the significant parameters.
 *
 * <h3>Configuring the cacheName</h3>
 * A cache entry in ehcache.xml should be configured with the name of the filter.
 * <p/>
 * Names can be set using the init-param <code>cacheName</code>, or by sub-classing this class and overriding the name.
 * <h3>Concurent Cache Misses</h3>
 * A cache miss will cause the filter chain, upstream of the caching filter to be processed. To avoid threads requesting
 * the same key to do useless duplicate work, these threads block behind the first thread.
 * <p/>
 * The thead timeout can be set to fail after a certain wait by setting the init-param <code>blockingTimeoutMillis</code>.
 * By default threads wait indefinitely. In the event upstream processing never returns, eventually the web server
 * may get overwhelmed with connections it has not responded to. By setting a timeout, the waiting threads will only block
 * for the set time, and then throw a {@link net.sf.ehcache.constructs.blocking.LockTimeoutException}. Under either
 * scenario an upstream failure will still cause a failure.
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
 * <h3>Caching Headers</h3>
 * This filter sets the HTTP cache headers: ETag, Last-Modified and Expires. It supports conditional GET.
 * <p/>
 * Because browsers and other HTTP clients have the expiry information returned in the response headers,
 * they do not even need to request the page again. Even once the local browser copy has expired, the browser
 * will do a conditional GET.
 * <p/>
 * So why would you ever want to use SimplePageCachingFilter, which does not set these headers?
 * The answer is that in some caching
 * scenarios you may wish to remove a page before its natural expiry. Consider a scenario where a web page shows dynamic
 * data. Under ehcache the Element can be removed at any time. However if a browser is holding expiry information, those
 * browsers will have to wait until the expiry time before getting updated. The caching in this scenario is more about
 * defraying server load rather than minimising browser calls.
 * <p/>
 * <p/>
 * <h3>Init-Params</h3>
 * The following init-params are supported:
 * <ol>
 * <li>cacheName - the name in ehcache.xml used by the filter.
 * <li>blockingTimeoutMillis - the time, in milliseconds, to wait for the filter chain to return with a response on a cache
 * miss. This is useful to fail fast in the event of an infrastructure failure.
 * </ol>
 * <h3>Reentrance</h3>
 * Care should be taken not to define a filter chain such that the same {@link CachingFilter} class is reentered.
 * The {@link CachingFilter} uses the {@link net.sf.ehcache.constructs.blocking.BlockingCache}. It blocks until the thread which
 * did a get which results in a null does a put. If reentry happens a second get happens before the first put. The second
 * get could wait indefinitely. This situation is monitored and if it happens, an IllegalStateException will be thrown.
 * @author Craig Andrews
 * @author Greg Luck
 * @see SimplePageCachingFilter
 */
public class SimpleCachingHeadersPageCachingFilter extends SimplePageCachingFilter {

    /**
     * The name of the filter. This should match a cache name in ehcache.xml
     */
    public static final String NAME = "SimpleCachingHeadersPageCachingFilter";

    private static final Logger LOG = Logger.getLogger(SimpleCachingHeadersPageCachingFilter.class.getName());
    private static final long ONE_YEAR_IN_MILLISECONDS = 60 * 60 * 24 * 365 * 1000L;
    private static final int MILLISECONDS_PER_SECOND = 1000;


    /**
     * Builds the PageInfo object by passing the request along the filter chain
     * <p>
     * The following headers are set:
     * <ul>
     * <li>Last-Modified
     * <li>Expires
     * <li>Cache-Control
     * <li>ETag
     * </ul>
     * Any of these headers aleady set in the response are ignored, and new ones generated. To control
     * your own caching headers, use {@link SimplePageCachingFilter}.
     *
     *
     * @param request
     * @param response
     * @param chain
     * @return a Serializable value object for the page or page fragment
     * @throws AlreadyGzippedException if an attempt is made to double gzip the body
     * @throws Exception
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    protected PageInfo buildPage(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws AlreadyGzippedException, Exception {
        PageInfo pageInfo = super.buildPage(request, response, chain);
        //add expires and last-modified headers
        Date now = new Date();

        List<String[]> headers = pageInfo.getResponseHeaders();

        HttpDateFormatter httpDateFormatter = new HttpDateFormatter();
        String lastModified = httpDateFormatter.formatHttpDate(pageInfo.getCreated());
        long ttlMilliseconds = calculateTimeToLiveMilliseconds();
        headers.add(new String[]{"Last-Modified", lastModified});
        headers.add(new String[]{"Expires", httpDateFormatter.formatHttpDate(new Date(now.getTime() + ttlMilliseconds))});
        headers.add(new String[]{"Cache-Control", "max-age=" + ttlMilliseconds / MILLISECONDS_PER_SECOND});
        headers.add(new String[]{"ETag", generateEtag(ttlMilliseconds)});
        return pageInfo;
    }


    /**
     * ETags are required to have double quotes around the value, unlike any other header.
     * <p/>
     * The ehcache eTag is effectively the Expires time, but accurate to milliseconds, i.e.
     * no conversion to the nearest second is done as is done for the Expires tag. It therefore
     * is the most precise indicator of whether the client cached version is the same as the server
     * version.
     * <p/>
     * MD5 is not used to calculate ETag, as it is in some implementations, because it does not
     * add any extra value in this situation, and it has a higher cost.
     *
     * @see "http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.3.3"
     */
    private String generateEtag(long ttlMilliseconds) {
        StringBuffer stringBuffer = new StringBuffer();
        Long eTagRaw = System.currentTimeMillis() + ttlMilliseconds;
        String eTag = stringBuffer.append("\"").append(eTagRaw).append("\"").toString();
        return eTag;
    }



    /**
     * Writes the response from a PageInfo object.
     *
     * This method actually performs the conditional GET and returns 304
     * if not modified, short-circuiting the normal writeResponse.
     * <p/>
     * Indeed, if the short cicruit does not occur it calls the super method.
     */
    @Override
    protected void writeResponse(HttpServletRequest request, HttpServletResponse response, PageInfo pageInfo)
            throws IOException, DataFormatException, ResponseHeadersNotModifiableException {

        HttpDateFormatter httpDateFormatter = new HttpDateFormatter();


        final Collection responseHeaders = pageInfo.getResponseHeaders();
        final int header = 0;
        final int value = 1;
        for (Object header1 : responseHeaders) {
            final String[] headerPair = (String[]) header1;


            if (headerPair[header].equals("ETag")) {
                String requestIfNoneMatch = request.getHeader("If-None-Match");
                if (headerPair[value].equals(requestIfNoneMatch)) {
                    response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    // use the same date we sent when we created the ETag the first time through
                    //response.setHeader("Last-Modified", request.getHeader("If-Modified-Since"));
                    return;
                }
                break;
            }
            if (headerPair[header].equals("Last-Modified")) {
                String requestIfModifiedSince = request.getHeader("If-Modified-Since");
                if (requestIfModifiedSince != null) {
                    Date requestDate = httpDateFormatter.parseDateFromHttpDate(requestIfModifiedSince);
                    Date pageInfoDate = httpDateFormatter.parseDateFromHttpDate(headerPair[value]);
                    if (requestDate.getTime() >= pageInfoDate.getTime()) {
                        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                        response.setHeader("Last-Modified", request.getHeader("If-Modified-Since"));
                        return;
                    }
                }
            }
        }

        super.writeResponse(request, response, pageInfo);
    }

    /**
     * Get the time to live for a page, in milliseconds
     *
     * @return time to live in milliseconds
     */
    protected long calculateTimeToLiveMilliseconds() {
        if (blockingCache.isDisabled()) {
            return -1;
        } else {
            CacheConfiguration cacheConfiguration = blockingCache.getCacheConfiguration();
            if (cacheConfiguration.isEternal()) {
                return ONE_YEAR_IN_MILLISECONDS;
            } else {
                return cacheConfiguration.getTimeToLiveSeconds() * MILLISECONDS_PER_SECOND;
            }
        }
    }

    /**
     * Each time an element is put into ehcache the creation time is set even if it is an update.
     * So, "creation time" means Last-Modified.
     *
     * @param ehcacheElement the underlying Ehcache element
     * @return the last modified date. If this is the first version of the element, the last-modified means the name things as created.
     *         This date is accurate to ms, however the HTTP protocol is not - it only goes down to seconds. Jersey removes the ms.
     */
    private Date createLastModified(net.sf.ehcache.Element ehcacheElement) {
        long lastModified = ehcacheElement.getCreationTime();
        Date lastModifiedDate = new Date(lastModified);
        LOG.log(Level.FINE, "lastModified as long: {}", lastModified);
        LOG.log(Level.FINE, "lastModified as Date without ms: {}", lastModifiedDate);
        return lastModifiedDate;
    }

    /**
     * A very performant ETag implementation.
     * This will be unique across JVM restarts, or deleting an element and putting one back in.
     *
     * @param ehcacheElement A backing ehcache element
     * @return the ETag for this entry
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.11">HTTP/1.1 section 3.11</a>
     */
    private String createETag(net.sf.ehcache.Element ehcacheElement) {

        //For a given key and server this is unique, unless two updates for that key happened in the same millisecond.
        long eTagNumber = ehcacheElement.getCreationTime();

        return new StringBuffer().append(eTagNumber).toString();
    }

}


