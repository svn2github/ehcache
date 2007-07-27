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

package net.sf.ehcache.constructs.web;

import net.sf.ehcache.CacheException;

/**
 * The web package performs gzipping operations. One cause of problems on web browsers
 * is getting content that is double or triple gzipped. They will either get gobblydeegook
 * or a blank page. This exception is thrown when a gzip is attempted on already gzipped content.
 * <p/>
 * This exception should be logged and the causes investigated, which are likely to be going through
 * a caching filter more than once. This exception is not normally recoverable from.
 * @author Greg Luck
 * @version $Id$
 */
public class AlreadyGzippedException extends CacheException {

    /**
     * Constructor for the exception
     */
    public AlreadyGzippedException() {
        super();
    }

    /**
     * Constructs an exception with the message given
     * @param message the message
     */
    public AlreadyGzippedException(String message) {
        super(message);
    }
}
