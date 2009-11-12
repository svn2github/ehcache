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

package net.sf.ehcache.constructs.web.filter;

import net.sf.ehcache.constructs.web.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;


/**
 * This implementation only writes the response when it is not committed. This is half
 * right. In the wild it can cause the dreaded blank page problem.
 * <p/>
 * Another half right solution is to write the response. The problem then is,
 * if the response is gzipped, the body might be gzipped but the headers won't show it.
 * The result will be yet another blank page in Internet Explorer.
 * <p/>
 * The correct thing to do is to throw an exception.
 *
 * @author Greg Luck
 * @version $Id: SimplePageCachingFilterWithBlankPageProblem.java 796 2008-10-09 02:39:03Z gregluck $
 * @see SimplePageCachingFilter
 */
public class SimplePageCachingFilterWithBlankPageProblem extends SimplePageCachingFilter {
    /**
     * The name of the filter. This should match a cache name in ehcache.xml
     */
    public static final String NAME = "SimplePageCachingFilterWithBlankPageProblem";

    private static final Logger LOG = LoggerFactory.getLogger(SimplePageCachingFilterWithBlankPageProblem.class);

    /**
     * {@inheritDoc}
     */
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response,
                            final FilterChain chain) throws Exception {
        PageInfo pageInfo = buildPageInfo(request, response, chain);
        if (response.isCommitted()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Response cannot be written as it was already committed for " + request.getRequestURL());
            }
        } else {
            writeResponse(request, response, pageInfo);
        }
    }
}

