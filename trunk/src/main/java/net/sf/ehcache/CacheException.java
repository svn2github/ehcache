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


package net.sf.ehcache;

/**
 * A runtime Cache Exception
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheException extends RuntimeException {


    /**
     * Constructor for the CacheException object.
     */
    public CacheException() {
        super();
    }

    /**
     * Constructor for the CacheException object.
     * @param message the exception detail message
     */
    public CacheException(String message) {
        super(message);
    }



    /**
     * Constructs a new CacheException with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.2.4
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Constructs a new CacheException with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt>
     * (which typically contains the class and detail message of
     * <tt>cause</tt>).  This constructor is useful for runtime exceptions
     * that are little more than wrappers for other throwables.
     *
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.2.4
     */
    public CacheException(Throwable cause) {
        super(cause);
    }


    /**
     * The initial cause of this Exception.
     * This method is kept for backward compatibility with earlier versions of ehcache which
     * had its own chained exceptions mechanism, in case any clients out there are using it.
     * @return the cause or null if this exception has no deeper cause.
     * @deprecated use getCause instead
     */
    public final Throwable getInitialCause() {
        return super.getCause();
    }




}
