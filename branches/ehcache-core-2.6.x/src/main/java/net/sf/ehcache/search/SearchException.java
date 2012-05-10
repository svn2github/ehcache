/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.search;

import net.sf.ehcache.CacheException;

/**
 * A generic search exception. This exception (or a more specific subclass) will be
 * thrown for a number of conditions including (but not limited to):
 * <ul>
 * <li>Type conflict for search attribute. For example a search attribute is of type "int" but the query criteria is for
 * equals("some string")
 * <li>IOException or timeout communicating with a remote server or performing disk I/O
 * <li>Attempting to read from a discard()'d {@link Results} instance
 * </ul>
 *
 * @author teck
 */
public class SearchException extends CacheException {

    private static final long serialVersionUID = 6942653724476318512L;

    /**
     * Construct a search exception
     *
     * @param message
     */
    public SearchException(String message) {
        super(message);
    }

    /**
     * Construct a search exception with an underlying cause and message
     *
     * @param message
     * @param cause
     */
    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a search exception with an underlying cause
     *
     * @param cause
     */
    public SearchException(Throwable cause) {
        super(cause);
    }

}
