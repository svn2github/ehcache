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

package net.sf.ehcache.server.soap;

import net.sf.ehcache.CacheException;

/**
 * Thrown to indicate an operation is being attempted on a cache that does not exist.
 * @author Greg Luck
 * @version $Id$
 */
public class NoSuchCacheException extends CacheException {

    /**
     * Constructor for the CacheException object.
     */
    public NoSuchCacheException() {
        super();
    }

    /**
     * Constructor for the CacheException object.
     *
     * @param message the exception detail message
     */
    public NoSuchCacheException(String message) {
        super(message);
    }

    /**
     * Constructs a new CacheException with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.2.4
     */
    public NoSuchCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new CacheException with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt>
     * (which typically contains the class and detail message of
     * <tt>cause</tt>).  This constructor is useful for runtime exceptions
     * that are little more than wrappers for other throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method).  (A <tt>null</tt> value is
     *              permitted, and indicates that the cause is nonexistent or
     *              unknown.)
     * @since 1.2.4
     */
    public NoSuchCacheException(Throwable cause) {
        super(cause);
    }
}
