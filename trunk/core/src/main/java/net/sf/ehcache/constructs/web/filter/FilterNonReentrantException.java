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

import net.sf.ehcache.CacheException;

/**
 * Thrown when it is detected that a caching filter's {@link CachingFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
 * method is reentered by the same thread.
 * <p/>
 * Reentrant calls will block indefinitely because the first request has not yet
 * unblocked the cache.
 * <p/>
 * This condition usually happens declaratively when the same filter is specified twice in a filter chain
 * or programmatically when a {@link javax.servlet.RequestDispatcher} includes or forwards back to the same URL,
 * either directly or indirectly.
 * @author Greg Luck
 * @version $Id$
 */
public class FilterNonReentrantException extends CacheException {

    /**
     * Constructor for the exception
     */
    public FilterNonReentrantException() {
        super();
    }

    /**
     * Constructs an exception with the message given
     *
     * @param message the message
     */
    public FilterNonReentrantException(String message) {
        super(message);
    }
}
