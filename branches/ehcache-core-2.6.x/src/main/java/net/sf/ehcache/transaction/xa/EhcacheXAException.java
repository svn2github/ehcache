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

package net.sf.ehcache.transaction.xa;

import javax.transaction.xa.XAException;

/**
 * Small extension to the XAException defined in the JTA specification, to that the errorCode is provided when
 * instantiating the Exception thrown
 *
 * @author Alex Snaps
 */
public class EhcacheXAException extends XAException {

    /**
     * Constructor
     * @param message The message
     * @param errorCode the XA error code
     */
    public EhcacheXAException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode; 
    }

    /**
     * Constructor
     * @param message The message
     * @param errorCode the XA error code
     * @param cause the Exception causing the XAException
     */
    public EhcacheXAException(String message, int errorCode, Throwable cause) {
        super(message);
        this.errorCode = errorCode;
        initCause(cause);
    }
}
