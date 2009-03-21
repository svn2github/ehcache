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

package net.sf.ehcache.constructs.web;

import net.sf.ehcache.CacheException;

/**
 *
 * The {@link javax.servlet.http.HttpServletResponse#setHeader(String, String)} method
 * sets a response header with the given name and value.
 * <p/>
 * If the header had already been set, the new value overwrites the previous one.
 * The containsHeader method can be  used to test for the presence of a header before setting its  value.
 * <p/>
 * In some cases, the {@link javax.servlet.http.HttpServletResponse#setHeader(String, String)} is ignored.
 * <ol>
 * <li>The {@link javax.servlet.ServletResponse#isCommitted()}.
 * <li>The {@link javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
 * method was used to call the resource.
 * </ol>
 * Ehcache-constructs may set the "Accept-Encoding" header to "gzip". If the response is committed before
 * it has a change to do this, the client may receive gzipped content, but not the gzip header. This
 * will cause an error in Internet Explorer. Mozilla will recognise the content and ungzip it.
 * <p/>
 * If this situation occurs, rather than continue, this exception is thrown.
 * @see "SRV.8.3 in the Servlet 2.3 Specification"
 * @author Greg Luck
 * @version $Id: ResponseHeadersNotModifiableException.java 744 2008-08-16 20:10:49Z gregluck $
 */
public class ResponseHeadersNotModifiableException extends CacheException {

    /**
     * Constructor for the exception
     */
    public ResponseHeadersNotModifiableException() {
        super();
    }

    /**
     * Constructs an exception with the message given
     * @param message the message
     */
    public ResponseHeadersNotModifiableException(String message) {
        super(message);
    }
}
