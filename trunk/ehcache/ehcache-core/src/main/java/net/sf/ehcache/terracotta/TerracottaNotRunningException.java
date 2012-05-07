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

package net.sf.ehcache.terracotta;

import net.sf.ehcache.CacheException;

/**
 * Exception thrown when using Terracotta clustered operations and Terracotta is not running
 *
 * @author Abhishek Sanoujam
 *
 */
public class TerracottaNotRunningException extends CacheException {

    /**
     * Default Constructor
     */
    public TerracottaNotRunningException() {
        super();
    }

    /**
     * Constructor accepting message and {@link Throwable}
     */
    public TerracottaNotRunningException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor accepting message
     */
    public TerracottaNotRunningException(String message) {
        super(message);
    }

    /**
     * Constructor accepting {@link Throwable}
     */
    public TerracottaNotRunningException(Throwable cause) {
        super(cause);
    }

}
