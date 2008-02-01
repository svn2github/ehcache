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
    
package net.sf.ehcache.exceptionhandler;

import net.sf.ehcache.Ehcache;

/**
 * A handler which may be registered with an Ehcache, to handle exception on Cache operations.
 * <p/>
 * Handlers may be registered at configuration time in ehcache.xml, using a CacheExceptionHandlerFactory, or
 *  set at runtime (a strategy).
 * <p/>
 * If an exception handler is registered, the default behaviour of throwing the exception will not occur. The handler
 * method <code>onException</code> will be called. Of course, if the handler decides to throw the exception, it will
 * propagate up through the call stack. If the handler does not, it won't.
 * <p/>
 * Some common Exceptions thrown, and which therefore should be considered when implementing this class are listed below:
 * <ul>
 * <li>{@link IllegalStateException} if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
 * <li>{@link IllegalArgumentException} if an attempt is made to put a null element into a cache
 * <li>{@link net.sf.ehcache.distribution.RemoteCacheException} if an issue occurs in remote synchronous replication
 * <li>
 * <li>
 * </ul>
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public interface CacheExceptionHandler {

    /**
     * Called if an Exception occurs in a Cache method. This method is not called
     * if an <code>Error</code> occurs.
     *
     * @param ehcache   the cache in which the Exception occurred
     * @param key       the key used in the operation, or null if the operation does not use a key or the key was null
     * @param exception the exception caught
     */
    void onException(Ehcache ehcache, Object key, Exception exception);
}

