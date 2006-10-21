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

package net.sf.ehcache.distribution;

import net.sf.ehcache.CacheException;

/**
 * A Cache Exception in the distribution mechanism.
 * <p/>
 * A RemoteCacheException may not always matter to an application, which may want to ignore it.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class RemoteCacheException extends CacheException {

    /**
     * Constructor for the RemoteCacheException object.
     */
    public RemoteCacheException() {
        super();
    }

    /**
     * Constructor for the RemoteCacheException object.
     *
     * @param message the reason the exception was thrown
     */
    public RemoteCacheException(String message) {
        super(message);
    }

    /**
     * Constructor for the RemoteCacheException object.
     *
     * @param message      the exception detail message
     * @param cause the cause of the exception
     */
    public RemoteCacheException(String message, Throwable cause) {
        super(message, cause);
    }

}
