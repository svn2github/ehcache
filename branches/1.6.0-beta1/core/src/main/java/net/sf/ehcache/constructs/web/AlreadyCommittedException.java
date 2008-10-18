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

package net.sf.ehcache.constructs.web;



/**
 * <p/>
 * This exception is thrown if, in particular, the {@link javax.servlet.ServletResponse#isCommitted()}
 * method shows the response has been  committed. A commited response has already had its status code and headers written.
 * <p/>
 * One situation that can cause this problem is using jsp:include to include a full, cached page in another
 * page. When the JSP page is entered the response gets committed.
 * @see ResponseHeadersNotModifiableException
 * @author Greg Luck
 * @version $Id$
 */
public class AlreadyCommittedException extends ResponseHeadersNotModifiableException {

    /**
     * Constructor for the exception
     */
    public AlreadyCommittedException() {
        super();
    }

    /**
     * Constructs an exception with the message given
     * @param message the message
     */
    public AlreadyCommittedException(String message) {
        super(message);
    }
}
