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

package net.sf.ehcache.constructs.nonstop;

import net.sf.ehcache.CacheException;

/**
 * Exception type thrown for rejoins when non-stop is disabled.
 * 
 * @author cdennis
 */
public class RejoinCacheException extends CacheException {

    /**
     * Constructor accepting a String message and a Throwable cause
     * 
     * @param message
     * @param cause
     */
    public RejoinCacheException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor accepting a String message
     * 
     * @param message
     */
    public RejoinCacheException(final String message) {
        super(message);
    }
}
