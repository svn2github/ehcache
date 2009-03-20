/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
 *
 * Portions released at http://candrews.integralblue.com/2009/02/http-caching-header-aware-servlet-filter/
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
import net.sf.ehcache.config.CacheConfiguration;


/**
 * This Filter extends {@link SimplePageCachingFilter}, adding support for
 * the HTTP cache headers, ETag, Last-Modified, Expires, and If-None-Match.
 * <p/>
 * Because browsers and other HTTP clients have the expiry information returned in the response headers,
 * they do not even need to request the page again. Even once the local browser copy has expired, the browser
 * will do a conditional GET. If the todo ?? the element will always be new ?? is unchanged, a 304 will be returned.
 * <p/>
 * So why would you ever want to use SimplePageCachingFilter, which does not set these headers? Because in some caching
 * scenarios you may wish to remove a page before its natural expiry. Consider a scenario where a web page shows dynamic
 * data. Under ehcache the Element can be removed at any time. However if a browser is holding expiry information, those
 * browsers will have to wait until the expiry time before getting updated. The caching in this scenario is more about
 * defraying server load rather than minimising browser calls.
 *
 * @author Craig Andrews
 * @author Greg Luck
 * @see SimplePageCachingFilter
 */
public class SimpleBrowserHeadersPageCachingFilter extends SimplePageCachingFilter {

    /**
     * The name of the filter. This should match a cache name in ehcache.xml
     */
    public static final String NAME = "SimpleBrowserHeadersPageCachingFilter";

    private static final Logger LOG = Logger.getLogger(SimpleBrowserHeadersPageCachingFilter.class.getName());


    @SuppressWarnings("unchecked")
    @Override
    protected PageInfo buildPage(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws AlreadyGzippedException, Exception {
        PageInfo pageInfo = super.buildPage(request, response, chain);
        //add expires and last-modified headers
        Date now = new Date();

        List<String[]> headers = pageInfo.getResponseHeaders();

        HttpDateFormatter httpDateFormatter = new HttpDateFormatter();
        String lastModified = httpDateFormatter.formatHttpDate(pageInfo.getCreated());
        long ttlSeconds = calculateTimeToLiveSeconds();

        headers.add(new String[]{"Last-Modified", lastModified});
        headers.add(new String[]{"Expires", httpDateFormatter.formatHttpDate(new Date(now.getTime() + ttlSeconds * 1000))});
        headers.add(new String[]{"Cache-Control", "max-age=" + ttlSeconds});
        headers.add(new String[]{"ETag", httpDateFormatter.formatHttpDate(new Date(now.getTime() + ttlSeconds * 1000))});
        return pageInfo;
    }

    @Override
    protected void writeResponse(HttpServletRequest request, HttpServletResponse response, PageInfo pageInfo) throws IOException, DataFormatException, ResponseHeadersNotModifiableException {

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
     * Get the time to live for a page, in seconds
     *
     * @return time to live in seconds
     */
    protected long calculateTimeToLiveSeconds() {
        if (blockingCache.isDisabled()) {
            return -1;
        } else {
            CacheConfiguration cacheConfiguration = blockingCache.getCacheConfiguration();
            if (cacheConfiguration.isEternal()) {
                return 60 * 60 * 24 * 365; //one year, in seconds
            } else {
                return cacheConfiguration.getTimeToLiveSeconds();
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


