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
 */

package net.sf.ehcache.constructs.web.filter;

import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.PageInfo;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;


/**
 * A Template for a page caching filter that is designed for "included" pages, eg: jsp:includes.  This filter
 * differs from the {@link CachingFilter} in that it is not writing an entire response to the output stream.
 * <p/>
 * This class should be sub-classed for each included page to be cached.
 * <p/>
 * Filter Mappings need to be set up for a cache to have effect.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: PageFragmentCachingFilter.java 744 2008-08-16 20:10:49Z gregluck $
 */
public abstract class PageFragmentCachingFilter extends CachingFilter {

    /**
     * Performs the filtering for a request.
     */
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response,
                            final FilterChain chain) throws Exception {

        PageInfo pageInfo = buildPageInfo(request, response, chain);

        // Send the page to the client
        writeResponse(response, pageInfo);
    }

    /**
     * {@inheritDoc}
     *
     * @param request  {@inheritDoc}
     * @param response {@inheritDoc}
     * @param chain    {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AlreadyGzippedException {@inheritDoc}
     * @throws Exception               {@inheritDoc}
     */
    protected PageInfo buildPage(final HttpServletRequest request, final HttpServletResponse response,
                                 final FilterChain chain) throws AlreadyGzippedException, Exception {

        // Invoke the next entity in the chain
        final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, outstr);
        chain.doFilter(request, wrapper);
        wrapper.flush();

        long timeToLiveSeconds = blockingCache.getCacheConfiguration().getTimeToLiveSeconds();

        // Return the page info
        return new PageInfo(wrapper.getStatus(), wrapper.getContentType(), wrapper.getHeaders(), wrapper.getCookies(),
                outstr.toByteArray(), false, timeToLiveSeconds);
    }


    /**
     * Assembles a response from a cached page include.
     * These responses are never gzipped
     * The content length should not be set in the response, because it is a fragment of a page.
     * Don't write any headers at all.
     */
    protected void writeResponse(final HttpServletResponse response, final PageInfo pageInfo) throws IOException {
        // Write the page
        final byte[] cachedPage = pageInfo.getUngzippedBody();
        //needed to support multilingual
        final String page = new String(cachedPage, response.getCharacterEncoding());


        String implementationVendor = response.getClass().getPackage().getImplementationVendor();
        if (implementationVendor != null && implementationVendor.equals("\"Evermind\"")) {
            response.getOutputStream().print(page);
        } else {
            response.getWriter().write(page);
        }
    }
}
