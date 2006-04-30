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

import net.sf.ehcache.constructs.web.GenericResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Provides GZIP compression of responses.
 * <p/>
 * See the filter-mappings.xml entry for the gzip filter for the URL patterns
 * which will be gzipped. At present this includes .jsp, .js and .css.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @author <a href="mailto:amurdoch@thoughtworks.com">Adam Murdoch</a>
 * @version $Id$
 */
public class GzipFilter extends Filter {

    private static final Logger LOG = Logger.getLogger(GzipFilter.class.getName());

    /**
     * Performs initialisation.
     */
    protected void doInit() throws Exception {
    }

    /**
     * A template method that performs any Filter specific destruction tasks.
     * Called from {@link #destroy()}
     */
    protected void doDestroy() {
        //noop
    }

    /**
     * Performs the filtering for a request.
     */
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response,
                            final FilterChain chain) throws Exception {
        if (!isIncluded(request) && acceptsEncoding(request, "gzip")) {
            // Client accepts zipped content
            LOG.fine("Writing with gzip compression");

            // Create a gzip stream
            final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            final GZIPOutputStream gzout = new GZIPOutputStream(compressed);

            // Handle the request
            final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, gzout);
            chain.doFilter(request, wrapper);
            wrapper.flush();
            gzout.close();

            // Write the zipped body
            addGzipHeader(response);
            response.setContentLength(compressed.size());
            response.getOutputStream().write(compressed.toByteArray());
        } else {
            // Client does not accept zipped content - don't bother zipping
            LOG.fine("Writing without compression");
            chain.doFilter(request, response);
        }
    }

    /**
     * Checks if the request uri is an include.
     * These cannot be gzipped.
     * todo maybe issue a warning here or maybe log
     */
    private boolean isIncluded(final ServletRequest request) {
        final String uri = (String) request.getAttribute("javax.servlet.include.request_uri");
        return !(uri == null);
    }

    /**
     * Determine whether the user agent accepts GZIP encoding. This feature is part of HTTP1.1.
     * If a browser accepts GZIP encoding it will advertise this by including in its HTTP header:
     * <p/>
     * <code>
     * Accept-Encoding: gzip
     * </code>
     * <p/>
     * Requests which do not accept GZIP encoding fall into the following categories:
     * <ul>
     * <li>Old browsers, notably IE 5 on Macintosh.
     * <li>Internet Explorer through a proxy. By default HTTP1.1 is enabled but disabled when going
     * through a proxy. 90% of non gzip requests seen on the Internet are caused by this.
     * </ul>
     * As of September 2004, about 34% of Internet requests do not accept GZIP encoding.
     *
     * @param request
     * @return true, if the User Agent request accepts GZIP encoding
     */
    protected boolean acceptsGzipEncoding(HttpServletRequest request) {
        return acceptsEncoding(request, "gzip");
    }

    /**
     * Adds the gzip HTTP header to the response. This is need when a gzipped body
     * is returned
     *
     * @param response
     */
    protected void addGzipHeader(final HttpServletResponse response) {
        response.setHeader("Content-Encoding", "gzip");
    }
}
