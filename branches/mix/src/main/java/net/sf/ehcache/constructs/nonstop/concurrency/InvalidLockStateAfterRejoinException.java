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

package net.sf.ehcache.constructs.nonstop.concurrency;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

/**
 * Exception class signifying invalid lock state. This normally happens when locks are acquired using explicit lock api's in Ehcache and a
 * rejoin happens. All locks are flushed/invalidated when a Terracotta client rejoins the cluster. This exception will be thrown, for
 * example, when trying to unlock a lock that was acquired prior to a rejoin.
 *
 * @author Abhishek Sanoujam
 *
 */
public class InvalidLockStateAfterRejoinException extends NonStopCacheException {

    /**
     * Default Constructor
     */
    public InvalidLockStateAfterRejoinException() {
        this(null);
    }

    /**
     * Constructor accepting a root cause
     */
    public InvalidLockStateAfterRejoinException(Throwable cause) {
        this("Invalid lock state as locks are flushed after rejoin. The cluster rejoined and locks were acquired before rejoin.", cause);
    }

    /**
     * Constructor accepting a root cause and a message
     */
    public InvalidLockStateAfterRejoinException(String message, Throwable cause) {
        super(message, cause);
    }

}
