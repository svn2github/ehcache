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

package net.sf.ehcache.search.attribute;

import net.sf.ehcache.search.SearchException;

/**
 * Thrown at query execution time if query referenced an unknown search attribute
 * @author vfunshte
 */
public class UnknownAttributeException extends SearchException {

    /**
     * @param message
     */
    public UnknownAttributeException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public UnknownAttributeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public UnknownAttributeException(Throwable cause) {
        super(cause);
    }

}
