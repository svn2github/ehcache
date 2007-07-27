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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.CacheException;


/**
 * Indicates that a timeout has occured while attempting to obtain a lock using
 * {@link net.sf.ehcache.constructs.concurrent.Mutex#attempt(long)}
 * <p/>
 * This is a normal runtime exception which should be handled by calling code.
 * It is possible that simply reattempting to obtain the lock may succeed.
 * Timeouts are often caused by overloaded resources.
 * <p/>
 * The frequency of these Exceptions may be reduced by increasing the timeout
 * if appropriate.
 * @author Greg Luck
 * @version $Id$
 */
public class LockTimeoutException extends CacheException {

    /**
     * Constructs a new runtime exception with <code>null</code> as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public LockTimeoutException() {
        super();
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public LockTimeoutException(String message) {
        super(message);
    }


    /**
     * Constructor for the LockTimeoutException object.
     *
     * @param message      the exception detail message
     * @param cause the cause of the exception which can later be retrieved with the {@link #getInitialCause()}
     *                     method
     */
    public LockTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }


}
