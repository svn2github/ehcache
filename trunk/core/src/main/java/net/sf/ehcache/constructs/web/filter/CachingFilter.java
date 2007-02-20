/**
 *  Copyright 2003-2007 Greg Luck
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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.web.AlreadyCommittedException;
import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.ResponseHeadersNotModifiableException;
import net.sf.ehcache.constructs.web.ResponseUtil;
import net.sf.ehcache.constructs.web.SerializableCookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.DataFormatException;

/**
 * An abstract CachingFilter.
 * <p/>
 * This class should be sub-classed for each page to be cached.
 * <p/>
 * The filters must be declared in the web.xml deployment descriptor. Then a mapping from a web resource,
 * such as a JSP, Servlet or static resouce needs to be defined. Finally, a succession of mappings can be used
 * to create a filter chain. See SRV.6 of the Servlet 2.3 specification for more details.
 * <p/>
 * Care should be taken not to define a filter chain such that the same {@link CachingFilter} class is reentered.
 * The {@link CachingFilter} uses the {@link net.sf.ehcache.constructs.blocking.BlockingCache}. It blocks until the thread which
 * did a get which results in a null does a put. If reentry happens a second get happens before the first put. The second
 * get could wait indefinitely. This situation is monitored and if it happens, an IllegalStateException will be thrown.
 * @author @author Greg Luck
 * @version $Id$
 */
public abstract class CachingFilter extends Filter {
    private static final Log LOG = LogFactory.getLog(CachingFilter.class.getName());


    /**
     * The cache holding the web pages. Ensure that all threads for a given cache name are using the same instance of this.
     */
    protected BlockingCache blockingCache;

    /**
     * Initialises blockingCache to use. The BlockingCache created by this method does not have a lock timeout set.
     * <p/>
     * A timeout can be appled using <code>blockingCache.setTimeoutMillis(int timeout)</code> and takes effect immediately
     * for all new requests
     *
     * @throws CacheException The most likely cause is that a cache has not been
     *                        configured in ehcache's configuration file ehcache.xml for the filter name
     */
    public void doInit() throws CacheException {
        synchronized (this.getClass()) {
            if (blockingCache == null) {
                final String cacheName = getCacheName();
                Ehcache cache = getCacheManager().getEhcache(cacheName);
                if (!(cache instanceof BlockingCache)) {
                    //decorate and substitute
                    BlockingCache newBlockingCache = new BlockingCache(cache);
                    getCacheManager().replaceCacheWithDecoratedCache(cache, newBlockingCache);
                }
                blockingCache = (BlockingCache) getCacheManager().getEhcache(getCacheName());
            }
        }
    }


    /**
     * Destroys the filter.
     */
    protected void doDestroy() {
        //noop
    }

    /**
     * Performs the filtering for a request.
     *
     * @param request
     * @param response
     * @param chain
     * @throws AlreadyGzippedException     if a double gzip is attempted
     * @throws AlreadyCommittedException   if the response was committed on the way in or the on the way back
     * @throws FilterNonReentrantException if an attempt is made to reenter this filter in the same request.
     * @throws LockTimeoutException        if this request is waiting on another that is populating the cache entry
     *                                     and timeouts while waiting. Only occurs if the BlockingCache has a timeout set.
     * @throws Exception                   for all other exceptions. They will be caught and logged in
     *                                     {@link Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
     */
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response,
                            final FilterChain chain)
            throws AlreadyGzippedException,
            AlreadyCommittedException,
            FilterNonReentrantException,
            LockTimeoutException,
            Exception {
        if (response.isCommitted()) {
            throw new AlreadyCommittedException("Response already committed before doing buildPage.");
        }
        logRequestHeaders(request);
        PageInfo pageInfo = buildPageInfo(request, response, chain);

        //return on error or redirect code
        int statusCode = pageInfo.getStatusCode();
        if (statusCode != HttpServletResponse.SC_OK) {
            return;
        }

        if (response.isCommitted()) {
            throw new AlreadyCommittedException("Response already committed after doing buildPage"
                    + "but before writing response from PageInfo.");
        }
        writeResponse(request, response, pageInfo);
    }


    /**
     * Build page info either using the cache or building the page directly.
     * <p/>
     * Some requests are for page fragments which should never be gzipped, or for
     * other pages which are not gzipped.
     */
    protected PageInfo buildPageInfo(final HttpServletRequest request, final HttpServletResponse response,
                                     final FilterChain chain) throws Exception {
        // Look up the cached page
        final String key = calculateKey(request);
        PageInfo pageInfo = null;
        String originalThreadName = Thread.currentThread().getName();
        try {
            checkNoReentry(request);
            Element element = blockingCache.get(key);
            if (element == null || element.getObjectValue() == null) {
                try {
                    // Page is not cached - build the response, cache it, and send to client
                    pageInfo = buildPage(request, response, chain);
                    if (pageInfo.isOk()) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("PageInfo ok. Adding to cache " + blockingCache.getName() + " with key " + key);
                        }
                        blockingCache.put(new Element(key, pageInfo));
                    } else {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("PageInfo was not ok(200). Putting null into cache " + blockingCache.getName()
                                    + " with key " + key);
                        }
                        blockingCache.put(new Element(key, null));
                    }
                } catch (final Throwable throwable) {
                    // Must unlock the cache if the above fails. Will be logged at Filter
                    blockingCache.put(new Element(key, null));
                    throw new Exception(throwable);
                }
            } else {
                pageInfo = (PageInfo) element.getObjectValue();
            }
        } catch (LockTimeoutException e) {
            //do not release the lock, because you never acquired it
            throw e;
        } finally {
            Thread.currentThread().setName(originalThreadName);
        }
        return pageInfo;
    }


    /**
     * Builds the PageInfo object by passing the request along the filter chain
     *
     * @param request
     * @param response
     * @param chain
     * @return a Serializable value object for the page or page fragment
     * @throws AlreadyGzippedException if an attempt is made to double gzip the body
     * @throws Exception
     */
    protected PageInfo buildPage(final HttpServletRequest request, final HttpServletResponse response,
                                 final FilterChain chain) throws AlreadyGzippedException, Exception {

        // Invoke the next entity in the chain
        final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, outstr);
        chain.doFilter(request, wrapper);
        wrapper.flush();


        // Return the page info
        return new PageInfo(wrapper.getStatus(), wrapper.getContentType(), wrapper.getHeaders(), wrapper.getCookies(),
                outstr.toByteArray(), true);
    }

    /**
     * Writes the response from a PageInfo object.
     *
     * @param request
     * @param response
     * @param pageInfo
     * @throws IOException
     * @throws DataFormatException
     * @throws ResponseHeadersNotModifiableException
     *
     */
    protected void writeResponse(final HttpServletRequest request, final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, DataFormatException, ResponseHeadersNotModifiableException {
        boolean requestAcceptsGzipEncoding = acceptsGzipEncoding(request);

        setStatus(response, pageInfo);
        setHeaders(pageInfo, requestAcceptsGzipEncoding, response);
        setCookies(pageInfo, response);
        setContentType(response, pageInfo);
        writeContent(request, response, pageInfo);
    }

    /**
     * Set the content type
     *
     * @param response
     * @param pageInfo
     */
    protected void setContentType(final HttpServletResponse response, final PageInfo pageInfo) {
        response.setContentType(pageInfo.getContentType());
    }

    /**
     * Set the serializableCookies
     *
     * @param pageInfo
     * @param response
     */
    protected void setCookies(final PageInfo pageInfo, final HttpServletResponse response) {

        final Collection cookies = pageInfo.getSerializableCookies();
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            final Cookie cookie = ((SerializableCookie) iterator.next()).toCookie();
            response.addCookie(cookie);
        }
    }

    /**
     * Status code
     *
     * @param response
     * @param pageInfo
     */
    protected void setStatus(final HttpServletResponse response, final PageInfo pageInfo) {
        response.setStatus(pageInfo.getStatusCode());
    }


    /**
     * Set the headers in the response object, excluding the Gzip header
     *
     * @param pageInfo
     * @param requestAcceptsGzipEncoding
     * @param response
     */
    protected void setHeaders(final PageInfo pageInfo,
                              boolean requestAcceptsGzipEncoding,
                              final HttpServletResponse response) {

        final Collection headers = pageInfo.getHeaders();
        final int header = 0;
        final int value = 1;

        for (Iterator iterator = headers.iterator(); iterator.hasNext();) {
            final String[] headerPair = (String[]) iterator.next();
            response.addHeader(headerPair[header], headerPair[value]);
        }
    }


    /**
     * A meaningful name representative of the JSP page being cached.
     *
     * @return the name of the cache to use for this filter.
     */
    protected abstract String getCacheName();


    /**
     * Gets the CacheManager for this CachingFilter. It is therefore up to subclasses what CacheManager to use.
     * <p/>
     * This method was introduced in ehcache 1.2.1. Older versions used a singleton CacheManager instance created with
     * the default factory method.
     * @return the CacheManager to be used
     * @since 1.2.1
     */
    protected abstract CacheManager getCacheManager();


    /**
     * CachingFilter works off a key.
     * <p/>
     * The key should be unique. Factors to consider in generating a key are:
     * <ul>
     * <li>The various hostnames that a request could come through
     * <li>Whether additional parameters used for referral tracking e.g. google should be excluded
     * to maximise cache hits
     * <li>Additional parameters can be added to any page. The page will still work but will miss the
     * cache. Consider coding defensively around this issue.
     * </ul>
     *
     * @param httpRequest
     * @return the key, generally the URL plus request parameters
     */
    protected abstract String calculateKey(final HttpServletRequest httpRequest);


    /**
     * Writes the response content.
     * This will be gzipped or non gzipped depending on whether the User Agent accepts
     * GZIP encoding.
     * <p/>
     * If the body is written gzipped a gzip header is added.
     *
     * @param response
     * @param pageInfo
     * @throws IOException
     */
    protected void writeContent(final HttpServletRequest request,
                                final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, ResponseHeadersNotModifiableException {
        byte[] body;
        if (acceptsGzipEncoding(request)) {
            ResponseUtil.addGzipHeader(response);
            body = pageInfo.getGzippedBody();
            if (ResponseUtil.shouldGzippedBodyBeZero(body, request)) {
                body = new byte[0];
            }
        } else {
            body = pageInfo.getUngzippedBody();
        }

        boolean shouldBodyBeZero = ResponseUtil.shouldBodyBeZero(request, pageInfo.getStatusCode());
        if (shouldBodyBeZero) {
            body = new byte[0];
        }

        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    /**
     * Check that this caching filter is not being reentered by the same recursively.
     * Recursive calls will block indefinitely because the first request has not yet
     * unblocked the cache.
     * <p/>
     * This condition usually indicates an error in filter chaining or RequestDispatcher
     * dispatching.
     *
     * @param httpRequest
     * @throws FilterNonReentrantException if reentry is detected
     */
    protected void checkNoReentry(final HttpServletRequest httpRequest) throws FilterNonReentrantException {
        Thread thread = Thread.currentThread();
        String threadName = thread.getName();
        String filterName = getClass().getName();
        if (thread.getName().indexOf(" been through " + filterName) != -1) {
            throw new FilterNonReentrantException("The request thread is attempting to reenter"
                    + " filter "
                    + filterName
                    + ". URL: "
                    + httpRequest.getRequestURL());
        }
        //Instrument thread name
        thread.setName(thread.getName() + " been through " + filterName);
        String newThreadName = thread.getName();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Thread name changed from " + threadName
                    + " to " + newThreadName);
        }
    }
}
